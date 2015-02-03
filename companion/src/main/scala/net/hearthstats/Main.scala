package net.hearthstats

import java.awt.Component
import java.io.File

import scala.util.control.NonFatal

import com.softwaremill.macwire.MacwireMacros.wire
import com.softwaremill.macwire.Tagging._

import grizzled.slf4j.Logging
import javax.swing.{ JFrame, JOptionPane, WindowConstants }
import net.hearthstats.companion.{ CompanionState, DeckOverlayModule, GameMonitor, ScreenEvents }
import net.hearthstats.config.{ Application, Environment, UserConfig }
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.game.{ HearthstoneLogMonitor, LogParser, MatchState }
import net.hearthstats.game.imageanalysis.{ IndividualPixelAnalyser, LobbyAnalyser, ScreenAnalyser }
import net.hearthstats.hstatsapi.{ API, CardUtils, DeckUtils, MatchUtils }
import net.hearthstats.modules.{ FileUploaderFactory, ReplayHandler, VideoEncoderFactory }
import net.hearthstats.ui.{ CompanionFrame, ExportDeckBox, MatchEndPopup }
import net.hearthstats.ui.deckoverlay._
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.DialogNotification
import net.hearthstats.util.{ AnalyticsTrackerFactory, FileObserver, Translation, TranslationConfig, Updater }
import net.sourceforge.tess4j.Tesseract

class Main(
  environment: Environment,
  config: UserConfig,
  programHelper: ProgramHelper) extends Logging {

  private var ocrLanguage: String = "eng"
  import config._

  val translationConfig = TranslationConfig("net.hearthstats.resources.Main", "en")
  val uiLog = wire[Log]
  val translation = wire[Translation]
  val updater: Updater = wire[Updater]

  val initialCompanionState = new CompanionState
  val initialMatchState = new MatchState
  val api = wire[API]
  val cardUtils = wire[CardUtils]
  val deckUtils = wire[DeckUtils]

  val notificationQueue = environment.newNotificationQueue(notificationType)

  val analytics = AnalyticsTrackerFactory.tracker(enableAnalytics)

  val screenAnalyser = wire[ScreenAnalyser]
  val individualPixelAnalyser = wire[IndividualPixelAnalyser]
  val lobbyAnalyser = wire[LobbyAnalyser]
  val hsMatch = new HearthstoneMatch

  val rectangleConfig = config.deckOverlay.taggedWith[UserDeckOverlayRectangle]
  val deckOverlay = wire[UserOverlaySwing]
  val opponentConfig = config.opponentOverlay.taggedWith[OpponentDeckOverlayRectangle]
  val opponentOverlay = wire[OpponentOverlaySwing]

  val matchEndPopup = wire[MatchEndPopup]

  if (config.gameLogFile.get == "") {
    config.gameLogFile.set(environment.hearthstoneLogFile)
  }
  val fileObserver = wire[FileObserver]
  val logParser = wire[LogParser]
  val logMonitor = wire[HearthstoneLogMonitor]
  val deckOverlayModule = wire[DeckOverlayModule]
  val videoEncoderFactory = wire[VideoEncoderFactory]
  val fileUploaderFactory = wire[FileUploaderFactory]

  val companionEvents = wire[ScreenEvents]
  val exportDeckBox = wire[ExportDeckBox]
  val mainFrame: CompanionFrame = wire[CompanionFrame]
  val replayHandler = wire[ReplayHandler]
  val startup: Startup = wire[Startup]
  val matchUtils = wire[MatchUtils]

  val monitor: GameMonitor = wire[GameMonitor]

  def start(): Unit = {
    val loadingNotification = new DialogNotification("HearthStats Companion", "Loading ...")
    loadingNotification.show()
    logSystemInformation()
    updater.cleanUp()
    cleanupDebugFiles()
    mainFrame.createAndShowGui()
    loadingNotification.close()
    programHelper.createConfig(environment, uiLog)
    startup.start()
    monitor.start()
  }

  private def logSystemInformation(): Unit = {
    if (isInfoEnabled) {
      info("**********************************************************************")
      info(s"  Starting HearthStats Companion ${Application.version} on ${environment.os}")
      info("  os.name=" + Environment.systemProperty("os.name"))
      info("  os.version=" + Environment.systemProperty("os.version"))
      info("  os.arch=" + Environment.systemProperty("os.arch"))
      info("  java.runtime.version=" + Environment.systemProperty("java.runtime.version"))
      info("  java.class.path=" + Environment.systemProperty("java.class.path"))
      info("  java.library.path=" + Environment.systemProperty("java.library.path"))
      info("  user.language=" + Environment.systemProperty("user.language"))
      info("**********************************************************************")
    }
  }

  private def cleanupDebugFiles(): Unit = {
    try {
      val folder = new File(environment.extractionFolder)
      if (folder.exists) {
        val files = folder.listFiles
        for (file <- files if file.isFile && file.getName.startsWith("class-") && file.getName.endsWith(".png")) {
          file.delete()
        }
      }
    } catch {
      case NonFatal(e) => warn("Ignoring exception when cleaning up debug files", e)
    }
  }

  def setupTesseract(outPath: String): Unit = {
    val instance = Tesseract.getInstance
    instance.setDatapath(outPath + "tessdata")
    instance.setLanguage(ocrLanguage)
  }
}

object Main extends Logging {
  def showMessageDialog(parentComponent: Component, message: String): Unit = {
    val op = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE)
    val dialog = op.createDialog(parentComponent, "HearthStats.net")
    dialog.setAlwaysOnTop(true)
    dialog.setModal(true)
    dialog.setFocusableWindowState(true)
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    dialog.setVisible(true)
  }

  def showErrorDialog(message: String, e: Throwable): Unit = {
    error(message, e)
    val frame = new JFrame
    frame.setFocusableWindowState(true)
    showMessageDialog(null, message + "\n" + e.getMessage + "\n\nSee log.txt for details")
  }
}