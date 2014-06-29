package net.hearthstats.osx

import net.hearthstats.config.{NotificationType, OS, Environment}
import net.hearthstats.notification.{ DialogNotificationQueue, NotificationQueue }
import java.io.File
import net.hearthstats.ProgramHelper

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

}