package net.hearthstats.config

import net.hearthstats.notification.NotificationQueue
import grizzled.slf4j.Logging


/**
 * Represents the environment-specific information that varies between OS X and Windows.
 */
abstract class Environment {

  /**
   * Which operating system this environment is running.
   */
  def os: OS


  /**
   * The location where temporary files can be extracted.
   */
  def extractionFolder: String


  /**
   * Creates a new NotificationQueue object that is suitable for the current environment.
   */
  def newNotificationQueue: NotificationQueue


  /**
   * Whether OS X notifications are supported in the current environment.
   */
  def osxNotificationsSupported: Boolean

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
    }
    catch {
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
      var versionMinor = 0
      if (osVersionSplit.length > 1) {
        versionMinor = osVersionSplit(1).toInt
      }
      else {
        versionMinor = 0
      }
      if (versionMajor > requiredMajor) {
        return true
      }
      else if (versionMajor == requiredMajor) {
        if (versionMinor >= requiredMinor) {
          return true
        }
        else {
          return false
        }
      }
      else {
        return false
      }
    }
    catch {
      case e: NumberFormatException => {
        warn(s"Error parsing os.version $osVersion", e)
        return false
      }
    }
  }

}