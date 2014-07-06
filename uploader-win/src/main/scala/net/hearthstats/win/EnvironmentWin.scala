package net.hearthstats.win

import net.hearthstats.config.{NotificationType, OS, Environment}
import net.hearthstats.notification.{ DialogNotificationQueue, NotificationQueue }
import java.io.File
import org.apache.commons.lang3.StringUtils
import net.hearthstats.updater.api.model.Release

/**
 * Windows environment.
 */
class EnvironmentWin extends Environment {

  val os: OS = OS.WINDOWS

  val programHelper: ProgramHelperWindows = new ProgramHelperWindows

  // OS X notifications are not supported
  val osxNotificationsSupported = false

  val extractionFolder = {
    val path = "tmp"
    (new File(path)).mkdirs
    path
  }

  val hearthstoneConfigFolder = {
    val appdata = System.getenv("LOCALAPPDATA")
    if (StringUtils.isBlank(appdata)) {
      throw new RuntimeException("Cannot find LOCALAPPDATA directory")
    }
    val folder = new File(appdata + "/Blizzard/Hearthstone")
    folder.getAbsolutePath
  }

  val hearthstoneLogFile = {
    var logLocation: String = null
    // Attempt to connect to Hearthstone to find out where it is running from
    if (programHelper.foundProgram && programHelper.getHearthstoneProcessFolder != null) {
      logLocation = programHelper.getHearthstoneProcessFolder + "\\Hearthstone_Data\\output_log.txt"
    } else {
      logLocation = System.getenv("PROGRAMFILES(X86)")
      if (StringUtils.isBlank(logLocation)) {
        logLocation = System.getenv("PROGRAMFILES")
        if (StringUtils.isBlank(logLocation)) {
          throw new RuntimeException("Cannot find Program Files directory")
        }
      }
      logLocation = logLocation + "\\Hearthstone\\Hearthstone_Data\\output_log.txt"
    }
    val logFile = new File(logLocation)
    logFile.getAbsolutePath
  }

  // Windows only supports the DialogNotificationQueue, so always return that regardless of what is requested.
  def newNotificationQueue(notificationType: NotificationType): NotificationQueue = new DialogNotificationQueue

  /**
   * Performs an update of the HearthStats Uploader. This method should quit the uploader then start the update.
   */
  def performApplicationUpdate(release: Release): String = { "not implemented" }
}