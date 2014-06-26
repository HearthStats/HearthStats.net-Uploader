package net.hearthstats.osx

import net.hearthstats.config.{OS, Environment}
import net.hearthstats.notification.{DialogNotificationQueue, NotificationQueue}
import java.io.File
import net.hearthstats.Config

/**
 * Mac OS X environment.
 */
class EnvironmentOsx extends Environment {

  override def os: OS = OS.OSX

  // OS X may use the HearthStats notifications or inbuilt OS X notifications
  override def newNotificationQueue: NotificationQueue = {
    if (Config.useOsxNotifications)
      new OsxNotificationQueue
    else
      new DialogNotificationQueue
  }

  override def extractionFolder = {
    val libFolder = new File(Environment.systemProperty("user.home") + "/Library/Application Support/HearthStatsUploader")
    libFolder.mkdir
    libFolder.getAbsolutePath
  }

  override def osxNotificationsSupported: Boolean = Environment.isOsVersionAtLeast(10, 8)

}