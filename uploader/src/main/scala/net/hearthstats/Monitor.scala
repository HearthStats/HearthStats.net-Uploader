package net.hearthstats

import java.awt.{ AWTException, Desktop, Dimension, Font }
import java.awt.{ MenuItem, PopupMenu, SystemTray, TrayIcon }
import java.awt.Frame.{ ICONIFIED, MAXIMIZED_BOTH, NORMAL }
import java.awt.event.{ ActionEvent, ActionListener, MouseAdapter, MouseEvent, WindowAdapter, WindowEvent, WindowStateListener }
import java.io.IOException
import java.net.URI
import java.util.{ EnumSet, Observable, Observer }

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing

import org.apache.commons.lang3.StringUtils

import akka.actor.{ Actor, ActorSystem, Cancellable, Props }
import akka.routing.RoundRobinRouter
import grizzled.slf4j.Logging
import javax.swing._
import javax.swing.JOptionPane._
import javax.swing.ScrollPaneConstants.{ HORIZONTAL_SCROLLBAR_AS_NEEDED, VERTICAL_SCROLLBAR_ALWAYS }
import net.hearthstats.Constants.PROFILES_URL
import net.hearthstats.analysis.AnalyserEvent
import net.hearthstats.analysis.AnalyserEvent._
import net.hearthstats.analysis.HearthstoneAnalyser
import net.hearthstats.config.{ Environment, MatchPopup, MonitoringMethod, OS, TempConfig }
import net.hearthstats.log.{ Log, LogPane }
import net.hearthstats.logmonitor.HearthstoneLogMonitor
import net.hearthstats.notification.NotificationQueue
import net.hearthstats.state.Screen
import net.hearthstats.state.Screen._
import net.hearthstats.state.ScreenGroup
import net.hearthstats.ui.{ AboutPanel, Button, ClickableDeckBox, DecksTab, MatchEndPopup, MatchPanel, OptionsPanel }
import net.hearthstats.util.Translations.t

class Monitor(val environment: Environment) extends JFrame with Observer with Logging {

  val POLLING_INTERVAL_IN_MS = 1000 / TempConfig.framesPerSec
  val GC_INTERVAL_IN_MS = 300
  val DO_NOT_NOTIFY_SCREENS = EnumSet.of(
    COLLECTION,
    COLLECTION_ZOOM,
    MAIN_TODAYSQUESTS,
    TITLE)

  val _hsHelper: ProgramHelper = environment.programHelper
  lazy val hearthstoneLogMonitor = new HearthstoneLogMonitor(environment.hearthstoneLogFile)
  val _analytics = AnalyticsTracker.tracker
  val _logText = new LogPane
  val _logScroll = new JScrollPane(_logText, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val _tabbedPane = new JTabbedPane
  val optionsPanel = new OptionsPanel(this)
  val matchPanel = new MatchPanel
  var poller: Cancellable = _

  var _hearthstoneDetected: Boolean = _
  var _notificationQueue: NotificationQueue = environment.newNotificationQueue(Config.notificationType())
  var _playingInMatch: Boolean = false
  var nextGcTime: Long = 0

  val system = ActorSystem("HearthStatsCompanion")

  def start() {
    if (Config.analyticsEnabled) {
      debug("Enabling analytics")
      _analytics.trackEvent("app", "AppStart")
    }
    addWindowListener(new WindowAdapter {
      override def windowClosing(e: WindowEvent) {
        handleClose()
      }
    })
    createAndShowGui()
    showWelcomeLog()
    checkForUpdates()
    API.addObserver(this)
    HearthstoneAnalyser.addObserver(this)
    _hsHelper.addObserver(this)
    if (_checkForUserKey()) {
      val delay = POLLING_INTERVAL_IN_MS.milliseconds
      poller = system.scheduler.schedule(delay, delay, tickActor, Tick)
    } else {
      System.exit(1)
    }
    if (environment.os == OS.OSX) {
      Log.info(t("waiting_for_hs"))
    } else {
      Log.info(t("waiting_for_hs_windowed"))
    }
  }

  val Tick = "tick"
  val tickActor = system.actorOf(Props(new Actor {
    def receive = {
      case Tick => pollHsImpl()
    }
  }) withRouter (RoundRobinRouter(nrOfInstances = 4)))

  private def pollHsImpl() {
    try {
      if (_hsHelper.foundProgram) {
        _handleHearthstoneFound()
      } else {
        debug("  - did not find Hearthstone")
        _handleHearthstoneNotFound()
      }
      _updateTitle()
      // We need to manually trigger GC due to memory leakage that occurs on Windows 8 if we leave GC to the JVM
      if (nextGcTime < System.currentTimeMillis) {
        System.gc()
        nextGcTime = System.currentTimeMillis + GC_INTERVAL_IN_MS
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace(System.err)
        error("  - exception which is not being handled:", ex)
        Log.error(s"ERROR: ${ex.getMessage} - You will need to restart HearthStats Companion.", ex)
        poller.cancel()
    } finally {
      debug("<-- finished")
    }
  }

  private def _checkForUserKey(): Boolean = {
    val userKeySet = Config.getUserKey != "your_userkey_here"
    if (userKeySet) {
      true
    } else {
      Log.warn(t("error.userkey_not_entered"))
      bringWindowToFront()
      showMessageDialog(this, t("error.title") + ":\n\n" + t("you_need_to_enter_userkey") +
        "\n\n" +
        t("get_it_at_hsnet_profiles"))
      val d = Desktop.getDesktop
      try {
        d.browse(new URI(PROFILES_URL))
      } catch {
        case e: IOException => Log.warn("Error launching browser with URL " + PROFILES_URL, e)
      }
      val userkey = showInputDialog(this, t("enter_your_userkey"))
      if (StringUtils.isEmpty(userkey)) {
        false
      } else {
        Config.setUserKey(userkey)
        try {
          optionsPanel.setUserKey(userkey)
          Config.save()
          Log.info(t("UserkeyStored"))
        } catch {
          case e: Exception => Log.warn("Error occurred trying to write settings file, your settings may not be saved", e)
        }
        true
      }
    }
  }

  def handleClose() {
    val p = getLocationOnScreen
    Config.setX(p.x)
    Config.setY(p.y)
    val rect = getSize
    Config.setWidth(rect.getWidth.toInt)
    Config.setHeight(rect.getHeight.toInt)
    try {
      Config.save()
    } catch {
      case t: Exception => Log.warn("Error occurred trying to write settings file, your settings may not be saved", t)
    }
    System.exit(0)
  }

  private def showWelcomeLog() {
    debug("Showing welcome log messages")
    Log.welcome("HearthStats " + t("Companion") + " v" + Config.getVersionWithOs)
    Log.help(t("welcome_1_set_decks"))
    if (environment.os == OS.OSX) {
      Log.help(t("welcome_2_run_hearthstone"))
      Log.help(t("welcome_3_notifications"))
    } else {
      Log.help(t("welcome_2_run_hearthstone_windowed"))
      Log.help(t("welcome_3_notifications_windowed"))
    }
    val logFileLocation = Log.getLogFileLocation
    if (logFileLocation == null) {
      Log.help(t("welcome_4_feedback"))
    } else {
      Log.help(t("welcome_4_feedback_with_log", logFileLocation))
    }
  }

  /**
   * Brings the monitor window to the front of other windows. Should only be
   * used for important events like a modal dialog or error that we want the
   * user to see immediately.
   */
  def bringWindowToFront() {
    Swing.onEDT(setVisible(true))
  }

  /**
   * Overridden version of setVisible based on
   * http://stackoverflow.com/questions
   * /309023/how-to-bring-a-window-to-the-front that should ensure the window is
   * brought to the front for important things like modal dialogs.
   */
  override def setVisible(visible: Boolean) {
    if (!visible || !isVisible) {
      super.setVisible(visible)
    }
    if (visible) {
      var state = super.getExtendedState
      state &= ~ICONIFIED
      super.setExtendedState(state)
      super.setAlwaysOnTop(true)
      super.toFront()
      super.requestFocus()
      super.setAlwaysOnTop(false)
    }
  }

  override def toFront() {
    super.setVisible(true)
    var state = super.getExtendedState
    state &= ~ICONIFIED
    super.setExtendedState(state)
    super.setAlwaysOnTop(true)
    super.toFront()
    super.requestFocus()
    super.setAlwaysOnTop(false)
  }

  private def createAndShowGui() {
    debug("Creating GUI")
    val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
    setIconImage(icon)
    setLocation(Config.getX, Config.getY)
    setSize(Config.getWidth, Config.getHeight)
    add(_tabbedPane)
    _tabbedPane.add(_logScroll, t("tab.log"))
    _tabbedPane.add(matchPanel, t("tab.current_match"))
    _tabbedPane.add(new DecksTab(this), t("tab.decks"))
    _tabbedPane.add(optionsPanel, t("tab.options"))
    _tabbedPane.add(new AboutPanel(), t("tab.about"))
    matchPanel.updateCurrentMatchUi()
    _enableMinimizeToTray()
    setMinimumSize(new Dimension(500, 600))
    setVisible(true)
    if (Config.startMinimized) setState(ICONIFIED)
    _updateTitle()
  }

  private def checkForUpdates() {
    if (Config.checkForUpdates()) {
      Log.info(t("checking_for_updates..."))
      try {
        var latestRelease = Updater.getLatestRelease
        if (latestRelease != null) {
          Log.info(t("latest_v_available") + " " + latestRelease.getVersion)
          if (!latestRelease.getVersion.equalsIgnoreCase("v" + Config.getVersion)) {
            bringWindowToFront()
            val dialogButton = YES_NO_OPTION
            var dialogResult = showConfirmDialog(
              this,
              s"""A new version of HearthStats Companion is available: ${latestRelease.getVersion}
                  |${latestRelease.getBody}
                  |            
                  |
                  | ${t("would_u_like_to_install_update")}""".stripMargin,
              "HearthStats " + t("uploader_updates_avail"),
              dialogButton)
            if (dialogResult == YES_OPTION) {
              Updater.run(environment, latestRelease)
            } else {
              dialogResult = showConfirmDialog(
                null,
                t("would_you_like_to_disable_updates"),
                t("disable_update_checking"),
                dialogButton)
              if (dialogResult == YES_OPTION) {
                val options = Array(t("button.ok"))
                val panel = new JPanel()
                val lbl = new JLabel(t("reenable_updates_any_time"))
                panel.add(lbl)
                showOptionDialog(this, panel, t("updates_disabled_msg"), NO_OPTION,
                  QUESTION_MESSAGE, null, options.toArray, options(0))
                Config.setCheckForUpdates(false)
              }
            }
          }
        } else Log.warn("Unable to determine latest available version")
      } catch {
        case e: Exception => {
          e.printStackTrace(System.err)
          _notify("Update Checking Error", "Unable to determine the latest available version")
        }
      }
    }
  }

  /**
   * Sets up the Hearthstone log monitoring if enabled, or stops if it is
   * disabled
   */
  def setupLogMonitoring() {
    setMonitorHearthstoneLog(Config.monitoringMethod == MonitoringMethod.SCREEN_LOG)
  }

  def setNotificationQueue(_notificationQueue: NotificationQueue) {
    this._notificationQueue = _notificationQueue
  }

  protected def _notify(header: String) {
    _notify(header, "")
  }

  protected def _notify(header: String, message: String) {
    if (Config.showNotifications) _notificationQueue.add(header, message, false)
  }

  protected def _updateTitle() {
    var title = "HearthStats Companion"
    if (_hearthstoneDetected) {
      if (HearthstoneAnalyser.screen != null) {
        title += " - " + HearthstoneAnalyser.screen.title
        if (HearthstoneAnalyser.screen == PLAY_LOBBY && HearthstoneAnalyser.getMode != null) {
          title += " " + HearthstoneAnalyser.getMode
        }
        if (HearthstoneAnalyser.screen == FINDING_OPPONENT) {
          if (HearthstoneAnalyser.getMode != null) {
            title += " for " + HearthstoneAnalyser.getMode + " Game"
          }
        }
        if ("Match Start" == HearthstoneAnalyser.screen.title ||
          "Playing" == HearthstoneAnalyser.screen.title) {
          title += " " +
            (if (HearthstoneAnalyser.getMode == null) "[undetected]" else HearthstoneAnalyser.getMode)
          title += " " + (if (HearthstoneAnalyser.getCoin) "" else "No ") +
            "Coin"
          title += " " +
            (if (HearthstoneAnalyser.getYourClass == null) "[undetected]" else HearthstoneAnalyser.getYourClass)
          title += " VS. " +
            (if (HearthstoneAnalyser.getOpponentClass == null) "[undetected]" else HearthstoneAnalyser.getOpponentClass)
        }
      }
    } else {
      title += " - Waiting for Hearthstone "
    }
    setTitle(title)
  }

  private def _submitMatchResult(hsMatch: HearthstoneMatch) {
    if ("Arena" == hsMatch.mode && HearthstoneAnalyser.isNewArena) {
      val run = new ArenaRun()
      run.setUserClass(hsMatch.userClass)
      Log.info("Creating new " + run.getUserClass + " arena run")
      _notify("Creating new " + run.getUserClass + " arena run")
      API.createArenaRun(run)
      HearthstoneAnalyser.setIsNewArena(false)
    }
    val header = t("match.end.submitting")
    val message = hsMatch.toString
    _notify(header, message)
    Log.matchResult(header + ": " + message)
    if (Config.analyticsEnabled()) {
      _analytics.trackEvent("app", "Submit" + hsMatch.mode + "Match")
    }
    API.createMatch(hsMatch)
    HearthstoneAnalyser.hsMatch = new HearthstoneMatch
  }

  protected def _handleHearthstoneFound() {
    if (!_hearthstoneDetected) {
      _hearthstoneDetected = true
      debug("  - hearthstoneDetected")
      if (Config.showHsFoundNotification) {
        _notify("Hearthstone found")
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
        _notify("Hearthstone closed")
        HearthstoneAnalyser.reset()
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
  private def checkMatchResult(hsMatch: HearthstoneMatch) {
    matchPanel.updateMatchClassSelectorsIfSet(hsMatch)
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
              infoMessage = if ((matchPopup == MatchPopup.INCOMPLETE)) t("match.popup.message.incomplete") else t("match.popup.message.always")
            }
            bringWindowToFront()
            val buttonPressed = MatchEndPopup.showPopup(this, hsMatch, infoMessage, t("match.popup.title"))
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
      _notify("End of Arena Run Detected")
      Log.info("End of Arena Run Detected")
      API.endCurrentArenaRun()

    case COIN =>
      _notify("Coin Detected")
      Log.info("Coin Detected")
      matchPanel.updateCurrentMatchUi()

    case DECK_SLOT =>
      val deck = DeckUtils.getDeckFromSlot(HearthstoneAnalyser.getDeckSlot)
      if (deck.isEmpty) {
        _tabbedPane.setSelectedIndex(2)
        bringWindowToFront()
        Main.showMessageDialog(this, "Unable to determine what deck you have in slot #" + HearthstoneAnalyser.getDeckSlot +
          "\n\nPlease set your decks in the \"Decks\" tab.")
      } else {
        _notify("Deck Detected", deck.get.name)
        Log.info("Deck Detected: " + deck.get.name + " Detected")
      }

    case MODE =>
      _playingInMatch = false
      matchPanel.setCurrentMatchEnabled(false)
      if (Config.showModeNotification) {
        debug(HearthstoneAnalyser.getMode + " level " + HearthstoneAnalyser.getRankLevel)
        if ("Ranked" == HearthstoneAnalyser.getMode) {
          _notify(HearthstoneAnalyser.getMode + " Mode Detected", "Rank Level " + HearthstoneAnalyser.getRankLevel)
        } else {
          _notify(HearthstoneAnalyser.getMode + " Mode Detected")
        }
      }
      if ("Ranked" == HearthstoneAnalyser.getMode) {
        Log.info(HearthstoneAnalyser.getMode + " Mode Detected - Level " + HearthstoneAnalyser.getRankLevel)
      } else {
        Log.info(HearthstoneAnalyser.getMode + " Mode Detected")
      }

    case NEW_ARENA =>
      if (HearthstoneAnalyser.isNewArena) _notify("New Arena Run Detected")
      Log.info("New Arena Run Detected")

    case OPPONENT_CLASS =>
      _notify("Playing vs " + HearthstoneAnalyser.getOpponentClass)
      Log.info("Playing vs " + HearthstoneAnalyser.getOpponentClass)
      matchPanel.updateCurrentMatchUi()

    case OPPONENT_NAME =>
      _notify("Opponent: " + HearthstoneAnalyser.getOpponentName)
      Log.info("Opponent: " + HearthstoneAnalyser.getOpponentName)
      matchPanel.updateCurrentMatchUi()

    case RESULT =>
      _playingInMatch = false
      matchPanel.setCurrentMatchEnabled(false)
      _notify(HearthstoneAnalyser.getResult + " Detected")
      Log.info(HearthstoneAnalyser.getResult + " Detected")
      checkMatchResult(HearthstoneAnalyser.hsMatch)
      matchPanel.updateCurrentMatchUi()

    case SCREEN =>
      val inGameModeScreen = Seq(Screen.ARENA_END, ARENA_LOBBY, PLAY_LOBBY) contains HearthstoneAnalyser.screen
      if (inGameModeScreen) {
        if (_playingInMatch && HearthstoneAnalyser.getResult == null) {
          _playingInMatch = false
          _notify("Detection Error", "Match result was not detected.")
          Log.info("Detection Error: Match result was not detected.")
          checkMatchResult(HearthstoneAnalyser.hsMatch)
        }
        _playingInMatch = false
      }
      if (HearthstoneAnalyser.screen == FINDING_OPPONENT) {
        setupLogMonitoring()
        matchPanel.resetMatchClassSelectors()
        if (Config.showDeckOverlay && "Arena" != HearthstoneAnalyser.getMode) {
          val selectedDeck = DeckUtils.getDeckFromSlot(HearthstoneAnalyser.getDeckSlot)
          if (selectedDeck.isDefined && selectedDeck.get.isValid) {
            ClickableDeckBox.showBox(selectedDeck.get, hearthstoneLogMonitor.cardEvents)
          } else {
            val message =
              if (selectedDeck.isEmpty)
                "Invalid or empty deck, edit it on HearthStats.net to display deck overlay (you will need to restart HearthStats Companion)"
              else
                s"Invalid or empty deck, <a href='http://hearthstats.net/decks/${selectedDeck.get.slug}/edit'>edit it on HearthStats.net</a> to display deck overlay (you will need to restart HearthStats Companion)"
            _notify(message)
            Log.info(message)
          }
        }
      }
      if (HearthstoneAnalyser.screen.group == ScreenGroup.MATCH_START) {
        matchPanel.setCurrentMatchEnabled(true)
        _playingInMatch = true
      }
      if (HearthstoneAnalyser.screen.group != ScreenGroup.MATCH_END &&
        !DO_NOT_NOTIFY_SCREENS.contains(HearthstoneAnalyser.screen) &&
        Config.showScreenNotification)
        if (HearthstoneAnalyser.screen == PRACTICE_LOBBY)
          _notify(HearthstoneAnalyser.screen.title + " Screen Detected", "Results are not tracked in practice mode")
        else
          _notify(HearthstoneAnalyser.screen.title + " Screen Detected")

      if (HearthstoneAnalyser.screen == PRACTICE_LOBBY)
        Log.info(HearthstoneAnalyser.screen.title + " Screen Detected. Result tracking disabled.")
      else {
        if (HearthstoneAnalyser.screen == MATCH_VS) {
          Log.divider()
        }
        Log.info(HearthstoneAnalyser.screen.title + " Screen Detected")
      }

    case YOUR_CLASS =>
      _notify("Playing as " + HearthstoneAnalyser.getYourClass)
      Log.info("Playing as " + HearthstoneAnalyser.getYourClass)
      matchPanel.updateCurrentMatchUi()

    case YOUR_TURN =>
      if (Config.showYourTurnNotification)
        _notify((if (HearthstoneAnalyser.isYourTurn) "Your" else "Opponent") + " turn detected")
      Log.info((if (HearthstoneAnalyser.isYourTurn) "Your" else "Opponent") + " turn detected")
      matchPanel.updateCurrentMatchUi()

    case ERROR_ANALYSING_IMAGE =>
      _notify("Error analysing opponent name image")
      Log.info("Error analysing opponent name image")

    case _ =>
      _notify("Unhandled event")
      Log.info("Unhandled event")

  }

  def getLogPane: LogPane = _logText

  private def _handleApiEvent(changed: AnyRef) = changed.toString match {
    case "error" =>
      _notify("API Error", API.message)
      Log.error("API Error: " + API.message)
      Main.showMessageDialog(this, "API Error: " + API.message)

    case "result" =>
      Log.info("API Result: " + API.message)
      val lastMatch = HearthstoneAnalyser.hsMatch
      lastMatch.id = API.lastMatchId
      matchPanel.setCurrentMatchEnabled(false)
      matchPanel.updateCurrentMatchUi()
      matchPanel.lastMatch = lastMatch
      if (API.message.matches(".*(Edit match|Arena match successfully created).*")) {
        HearthstoneAnalyser.hsMatch = new HearthstoneMatch
        matchPanel.resetMatchClassSelectors()
        Log.divider()
      }

  }

  private def _handleProgramHelperEvent(changed: AnyRef) {
    Log.info(changed.toString)
    if (changed.toString.matches(".*minimized.*"))
      _notify("Hearthstone Minimized", "Warning! No detection possible while minimized.")
    if (changed.toString.matches(".*fullscreen.*"))
      showMessageDialog(this, "Hearthstats Companion Warning! \n\nNo detection possible while Hearthstone is in fullscreen mode.\n\nPlease set Hearthstone to WINDOWED mode and close and RESTART Hearthstone.\n\nSorry for the inconvenience.")
    if (changed.toString.matches(".*restored.*"))
      _notify("Hearthstone Restored", "Resuming detection ...")
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

  lazy val restoreButton = {
    val button = new MenuItem("Restore")
    button.setFont(new Font("Arial", Font.BOLD, 14))
    button.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        setVisible(true)
        setExtendedState(NORMAL)
      }
    })
    button
  }

  lazy val exitButton = {
    val exitListener = new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        System.exit(0)
      }
    }
    val button = new MenuItem("Exit")
    button.addActionListener(exitListener)
    button.setFont(new Font("Arial", Font.PLAIN, 14))
    button
  }

  private def _enableMinimizeToTray() {
    if (SystemTray.isSupported) {
      val tray = SystemTray.getSystemTray
      val popup = new PopupMenu()
      popup.add(restoreButton)
      popup.add(exitButton)
      val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
      val trayIcon = new TrayIcon(icon, "HearthStats Companion", popup)
      trayIcon.setImageAutoSize(true)
      trayIcon.addMouseListener(new MouseAdapter {
        override def mousePressed(e: MouseEvent) {
          if (e.getClickCount >= 2) {
            setVisible(true)
            setExtendedState(NORMAL)
          }
        }
      })
      addWindowStateListener(new WindowStateListener {
        def windowStateChanged(e: WindowEvent) {
          if (Config.minimizeToTray) {
            e.getNewState match {
              case ICONIFIED =>
                try {
                  tray.add(trayIcon)
                  setVisible(false)
                } catch {
                  case ex: AWTException => debug(ex.getMessage, ex)
                }
              case MAXIMIZED_BOTH | NORMAL =>
                tray.remove(trayIcon)
                setVisible(true)
                debug("Tray icon removed")
            }
          }
        }
      })
    } else debug("system tray not supported")
  }

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

