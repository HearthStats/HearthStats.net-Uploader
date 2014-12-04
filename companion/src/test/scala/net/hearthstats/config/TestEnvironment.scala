package net.hearthstats.config

import java.io.File
import javax.imageio.ImageIO
import net.hearthstats.ProgramHelper
import net.hearthstats.ui.notification.{ DialogNotificationQueue, NotificationType }
import net.hearthstats.updater.api.model.Release
import java.awt.Rectangle

/**
 * Implementation which can be injected in test scenario.
 */
object TestEnvironment extends Environment {
  val hearthstoneLogFile = File.createTempFile("log", ".txt").getAbsolutePath

  val os: OS = OS.WINDOWS
  val config: UserConfig = TestConfig
  val programHelper = TestProgramHelper
  val osxNotificationsSupported = false
  val extractionFolder = "tmp"
  val imageCacheFolder = "tmp"
  val hearthstoneConfigFolder = ""

  def newNotificationQueue(notificationType: NotificationType) = new DialogNotificationQueue
  def performApplicationUpdate(release: Release) = ""
}

object TestProgramHelper extends ProgramHelper {
  def foundProgram = true
  def getHSWindowBounds = new Rectangle(10, 10)
  def getScreenCaptureNative = img("/net/hearthstats/game/imageanalysis/play_lobby.png")
  def bringWindowToForeground = false
  //  override def createConfig(environment: Environment) = false

  private def img(fileName: String) = ImageIO.read(TestProgramHelper.getClass().getResourceAsStream(fileName))

}

object TestConfig extends UserConfig {
  override val userKey: ConfigValue[String] = "a9efa89e4a7a806d428bdda944d7b48f" // a specific test key
  override val pollingDelayMs: ConfigValue[Int] = 100
  override val enableAnalytics: ConfigValue[Boolean] = false
  override val enableDeckOverlay: ConfigValue[Boolean] = true
  override val enableStartMin: ConfigValue[Boolean] = false
  override val enableMinToTray: ConfigValue[Boolean] = true
  override val enableUpdateCheck: ConfigValue[Boolean] = false
  override val matchPopup: ConfigValue[MatchPopup] = MatchPopup.INCOMPLETE
  override val notificationType: ConfigValue[NotificationType] = NotificationType.HEARTHSTATS
  override val notifyOverall: ConfigValue[Boolean] = false
  override val notifyHsFound: ConfigValue[Boolean] = true
  override val notifyDeck: ConfigValue[Boolean] = true
  override val windowX: ConfigValue[Int] = 0
  override val windowY: ConfigValue[Int] = 0
  override val windowHeight: ConfigValue[Int] = 700
  override val windowWidth: ConfigValue[Int] = 600
  override val deckX: ConfigValue[Int] = 0
  override val deckY: ConfigValue[Int] = 0
  override val deckHeight: ConfigValue[Int] = 600
  override val deckWidth: ConfigValue[Int] = 485

  implicit def readOnlyConfig[T](value: T): ConfigValue[T] =
    new ConfigValue[T] {
      val get = value
      def set(v: T): Unit = {}
    }
}