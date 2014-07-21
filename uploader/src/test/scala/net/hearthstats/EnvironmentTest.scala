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
  val configUserKey: ConfigValue[String] = "a9efa89e4a7a806d428bdda944d7b48f" // a specific test key
  val configApiBaseUrl: ConfigValue[String] = API.DefaultApiBaseUrl
  val enableAnalytics: ConfigValue[Boolean] = true
  val enableDeckOverlay: ConfigValue[Boolean] = true
  val enableStartMin: ConfigValue[Boolean] = false
  val enableMinToTray: ConfigValue[Boolean] = true
  val enableUpdateCheck: ConfigValue[Boolean] = false
  val optionGameLanguage: ConfigValue[GameLanguage] = GameLanguage.EN
  val optionMatchPopup: ConfigValue[MatchPopup] = MatchPopup.INCOMPLETE
  val optionMonitoringMethod: ConfigValue[MonitoringMethod] = MonitoringMethod.SCREEN
  val optionNotificationType: ConfigValue[NotificationType] = NotificationType.HEARTHSTATS
  val notifyOverall: ConfigValue[Boolean] = true
  val notifyHsFound: ConfigValue[Boolean] = true
  val notifyHsClosed: ConfigValue[Boolean] = true
  val notifyScreen: ConfigValue[Boolean] = true
  val notifyMode: ConfigValue[Boolean] = true
  val notifyDeck: ConfigValue[Boolean] = true
  val notifyTurn: ConfigValue[Boolean] = true
  val windowX: ConfigValue[Int] = 0
  val windowY: ConfigValue[Int] = 0
  val windowHeight: ConfigValue[Int] = 700
  val windowWidth: ConfigValue[Int] = 600
  val deckX: ConfigValue[Int] = 0
  val deckY: ConfigValue[Int] = 0
  val deckHeight: ConfigValue[Int] = 600
  val deckWidth: ConfigValue[Int] = 485

  implicit def readOnlyConfig[T](value: T): ConfigValue[T] =
    new ConfigValue[T] {
      val get = value
      def set(v: T): Unit = {}
    }
}