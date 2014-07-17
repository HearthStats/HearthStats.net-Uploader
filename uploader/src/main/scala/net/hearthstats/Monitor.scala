package net.hearthstats

import net.hearthstats.Constants.PROFILES_URL
import net.hearthstats.util.Translations.t
import java.io.IOException
import java.net.URI
import java.util.EnumSet
import java.util.Observable
import java.util.Observer
import net.hearthstats.analysis.AnalyserEvent
import net.hearthstats.analysis.AnalyserEvent._
import net.hearthstats.log.Log
import net.hearthstats.log.LogPane
import net.hearthstats.logmonitor.HearthstoneLogMonitor
import net.hearthstats.notification.NotificationQueue
import net.hearthstats.state.Screen._
import net.hearthstats.state.ScreenGroup
import net.hearthstats.ui.AboutPanel
import net.hearthstats.ui.ClickableDeckBox
import net.hearthstats.ui.DecksTab
import net.hearthstats.ui.MatchEndPopup
import net.hearthstats.ui.MatchPanel
import net.hearthstats.ui.OptionsPanel
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import net.hearthstats.analysis.HearthstoneAnalyser
import net.hearthstats.config.{ Environment, OS, MonitoringMethod, MatchPopup }
import net.hearthstats.state.Screen
import net.hearthstats.ui.CompanionFrame
import scala.swing.Swing
import net.hearthstats.ui.Button
import javax.swing.JOptionPane
import scala.swing.MainFrame
import grizzled.slf4j.Logging
import Monitor._

class Monitor(val environment: Environment) extends Observer with Logging {

  val _hsHelper: ProgramHelper = environment.programHelper
  lazy val hearthstoneLogMonitor = new HearthstoneLogMonitor(environment.hearthstoneLogFile)
  val _analytics = AnalyticsTracker.tracker

  var _hearthstoneDetected: Boolean = _
  var _playingInMatch: Boolean = false
  var nextGcTime: Long = 0
  val mainFrame = new CompanionFrame(environment, this)

  def start() {
    HearthstoneAnalyser.monitor = this
    if (Config.analyticsEnabled) {
      debug("Enabling analytics")
      _analytics.trackEvent("app", "AppStart")
    }

    mainFrame.checkForUpdates()
    API.addObserver(this)
    HearthstoneAnalyser.addObserver(this)
    _hsHelper.addObserver(this)
    if (mainFrame.checkForUserKey()) {
      poller.start()
    } else {
      System.exit(1)
    }
    if (environment.os == OS.OSX) {
      Log.info(t("waiting_for_hs"))
    } else {
      Log.info(t("waiting_for_hs_windowed"))
    }
  }

  /**
   * Sets up the Hearthstone log monitoring if enabled, or stops if it is
   * disabled
   */
  def setupLogMonitoring() {
    setMonitorHearthstoneLog(Config.monitoringMethod == MonitoringMethod.SCREEN_LOG)
  }

  var title = "HearthStats Companion"
  private def _submitMatchResult(hsMatch: HearthstoneMatch): Unit = {
    if ("Arena" == hsMatch.mode && HearthstoneAnalyser.isNewArena) {
      val run = new ArenaRun()
      run.setUserClass(hsMatch.userClass)
      Log.info("Creating new " + run.getUserClass + " arena run")
      mainFrame.notify("Creating new " + run.getUserClass + " arena run")
      API.createArenaRun(run)
      HearthstoneAnalyser.setIsNewArena(false)
    }
    val header = t("match.end.submitting")
    val message = hsMatch.toString
    mainFrame.notify(header, message)
    Log.matchResult(header + ": " + message)
    if (Config.analyticsEnabled()) {
      _analytics.trackEvent("app", "Submit" + hsMatch.mode + "Match")
    }
    API.createMatch(hsMatch)
    hsMatch.submitted = true
  }

  protected def _handleHearthstoneFound() {
    if (!_hearthstoneDetected) {
      _hearthstoneDetected = true
      debug("  - hearthstoneDetected")
      if (Config.showHsFoundNotification) {
        mainFrame.notify("Hearthstone found")
      }
      setupLogMonitoring()
    }
    debug("  - screen capture")
    val image = _hsHelper.getScreenCapture
    if (image == null)
      debug("  - screen capture returned null")
    else if (image.getWidth >= 1024) {
      debug("  - analysing image")
      HearthstoneAnalyser.analyze(image)
      image.flush()
    }
  }

  protected def _handleHearthstoneNotFound() {
    if (_hearthstoneDetected) {
      _hearthstoneDetected = false
      debug("  - changed hearthstoneDetected to false")
      if (Config.showHsClosedNotification) {
        mainFrame.notify("Hearthstone closed")
        HearthstoneAnalyser.reset()
      }
    }
  }

  private def pollHsImpl() {
    var err = false
    while (!err) {
      try {
        if (_hsHelper.foundProgram)
          _handleHearthstoneFound()
        else {
          debug("  - did not find Hearthstone")
          _handleHearthstoneNotFound()
        }
        mainFrame.updateTitle()
        // We need to manually trigger GC due to memory leakage that occurs on Windows 8 if we leave GC to the JVM
        if (nextGcTime > System.currentTimeMillis)
          Thread.sleep(POLLING_INTERVAL_IN_MS)
        else {
          System.gc()
          nextGcTime = System.currentTimeMillis + GC_INTERVAL_IN_MS
        }
      } catch {
        case ex: Exception => {
          ex.printStackTrace(System.err)
          error("  - exception which is not being handled:", ex)
          Log.error("ERROR: " + ex.getMessage +
            ". You will need to restart HearthStats Companion.", ex)
          err = true
        }
      } finally {
        debug("<-- finished")
      }
    }
  }

  /**
   * Checks whether the match result is complete, showing a popup if necessary
   * to fix the match data, and then submits the match when ready.
   *
   * @param hsMatch
   *          The match to check and submit.
   */
  def checkMatchResult(hsMatch: HearthstoneMatch): Unit =
    if (!hsMatch.submitted && hsMatch.initialized) {
      mainFrame.matchPanel.updateMatchClassSelectorsIfSet(hsMatch)
      val matchPopup = Config.showMatchPopup
      val showPopup = matchPopup match {
        case MatchPopup.ALWAYS => true
        case MatchPopup.INCOMPLETE => !hsMatch.isDataComplete
        case MatchPopup.NEVER => false
        case _ => throw new UnsupportedOperationException("Unknown config option " + Config.showMatchPopup)
      }
      if (showPopup) {
        Swing.onEDT {
          try {
            var matchHasValidationErrors = !hsMatch.isDataComplete
            var infoMessage: String = null
            do {
              if (infoMessage == null) {
                infoMessage = if ((matchPopup == MatchPopup.INCOMPLETE))
                  t("match.popup.message.incomplete")
                else
                  t("match.popup.message.always")
              }
              mainFrame.bringWindowToFront()
              val buttonPressed = MatchEndPopup.showPopup(mainFrame, hsMatch, infoMessage, t("match.popup.title"))
              matchHasValidationErrors = !hsMatch.isDataComplete
              buttonPressed match {
                case Button.SUBMIT => if (matchHasValidationErrors) {
                  infoMessage = "Some match information is incomplete.<br>Please update these details then click Submit to submit the match to HearthStats:"
                } else {
                  _submitMatchResult(hsMatch)
                }
                case Button.CANCEL => return
              }
            } while (matchHasValidationErrors);
          } catch {
            case e: Exception => Main.showErrorDialog("Error submitting match result", e)
          }
        }
      } else
        try {
          _submitMatchResult(hsMatch)
        } catch {
          case e: Exception => Main.showErrorDialog("Error submitting match result", e)
        }
    }

  private def handleAnalyserEvent(changed: AnalyserEvent) = changed match {
    case AnalyserEvent.ARENA_END =>
      mainFrame.notify("End of Arena Run Detected")
      Log.info("End of Arena Run Detected")
      API.endCurrentArenaRun()

    case COIN =>
      mainFrame.notify("Coin Detected")
      Log.info("Coin Detected")
      mainFrame.matchPanel.updateCurrentMatchUi()

    case DECK_SLOT =>
      val deck = DeckUtils.getDeckFromSlot(HearthstoneAnalyser.getDeckSlot)
      if (deck.isEmpty) {
        mainFrame.tabbedPane.setSelectedIndex(2)
        mainFrame.bringWindowToFront()
        Main.showMessageDialog(mainFrame, "Unable to determine what deck you have in slot #" + HearthstoneAnalyser.getDeckSlot +
          "\n\nPlease set your decks in the \"Decks\" tab.")
      } else {
        mainFrame.notify("Deck Detected", deck.get.name)
        Log.info("Deck Detected: " + deck.get.name + " Detected")
        showDeckOverlay()
      }

    case MODE =>
      _playingInMatch = false
      mainFrame.matchPanel.setCurrentMatchEnabled(false)
      val mode = HearthstoneAnalyser.getMode
      if (Config.showModeNotification) {
        debug(mode + " level " + HearthstoneAnalyser.getRankLevel)
        if ("Ranked" == mode) {
          mainFrame.notify(mode + " Mode Detected", "Rank Level " + HearthstoneAnalyser.getRankLevel)
        } else {
          mainFrame.notify(mode + " Mode Detected")
        }
      }
      if ("Ranked" == mode) {
        Log.info(mode + " Mode Detected - Level " + HearthstoneAnalyser.getRankLevel)
      } else {
        Log.info(mode + " Mode Detected")
      }

    case NEW_ARENA =>
      if (HearthstoneAnalyser.isNewArena) mainFrame.notify("New Arena Run Detected")
      Log.info("New Arena Run Detected")

    case OPPONENT_CLASS =>
      mainFrame.notify("Playing vs " + HearthstoneAnalyser.getOpponentClass)
      Log.info("Playing vs " + HearthstoneAnalyser.getOpponentClass)
      mainFrame.matchPanel.updateCurrentMatchUi()

    case OPPONENT_NAME =>
      mainFrame.notify("Opponent: " + HearthstoneAnalyser.getOpponentName)
      Log.info("Opponent: " + HearthstoneAnalyser.getOpponentName)
      mainFrame.matchPanel.updateCurrentMatchUi()

      mainFrame.matchPanel.setCurrentMatchEnabled(false)
      mainFrame.matchPanel.updateCurrentMatchUi()
    case SCREEN =>
      handleScreenChange()

    case YOUR_CLASS =>
      mainFrame.notify("Playing as " + HearthstoneAnalyser.getYourClass)
      Log.info("Playing as " + HearthstoneAnalyser.getYourClass)
      mainFrame.matchPanel.updateCurrentMatchUi()

    case YOUR_TURN =>
      if (Config.showYourTurnNotification)
        mainFrame.notify((if (HearthstoneAnalyser.isYourTurn) "Your" else "Opponent") + " turn detected")
      Log.info((if (HearthstoneAnalyser.isYourTurn) "Your" else "Opponent") + " turn detected")
      mainFrame.matchPanel.updateCurrentMatchUi()

    case ERROR_ANALYSING_IMAGE =>
      mainFrame.notify("Error analysing opponent name image")
      Log.info("Error analysing opponent name image")

    case _ =>
      mainFrame.notify("Unhandled event")
      Log.info("Unhandled event")
  }

  def handleGameResult(): Unit = {
    _playingInMatch = false
    mainFrame.matchPanel.setCurrentMatchEnabled(false)
    mainFrame.notify(HearthstoneAnalyser.hsMatch.describeResult + " Detected")
    Log.info(HearthstoneAnalyser.hsMatch.describeResult + " Detected")
    checkMatchResult(HearthstoneAnalyser.hsMatch)
    mainFrame.matchPanel.updateCurrentMatchUi()
  }

  private def handleScreenChange(): Unit = HearthstoneAnalyser.screen match {
    case Screen.ARENA_END | ARENA_LOBBY | PLAY_LOBBY =>
      if (_playingInMatch && HearthstoneAnalyser.hsMatch.result.isEmpty) {
        _playingInMatch = false
        mainFrame.notify("Detection Error", "Match result was not detected.")
        Log.info("Detection Error: Match result was not detected.")
        checkMatchResult(HearthstoneAnalyser.hsMatch)
      }
      _playingInMatch = false

    case MATCH_VS | FINDING_OPPONENT =>
      Log.divider()
      setupLogMonitoring()
      mainFrame.matchPanel.resetMatchClassSelectors()

    case s if s.group == ScreenGroup.MATCH_START =>
      mainFrame.matchPanel.setCurrentMatchEnabled(true)
      _playingInMatch = true

    case s if (s.group == ScreenGroup.MATCH_END && DO_NOT_NOTIFY_SCREENS.contains(s) && Config.showScreenNotification) =>
      if (HearthstoneAnalyser.screen == PRACTICE_LOBBY)
        mainFrame.notify(HearthstoneAnalyser.screen.title + " Screen Detected", "Results are not tracked in practice mode")
      else
        mainFrame.notify(HearthstoneAnalyser.screen.title + " Screen Detected")

    case PRACTICE_LOBBY =>
      Log.info(HearthstoneAnalyser.screen.title + " Screen Detected. Result tracking disabled.")

    case s =>
      Log.info(s.title + " Screen Detected")
  }

  private def showDeckOverlay(): Unit = {
    if (Config.showDeckOverlay && "Arena" != HearthstoneAnalyser.getMode) {
      val selectedDeck = DeckUtils.getDeckFromSlot(HearthstoneAnalyser.getDeckSlot)
      if (selectedDeck.isDefined && selectedDeck.get.isValid) {
        ClickableDeckBox.showBox(selectedDeck.get, hearthstoneLogMonitor.cardEvents)
      } else {
        val message = selectedDeck match {
          case Some(deck) => s"Invalid or empty deck, <a href='http://hearthstats.net/decks/${deck.slug}/edit'>edit it on HearthStats.net</a> to display deck overlay (you will need to restart HearthStats Companion)"
          case None => "Invalid or empty deck, edit it on HearthStats.net to display deck overlay (you will need to restart the uploader)"
        }
        mainFrame.notify(message)
        Log.info(message)
      }
    }
  }

  private def _handleApiEvent(changed: AnyRef) = changed.toString match {
    case "error" =>
      mainFrame.notify("API Error", API.message)
      Log.error("API Error: " + API.message)
      Main.showMessageDialog(mainFrame, "API Error: " + API.message)

    case "result" =>
      Log.info("API Result: " + API.message)
      val lastMatch = HearthstoneAnalyser.hsMatch
      lastMatch.id = API.lastMatchId
      mainFrame.matchPanel.setCurrentMatchEnabled(false)
      mainFrame.matchPanel.updateCurrentMatchUi()
      mainFrame.matchPanel.lastMatch = lastMatch
      if (API.message.matches(".*(Edit match|Arena match successfully created).*")) {
        HearthstoneAnalyser.hsMatch = new HearthstoneMatch
        mainFrame.matchPanel.resetMatchClassSelectors()
        Log.divider()
      }

  }

  private def _handleProgramHelperEvent(changed: AnyRef) {
    Log.info(changed.toString)
    if (changed.toString.matches(".*minimized.*"))
      mainFrame.notify("Hearthstone Minimized", "Warning! No detection possible while minimized.")
    if (changed.toString.matches(".*fullscreen.*"))
      JOptionPane.showMessageDialog(mainFrame, "Hearthstats.net Uploader Warning! \n\nNo detection possible while Hearthstone is in fullscreen mode.\n\nPlease set Hearthstone to WINDOWED mode and close and RESTART Hearthstone.\n\nSorry for the inconvenience.")
    if (changed.toString.matches(".*restored.*"))
      mainFrame.notify("Hearthstone Restored", "Resuming detection ...")
  }

  override def update(dispatcher: Observable, changed: AnyRef) {
    val dispatcherClass = if (dispatcher == null) "" else dispatcher.getClass.getCanonicalName
    if (dispatcherClass.startsWith("net.hearthstats.analysis.HearthstoneAnalyser")) {
      try handleAnalyserEvent(changed.asInstanceOf[AnalyserEvent])
      catch {
        case e: IOException => Main.showErrorDialog("Error handling analyzer event", e)
      }
    }
    if (dispatcherClass.startsWith("net.hearthstats.API")) _handleApiEvent(changed)
    if (dispatcherClass.matches(".*ProgramHelper(Windows|Osx)?")) _handleProgramHelperEvent(changed)
  }

  private var poller: Thread = new Thread(new Runnable() {
    override def run() {
      pollHsImpl()
    }
  })

  def setMonitorHearthstoneLog(monitorHearthstoneLog: Boolean) {
    debug(s"setMonitorHearthstoneLog($monitorHearthstoneLog)")
    if (monitorHearthstoneLog) {
      val configWasCreated = _hsHelper.createConfig(environment)
      if (_hearthstoneDetected) {
        if (configWasCreated) {
          Log.help("Hearthstone log.config changed &mdash; please restart Hearthstone so that it starts generating logs")
        }
      }
    } else {
      hearthstoneLogMonitor.stop()
    }
  }
}

object Monitor {
  val debugLog: Logger = LoggerFactory.getLogger(classOf[Monitor])

  val POLLING_INTERVAL_IN_MS = 100
  val GC_INTERVAL_IN_MS = 3000
  val DO_NOT_NOTIFY_SCREENS = EnumSet.of(
    COLLECTION,
    COLLECTION_ZOOM,
    MAIN_TODAYSQUESTS,
    TITLE)
}
