package net.hearthstats

import java.io.File

import net.hearthstats.config.{ Environment, NotificationType, OS }
import net.hearthstats.updater.api.model.Release

/**
 * Implementation which can be injected in test scenario.
 */
object EnvironmentTest extends Environment {
  val hearthstoneLogFile = File.createTempFile("log", ".txt").getAbsolutePath

  val os: OS = OS.WINDOWS
  val programHelper = ProgramHelperTest
  val osxNotificationsSupported = false
  val extractionFolder = null
  val hearthstoneConfigFolder = ""

  def newNotificationQueue(notificationType: NotificationType) = null
  def performApplicationUpdate(release: Release) = ""
}

object ProgramHelperTest extends ProgramHelper {
  def foundProgram = true
  def getHSWindowBounds = null
  def getScreenCapture = null
  override def createConfig(environment: Environment) = false

}