package net.hearthstats.win

import net.hearthstats.config.{OS, Environment}
import net.hearthstats.notification.{DialogNotificationQueue, NotificationQueue}
import java.io.File
import net.hearthstats.ProgramHelper
import org.apache.commons.lang3.StringUtils

/**
 * Windows environment.
 */
class EnvironmentWin extends Environment {

  override def os: OS = OS.WINDOWS

  // Windows only supports HearthStats notifications
  override def newNotificationQueue: NotificationQueue = new DialogNotificationQueue

  override def newProgramHelper: ProgramHelper = new ProgramHelperWindows

  // OS X notifications are not supported
  override def osxNotificationsSupported: Boolean = false

  override def extractionFolder =  {
    var path = "tmp"
    (new File(path)).mkdirs
    path
  }


  override def hearthstoneConfigFolder = {
    val appdata = System.getenv("LOCALAPPDATA")
    if (StringUtils.isBlank(appdata)) {
      throw new RuntimeException("Cannot find LOCALAPPDATA directory")
    }
    val folder = new File(appdata + "/Blizzard/Hearthstone")
    folder.getAbsolutePath
  }


  override def hearthstoneLogFile = {
    var logLocation: String = null
    // Attempt to connect to Hearthstone to find out where it is running from
    val programHelper = new ProgramHelperWindows
    if (programHelper.foundProgram() && programHelper.getHearthstoneProcessFolder != null) {
      logLocation = programHelper.getHearthstoneProcessFolder + "\\Hearthstone_Data\\output_log.txt"
    }
    else {
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
}