package net.hearthstats.win

import net.hearthstats.config.{OS, Environment}
import net.hearthstats.notification.{DialogNotificationQueue, NotificationQueue}
import java.io.File

/**
 * Windows environment.
 */
class EnvironmentWin extends Environment {

  override def os: OS = OS.WINDOWS

  // Windows only supports HearthStats notifications
  override def newNotificationQueue: NotificationQueue = new DialogNotificationQueue

  // OS X notifications are not supported
  override def osxNotificationsSupported: Boolean = false

  override def extractionFolder =  {
    var path = "tmp"
    (new File(path)).mkdirs
    path
  }

}