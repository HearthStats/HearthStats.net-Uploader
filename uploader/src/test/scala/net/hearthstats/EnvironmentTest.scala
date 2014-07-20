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
  val monitoringMethod: ConfigValue[MonitoringMethod] = MonitoringMethod.SCREEN
  var notifyOverall: ConfigValue[Boolean] = true
  var notifyHsFound: ConfigValue[Boolean] = true
  var notifyHsClosed: ConfigValue[Boolean] = true
  var notifyScreen: ConfigValue[Boolean] = true
  var notifyMode: ConfigValue[Boolean] = true
  var notifyDeck: ConfigValue[Boolean] = true
  var notifyTurn: ConfigValue[Boolean] = true
  var notificationType: ConfigValue[NotificationType] = NotificationType.HEARTHSTATS
  var windowX: ConfigValue[Int] = 0
  var windowY: ConfigValue[Int] = 0
  var windowHeight: ConfigValue[Int] = 700
  var windowWidth: ConfigValue[Int] = 600
  var deckX: ConfigValue[Int] = 0
  var deckY: ConfigValue[Int] = 0
  var deckHeight: ConfigValue[Int] = 600
  var deckWidth: ConfigValue[Int] = 485

  implicit def readOnlyConfig[T](value: T): ConfigValue[T] =
    new ConfigValue[T] {
      val get = value
      def set(v: T): Unit = {}
    }
}