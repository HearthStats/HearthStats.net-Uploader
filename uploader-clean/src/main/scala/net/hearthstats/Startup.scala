package net.hearthstats

import grizzled.slf4j.Logging
import net.hearthstats.ui.log.Log
import net.hearthstats.config.Environment
import net.hearthstats.util.Translation
import net.hearthstats.config.Application
import net.hearthstats.config.OS
import net.hearthstats.config.UserConfig
import net.hearthstats.util.AnalyticsTracker

class Startup(
  translation: Translation,
  uiLog: Log,
  environment: Environment,
  config: UserConfig) extends Logging {

  import translation.t
  import config._

  val analytics = AnalyticsTracker.tracker

  def start(): Unit = {
    showWelcomeLog()
    if (enableAnalytics) {
      debug("Enabling analytics")
      analytics.trackEvent("app", "AppStart")
    }
  }

  private def showWelcomeLog() {
    debug("Showing welcome log messages")
    uiLog.welcome("HearthStats " + t("Companion") + " v" + Application.version)
    debug("Showing welcome log messages")
    uiLog.help(t("welcome_1_set_decks"))
    if (environment.os == OS.OSX) {
      uiLog.help(t("welcome_2_run_hearthstone"))
      uiLog.help(t("welcome_3_notifications"))
    } else {
      uiLog.help(t("welcome_2_run_hearthstone_windowed"))
      uiLog.help(t("welcome_3_notifications_windowed"))
    }
    val logFileLocation = uiLog.getLogFileLocation
    if (logFileLocation == null) {
      uiLog.help(t("welcome_4_feedback"))
    } else {
      uiLog.help(t("welcome_4_feedback_with_log", logFileLocation))
    }
  }

  def checkForUpdates() {
    if (enableUpdateCheck) {
      uiLog.info(t("checking_for_updates..."))
      //      try {
      //        var latestRelease = Updater.getLatestRelease(environment)
      //        if (latestRelease != null) {
      //          uiLog.info(t("latest_v_available") + " " + latestRelease.getVersion)
      //          if (!latestRelease.getVersion.equalsIgnoreCase("v" + Application.version)) {
      //            bringWindowToFront()
      //            val dialogButton = YES_NO_OPTION
      //            var dialogResult = showConfirmDialog(
      //              this,
      //              s"""A new version of HearthStats Companion is available: ${latestRelease.getVersion}
      //                  |${latestRelease.getBody}
      //                  |            
      //                  |
      //                  | ${t("would_u_like_to_install_update")}""".stripMargin,
      //              "HearthStats " + t("uploader_updates_avail"),
      //              dialogButton)
      //            if (dialogResult == YES_OPTION) {
      //              Updater.run(environment, latestRelease)
      //            } else {
      //              dialogResult = showConfirmDialog(
      //                null,
      //                t("would_you_like_to_disable_updates"),
      //                t("disable_update_checking"),
      //                dialogButton)
      //              if (dialogResult == YES_OPTION) {
      //                val options = Array(t("button.ok"))
      //                val panel = new JPanel()
      //                val lbl = new JLabel(t("reenable_updates_any_time"))
      //                panel.add(lbl)
      //                showOptionDialog(this, panel, t("updates_disabled_msg"), NO_OPTION,
      //                  QUESTION_MESSAGE, null, options.toArray, options(0))
      //                enableUpdateCheck.set(false)
      //              }
      //            }
      //          }
      //        } else uiLog.warn("Unable to determine latest available version")
      //      } catch {
      //        case e: Exception => {
      //          e.printStackTrace(System.err)
      //          notify("Update Checking Error", "Unable to determine the latest available version")
      //        }
      //      }
    }
  }
}