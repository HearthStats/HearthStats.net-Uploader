package net.hearthstats

import java.io.File

import net.hearthstats.config._
import net.hearthstats.updater.api.model.Release

/**
 * Implementation which can be injected in test scenario.
 */
object EnvironmentTest extends Environment {
  val hearthstoneLogFile = File.createTempFile("log", ".txt").getAbsolutePath

  val os: OS = OS.WINDOWS
  val config: Config = ConfigTest
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

object ConfigTest extends Config {
  var monitoringMethod: MonitoringMethod = MonitoringMethod.SCREEN
  var notifyOverall: Boolean = true
  var notifyHsFound: Boolean = true
  var notifyHsClosed: Boolean = true
  var notifyScreen: Boolean = true
  var notifyMode: Boolean = true
  var notifyDeck: Boolean = true
  var notifyTurn: Boolean = true
  var notificationType: NotificationType = NotificationType.HEARTHSTATS
  var windowX: Int = 0
  var windowY: Int = 0
  var windowHeight: Int = 700
  var windowWidth: Int = 600
  var deckX: Int = 0
  var deckY: Int = 0
  var deckHeight: Int = 600
  var deckWidth: Int = 485
}