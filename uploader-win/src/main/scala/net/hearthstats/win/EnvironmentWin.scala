package net.hearthstats.win

import java.io.File

import grizzled.slf4j.Logging
import net.hearthstats.config._
import net.hearthstats.notification.{DialogNotificationQueue, NotificationQueue}
import net.hearthstats.updater.api.model.Release
import org.apache.commons.lang3.StringUtils

/**
 * Windows environment.
 */
class EnvironmentWin extends Environment with Logging {

  val os: OS = OS.WINDOWS

  val config = new UserConfig

  val programHelper: ProgramHelperWindows = new ProgramHelperWindows

  // OS X notifications are not supported
  val osxNotificationsSupported = false

  val extractionFolder = {
    val path = "tmp"
    (new File(path)).mkdirs()
    path
  }

  val imageCacheFolder = {
    val cacheFolder = new File("cache/cardimages")
    cacheFolder.mkdirs()
    cacheFolder.getAbsolutePath
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
   * Performs an update of the HearthStats Companion. This method should quit the app then start the update.
   */
  def performApplicationUpdate(release: Release): String = {
    if (release.getWindowsAsset == null) {
      s"No Windows download found for version ${release.getVersion}"
    } else {
      val updaterFile: File = new File(extractionFolder + "/updater.jar")
      Application.copyFileFromJarTo("/updater.jar", updaterFile.getPath)

      if (updaterFile.exists) {
        logger.debug(s"Found updater.jar in ${updaterFile.getPath}")

        val command: Array[String] = Array[String](
          "java",
          "-jar", updaterFile.getPath,
          "version=" + release.getVersion,
          "assetId=" + release.getWindowsAsset.getId,
          "hearthstatsLocation=" + (new File(".")).getAbsolutePath,
          "downloadFile=" + extractionFolder + "/update-" + release.getVersion + ".zip")
        logger.info("Running updater command: " + command.mkString(" "));

        try {
          Runtime.getRuntime().exec(command);
          null
        } catch {
          case e: Exception => {
            s"Unable to run updater due to error: ${e.getMessage}"
          }
        }
      } else {
        s"Unable to locate ${updaterFile.getPath}"
      }
    }
  }
}