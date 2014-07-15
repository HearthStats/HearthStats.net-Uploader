package net.hearthstats.config

import net.hearthstats.notification.NotificationQueue
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.updater.api.model.Release

/**
 * Represents the environment-specific information that varies between OS X and Windows.
 */
abstract class Environment {

  /**
   * Which operating system this environment is running.
   */
  val os: OS

  /**
   * The location where temporary files can be extracted.
   */
  val extractionFolder: String

  /**
   * The location of the Hearthstone log.config file.
   */
  val hearthstoneConfigFolder: String

  /**
   * The location of the Hearthstone log output file.
   */
  val hearthstoneLogFile: String

  /**
   * Provides a ProgramHelper object that is suitable for the current environment.
   */
  val programHelper: ProgramHelper

  /**
   * Whether OS X notifications are supported in the current environment.
   */
  val osxNotificationsSupported: Boolean

  /**
   * Creates a new NotificationQueue object of the requested type, if suitable for the current environment.
   */
  def newNotificationQueue(notificationType: NotificationType): NotificationQueue


  /**
   * Performs an update of the HearthStats Companion. This method should quit the app then start the update.
   */
  def performApplicationUpdate(release: Release): String

}

object Environment extends Logging {

  /**
   * Looks up a system property, but without throwing an exception if the property does not exist.
   *
   * @param property the name for a standard system property
   * @return the requested property, or blank if the property was not set
   */
  def systemProperty(property: String): String = {
    try {
      System.getProperty(property)
    } catch {
      case ex: SecurityException => {
        warn(s"Caught a SecurityException reading the system property '$property', defaulting to blank string.")
        ""
      }
    }
  }

  /**
   * Determines whether the operating system is at least a particular version.
   *
   * @param requiredMajor Major version number (before the dot)
   * @param requiredMinor Minor version number (after the dot)
   * @return true if the OS is equal to or later than the given version.
   */
  def isOsVersionAtLeast(requiredMajor: Int, requiredMinor: Int): Boolean = {
    val osVersion: String = systemProperty("os.version")
    try {
      val osVersionSplit = osVersion.split("\\.")
      val versionMajor = osVersionSplit(0).toInt
      val versionMinor =
        if (osVersionSplit.length > 1)
          osVersionSplit(1).toInt
        else 0
      if (versionMajor > requiredMajor)
        true
      else if (versionMajor == requiredMajor) {
        if (versionMinor >= requiredMinor)
          true
        else false
      } else
        false
    } catch {
      case e: NumberFormatException =>
        warn(s"Error parsing os.version $osVersion", e)
        false
    }
  }

}