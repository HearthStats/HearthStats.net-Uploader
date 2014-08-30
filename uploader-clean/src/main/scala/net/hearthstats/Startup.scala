package net.hearthstats

import grizzled.slf4j.Logging
import net.hearthstats.ui.log.Log
import net.hearthstats.config.Environment
import net.hearthstats.util.Translation
import net.hearthstats.config.Application
import net.hearthstats.config.OS

class Startup(
  translation: Translation,
  uiLog: Log,
  environment: Environment) extends Logging {

  import translation.t

  def start(): Unit = {
    showWelcomeLog()
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
}