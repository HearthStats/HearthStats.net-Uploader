package net.hearthstats.osx

import net.hearthstats.config.{Application, NotificationType, OS, Environment}
import net.hearthstats.notification.{ DialogNotificationQueue, NotificationQueue }
import java.io.File
import net.hearthstats.{Config, ProgramHelper}
import net.hearthstats.updater.api.model.Release
import grizzled.slf4j.Logging
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Mac OS X environment.
 */
class EnvironmentOsx extends Environment with Logging {

  val os: OS = OS.OSX

  val programHelper: ProgramHelper = new ProgramHelperOsx

  val extractionFolder = {
    val libFolder = new File(Environment.systemProperty("user.home") + "/Library/Application Support/HearthStatsUploader")
    libFolder.mkdir
    libFolder.getAbsolutePath
  }

  val hearthstoneConfigFolder = {
    val configFolder = new File(Environment.systemProperty("user.home") + "/Library/Preferences/Blizzard/Hearthstone")
    configFolder.getAbsolutePath
  }

  val hearthstoneLogFile = {
    val logFile = new File(Environment.systemProperty("user.home") + "/Library/Logs/Unity/Player.log")
    logFile.getAbsolutePath
  }

  val osxNotificationsSupported: Boolean =
    Environment.isOsVersionAtLeast(10, 8)

  def newNotificationQueue(notificationType: NotificationType): NotificationQueue = notificationType match {
    case NotificationType.OSX => new OsxNotificationQueue
    case _ => new DialogNotificationQueue
  }

  /**
   * Performs an update of the HearthStats Companion. This method should quit the app then start the update.
   */
  def performApplicationUpdate(release: Release): String = {
    if (release.getOsxAsset == null) {
      s"No Mac OS X download found for version ${release.getVersion}"
    } else {
      val updaterFile: File = new File(extractionFolder + "/updater.jar")
      Application.copyFileFromJarTo("/updater.jar", updaterFile.getPath)

      if (updaterFile.exists) {
        logger.debug(s"Found updater.jar in ${updaterFile.getPath}")
        val javaLibraryPath: File = new File(Config.getJavaLibraryPath)
        val bundlePath: File = javaLibraryPath.getParentFile.getParentFile.getParentFile
        val javaHome: String = System.getProperty("java.home")

        val command: Array[String] = Array[String](
          javaHome + "/bin/java",
          "-jar", updaterFile.getPath,
          "version=" + release.getVersion,
          "assetId=" + release.getOsxAsset.getId,
          "hearthstatsLocation=" + bundlePath.getAbsolutePath,
          "downloadFile=" + extractionFolder + "/update-" + release.getVersion + ".zip")
        logger.info("Running updater command: " + command.mkString(" "));

        try {
          Runtime.getRuntime.exec(command)
          null
        }
        catch {
          case e: Exception => {
            s"Unable to run updater due to error: ${e.getMessage}"
          }
        }
      }
      else {
        s"Unable to locate ${updaterFile.getPath}"
      }
    }
  }
}