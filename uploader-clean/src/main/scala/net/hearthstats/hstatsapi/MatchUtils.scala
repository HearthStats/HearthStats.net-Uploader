package net.hearthstats.hstatsapi

import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.game.MatchState
import net.hearthstats.companion.CompanionState
import net.hearthstats.core.ArenaRun
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.util.Translation
import net.hearthstats.util.Tracker

class MatchUtils(
  matchState: MatchState,
  companionState: CompanionState,
  api: API,
  translation: Translation,
  notifier: NotificationQueue,
  analytics: Tracker,
  uiLog: Log) {

  import translation.t

  def submitMatchResult(): Unit = {
    createArenaRun()
    val hsMatch = matchState.currentMatch.get
    val header = t("match.end.submitting")
    val message = hsMatch.toString
    notifier.add(header, message, false)
    analytics.trackEvent("app", "Submit" + hsMatch.mode + "Match")
    uiLog.matchResult(header + ": " + message)
    api.createMatch(hsMatch) match {
      case Some(id) =>
        hsMatch.submitted = true
        uiLog.info(s"Success. <a href='http://hearthstats.net/constructeds/$id/edit'>Edit match #$id on HearthStats.net</a>")
      case None => uiLog.warn("Could not submit the match to Hearthstats.net, API error")
    }

    //    import scala.concurrent.ExecutionContext.Implicits.global
    //    HearthstoneAnalyser.videoEncoder.finish().onSuccess {
    //      case fileName =>
    //        ReplayHandler.handleNewReplay(fileName, hsMatch).onSuccess {
    //          case name =>
    //            val msg = s"Video replay of your match $name successfully uploaded"
    //            Log.info(msg)
    //            mainFrame.notify(msg)
    //        }
    //    }

  }

  private def createArenaRun(): Unit = {
    val hsMatch = matchState.currentMatch.get
    if (companionState.isNewArenaRun) {
      val run = new ArenaRun()
      run.setUserClass(hsMatch.userClass.toString)
      uiLog.info("Creating new " + run.getUserClass + " arena run")
      api.createArenaRun(run)
      companionState.isNewArenaRun = false
    }
  }
}