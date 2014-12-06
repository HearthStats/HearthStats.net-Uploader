package net.hearthstats

import java.util.concurrent.{Executors, TimeUnit}
import javax.swing.JOptionPane._
import javax.swing.{JLabel, JPanel}

import grizzled.slf4j.Logging
import net.hearthstats.config.{Application, Environment, OS, UserConfig}
import net.hearthstats.game.ocr.BackgroundImageSave
import net.hearthstats.ui.CompanionFrame
import net.hearthstats.ui.log.Log
import net.hearthstats.util.{Tracker, Translation, Updater}

import scala.swing.Swing

class Startup(
  translation: Translation,
  uiLog: Log,
  analytics: Tracker,
  environment: Environment,
  updater: Updater,
  config: UserConfig,
  companionFrame: CompanionFrame) extends Logging {

  import config._
  import companionFrame._
  import translation.t

  def start(): Unit = {
    showWelcomeLog()
    analytics.trackEvent("app", "AppStart")
    Swing.onEDT(checkForUpdates())
    startAutoGc()
    BackgroundImageSave.setSaveFolder(environment.extractionFolder)
  }

  // It seems that the native libraries for screen capture have memory leak which are fixed this way
  private def startAutoGc(): Unit = {
    val GC_INTERVAL = 3000
    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
      new Runnable { def run() = System.gc() },
      GC_INTERVAL, GC_INTERVAL, TimeUnit.MILLISECONDS)
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
    uiLog.getLogFileLocation match {
      case Some(location) => uiLog.help(t("welcome_4_feedback_with_log", location))
      case None => uiLog.help(t("welcome_4_feedback"))
    }
  }

  def checkForUpdates() {
    if (enableUpdateCheck) {
      uiLog.info(t("checking_for_updates..."))
      try {
        var latestRelease = updater.getLatestRelease()
        if (latestRelease != null) {
          uiLog.info(t("latest_v_available") + " " + latestRelease.getVersion)
          if (!latestRelease.getVersion.equalsIgnoreCase("v" + Application.version)) {
            bringWindowToFront()
            var dialogResult = showConfirmDialog(
              s"""A new version of HearthStats Companion is available: ${latestRelease.getVersion}
                        |${latestRelease.getBody}
                        |            
                        |
                        | ${t("would_u_like_to_install_update")}""".stripMargin,
              "HearthStats " + t("uploader_updates_avail"),
              YES_NO_OPTION)
            if (dialogResult == YES_OPTION) {
              updater.run(latestRelease)
            } else {
              dialogResult = showConfirmDialog(
                t("would_you_like_to_disable_updates"),
                t("disable_update_checking"),
                YES_NO_OPTION)
              if (dialogResult == YES_OPTION) {
                val options = Array(t("button.ok"))
                val panel = new JPanel
                val lbl = new JLabel(t("reenable_updates_any_time"))
                panel.add(lbl)
                showOptionDialog(panel, t("updates_disabled_msg"), NO_OPTION, options.toArray)
                enableUpdateCheck.set(false)
              }
            }
          }
        } else uiLog.warn("Unable to determine latest available version")
      } catch {
        case e: Exception => {
          e.printStackTrace(System.err)
          companionFrame.notify("Update Checking Error", "Unable to determine the latest available version")
        }
      }
    }
  }
}