package net.hearthstats.osx

import net.hearthstats.config.{Application, NotificationType, OS, Environment}
import net.hearthstats.notification.{ DialogNotificationQueue, NotificationQueue }
import java.io.{IOException, File}
import net.hearthstats.{Config, ProgramHelper}
import net.hearthstats.log.Log
import net.hearthstats.updater.api.model.Release
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Mac OS X environment.
 */
class EnvironmentOsx extends Environment {

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
   * Performs an update of the HearthStats Uploader. This method should quit the uploader then start the update.
   */
  def performApplicationUpdate(release: Release): String = {
    val updaterFile: File = new File(extractionFolder + "/updater.jar")

    Application.copyFileFromJarTo("/updater.jar", updaterFile.getPath)

    if (updaterFile.exists) {
      System.out.println(s"Found updater.jar in ${updaterFile.getPath}")
      val javaLibraryPath: File = new File(Config.getJavaLibraryPath)
      val bundlePath: File = javaLibraryPath.getParentFile.getParentFile.getParentFile
      val javaHome: String = System.getProperty("java.home")
//      val command: Array[String] = Array[String](javaHome + "/bin/java", "-Dhearthstats.location=" + bundlePath.getAbsolutePath, "-jar", updaterFile.getPath)
      val command: Array[String] = Array[String](
        javaHome + "/bin/java",
        "-jar", updaterFile.getPath,
        "version=" + release.getVersion,
        "assetId=" + release.getOsxAsset.getId,
        "hearthstatsLocation=" + bundlePath.getAbsolutePath,
        "downloadFile=" + extractionFolder + "/update-" + release.getVersion + ".zip")
      System.out.println("EXEC: " + ToStringBuilder.reflectionToString(command))
      try {
        Runtime.getRuntime.exec(command)
        return null
      }
      catch {
        case e: IOException => {
          return "Unable to run updater due to error: " + e.getMessage
        }
      }
    }
    else {
      return "Unable to locate " + updaterFile.getPath
    }
  }
}