package net.hearthstats.hstatsapi

import java.awt.Component
import net.hearthstats.companion.CompanionState
import net.hearthstats.core.{ ArenaRun, GameMode, HearthstoneMatch }
import net.hearthstats.game.MatchState
import net.hearthstats.ui.log.Log
import net.hearthstats.config.UserConfig
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.ui.{ Button, HearthstatsPresenter, MatchEndPopup }
import net.hearthstats.util.{ Tracker, Translation }
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.modules.ReplayHandler
import scala.util.Success
import scala.util.Failure

class MatchUtils(
  matchState: MatchState,
  companionState: CompanionState,
  api: API,
  translation: Translation,
  notifier: NotificationQueue,
  matchPopup: MatchEndPopup,
  hsPresenter: HearthstatsPresenter,
  replayHandler: ReplayHandler,
  analytics: Tracker,
  uiLog: Log,
  config: UserConfig) {

  import translation.t
  import config._

  /**
   * Returns the submitted match if it was accepted by hs.net.
   */
  def submitMatchResult(): Option[HearthstoneMatch] = {
    createArenaRun()
    val hsMatch = matchState.currentMatch.get
    val d = hsMatch.duration
    val submittedMatch = if (d <= 10) {
      uiLog.warn(s"Ignoring match with duration $d s, you should close Hearthstats Companion before Hearthstone to avoid this warning.")
      None
    } else if (hsMatch.mode == GameMode.PRACTICE) {
      uiLog.info("Practice match was not submitted")
      None
    } else {
      if(config.matchPopup.get.toString() == "ALWAYS"){
        matchPopup.showPopup(hsPresenter.asInstanceOf[Component], hsMatch) match {
            case Some(m) => 
              submitMatchImpl(m)
            case _ =>
              uiLog.info(s"Match was not submitted")
              matchState.submitted = true
              None
          }
      }
      else if (config.matchPopup.get.toString() == "INCOMPLETE"){
        if (hsMatch.isDataComplete)submitMatchImpl(hsMatch) 
        else {
          matchPopup.showPopup(hsPresenter.asInstanceOf[Component], hsMatch) match {
            case Some(m) => submitMatchImpl(m)
            case _ =>
              uiLog.info(s"Match was not submitted")
              matchState.submitted = true
              None
          }
        }
      }
      else {
        if(hsMatch.isDataComplete)submitMatchImpl(hsMatch)
        else None
      }
      
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      v <- companionState.ongoingVideo
      fileName <- v.finish()
      m <- submittedMatch
    } {
      replayHandler.handleNewReplay(fileName, m)
    }
    companionState.ongoingVideo = None
    submittedMatch
  }

  def describeMatch(m: HearthstoneMatch): String = {
    import m._

    val describeMode = mode match {
      case GameMode.ARENA => t("match.end.mode.arena")
      case GameMode.CASUAL => t("match.end.mode.casual")
      case GameMode.RANKED => t("match.end.mode.ranked", rankLevel.get)
      case GameMode.PRACTICE => t("match.end.mode.practice")
      case GameMode.FRIENDLY => t("match.end.mode.friendly")
      case _ => "unknown mode"
    }

    val describeCoin = coin match {
      case Some(true) => t("match.end.coin.true")
      case _ => t("match.end.coin.false")
    }

    val describePlayers = opponentName match {
      case null => t("match.end.vs.unnamed", userClass.toString, opponentClass.toString)
      case _ => t("match.end.vs.named", userClass.toString, opponentClass.toString, opponentName)
    }

    val describeDeck = mode match {
      case GameMode.ARENA => ""
      case _ => {
        t("match.end.deck.name", deck match {
          case Some(d) => d.name
          case None => "[unknown]"
        })
      }
    }

    val describeTurns = t("match.end.turns", numTurns)

    val describeDuration = {
      val minutes = duration / 60
      val seconds = duration % 60
      t("match.end.duration", minutes, f"$seconds%02d")
    }

    s"$describeMode $describeCoin $describePlayers $describeResult $describeDeck $describeTurns $describeDuration"
  }

  private def submitMatchImpl(hsMatch: HearthstoneMatch): Option[HearthstoneMatch] = {
    val header = t("match.end.submitting")
    val message = describeMatch(hsMatch)
    notifier.add(header, message, false)
    analytics.trackEvent("app", "Submit" + hsMatch.mode + "Match")
    uiLog.matchResult(header + ": " + message)
    api.createMatch(hsMatch) match {
      case Success(id) =>
        matchState.submitted = true
        val submittedMatch = hsMatch.copy(id = id)
        matchState.currentMatch = Some(submittedMatch)

        uiLog.info(s"Success. <a href='${submittedMatch.matchUrl}'>Review match on HearthStats.net</a>")
        hsPresenter.matchSubmitted(submittedMatch, describeMatch(submittedMatch))
        Some(submittedMatch)
      case Failure(e) =>
        uiLog.warn("Could not submit the match to Hearthstats.net, API error", e)
        None
    }
  }

  private def createArenaRun(): Unit = {
    val hsMatch = matchState.currentMatch.get
    if (companionState.isNewArenaRun) {
      val run = ArenaRun(hsMatch.userClass.toString)
      uiLog.info("Creating new " + run.`class` + " arena run")
      api.createArenaRun(run)
      companionState.isNewArenaRun = false
    }
  }
}