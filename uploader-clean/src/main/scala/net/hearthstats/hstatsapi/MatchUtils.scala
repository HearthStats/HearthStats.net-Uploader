package net.hearthstats.hstatsapi

import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.game.MatchState
import net.hearthstats.companion.CompanionState
import net.hearthstats.core.ArenaRun
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.util.Translation
import net.hearthstats.util.Tracker
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.ui.MatchEndPopup
import net.hearthstats.ui.HearthstatsPresenter
import java.awt.Component
import net.hearthstats.ui.Button

class MatchUtils(
  matchState: MatchState,
  companionState: CompanionState,
  api: API,
  translation: Translation,
  notifier: NotificationQueue,
  matchPopup: MatchEndPopup,
  hsPresenter: HearthstatsPresenter,
  analytics: Tracker,
  uiLog: Log) {

  import translation.t

  def submitMatchResult(): Unit = {
    createArenaRun()
    val hsMatch = matchState.currentMatch.get
    if (hsMatch.isDataComplete) {
      submitMatchImpl(hsMatch)
    } else {
      matchPopup.showPopup(hsPresenter.asInstanceOf[Component], hsMatch) match {
        case Button.SUBMIT => submitMatchImpl(hsMatch)
        case _ =>
          uiLog.info(s"Match was not submitted")
          matchState.submitted = true
      }
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

  /**
   * Checks whether the match result is complete, showing a popup if necessary
   * to fix the match data, and then submits the match when ready.
   *
   * @param hsMatch
   *          The match to check and submit.
   */
  //  private def showEndMatchPopup(): Unit = {
  //      val matchPopup = optionMatchPopup.get
  //      val showPopup = matchPopup match {
  //        case MatchPopup.ALWAYS => true
  //        case MatchPopup.INCOMPLETE => !hsMatch.isDataComplete
  //        case MatchPopup.NEVER => false
  //        case _ => throw new UnsupportedOperationException("Unknown config option " + matchPopup)
  //      }
  //      if (showPopup) {
  //        Swing.onEDT {
  //          try {
  //            var matchHasValidationErrors = !hsMatch.isDataComplete
  //            var infoMessage: String = null
  //            do {
  //              if (infoMessage == null) {
  //                infoMessage = if (matchPopup == MatchPopup.INCOMPLETE)
  //                  t("match.popup.message.incomplete")
  //                else
  //                  t("match.popup.message.always")
  //              }
  //              mainFrame.bringWindowToFront()
  //              val buttonPressed = MatchEndPopup.showPopup(mainFrame, hsMatch, infoMessage, t("match.popup.title"))
  //              matchHasValidationErrors = !hsMatch.isDataComplete
  //              buttonPressed match {
  //                case Button.SUBMIT => if (matchHasValidationErrors) {
  //                  infoMessage = "Some match information is incomplete.<br>Please update these details then click Submit to submit the match to HearthStats:"
  //                } else {
  //                  _submitMatchResult(hsMatch)
  //                }
  //                case Button.CANCEL => return
  //              }
  //            } while (matchHasValidationErrors);
  //          } catch {
  //            case e: Exception => Main.showErrorDialog("Error submitting match result", e)
  //          }
  //        }
  //      } else
  //        try {
  //          _submitMatchResult(hsMatch)
  //        } catch {
  //          case e: Exception => Main.showErrorDialog("Error submitting match result", e)
  //        }
  //    }
  //  }

  private def submitMatchImpl(hsMatch: HearthstoneMatch): Unit = {
    val header = t("match.end.submitting")
    val message = hsMatch.toString
    notifier.add(header, message, false)
    analytics.trackEvent("app", "Submit" + hsMatch.mode + "Match")
    uiLog.matchResult(header + ": " + message)
    api.createMatch(hsMatch) match {
      case Some(id) =>
        matchState.submitted = true
        uiLog.info(s"Success. <a href='http://hearthstats.net/constructeds/$id/edit'>Edit match #$id on HearthStats.net</a>")
      case None => uiLog.warn("Could not submit the match to Hearthstats.net, API error")
    }

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