package net.hearthstats.ui

import net.hearthstats.Constants.PROFILES_URL
import net.hearthstats.analysis.AnalyserEvent._
import javax.swing._
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JOptionPane._
import net.hearthstats.state.Screen._
import java.awt.Frame._
import javax.swing.ScrollPaneConstants._
import scala.swing.Swing
import javax.swing.JTabbedPane
import java.awt.AWTException
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener
import javax.swing.JFrame
import javax.swing.JScrollPane
import net.hearthstats.util.Translations._
import java.awt.event.WindowAdapter
import net.hearthstats.log.LogPane
import javax.swing.JTabbedPane
import java.awt.event.WindowEvent
import net.hearthstats.notification.NotificationQueue
import net.hearthstats.analysis.HearthstoneAnalyser
import java.awt.SystemTray
import net.hearthstats.log.Log
import net.hearthstats.config.OS
import java.awt.AWTException
import javax.swing.ImageIcon
import java.awt.event.WindowStateListener
import java.awt.TrayIcon
import java.awt.event.ActionListener
import scala.swing.Swing
import java.awt.event.ActionEvent
import java.awt.MenuItem
import net.hearthstats.OldConfig
import java.awt.Dimension
import java.awt.PopupMenu
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Font
import org.slf4j.LoggerFactory
import net.hearthstats.Monitor
import org.slf4j.Logger
import net.hearthstats.config.Environment
import grizzled.slf4j.Logging
import net.hearthstats.Updater
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.net.URI

/**
 * Main Frame for HearthStats Companion.
 */
class CompanionFrame(val environment: Environment, val monitor: Monitor) extends JFrame with Logging {
  import environment.config._

  val logText = new LogPane
  val logScroll = new JScrollPane(logText, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val tabbedPane = new JTabbedPane
  val optionsPanel = new OptionsPanel(this)
  val matchPanel = new MatchPanel
  var notificationQueue: NotificationQueue = environment.newNotificationQueue(notificationType)

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent) {
      handleClose()
    }
  })
  createAndShowGui()
  showWelcomeLog()

  def handleClose() {
    try {
      val p = getLocationOnScreen
      windowX.set(p.x)
      windowY.set(p.y)
      val rect = getSize
      windowWidth.set(rect.getWidth.toInt)
      windowHeight.set(rect.getHeight.toInt)
    } catch {
      case t: Exception => Log.warn("Error occurred trying to save your settings, your window position may not be saved", t)
    }
    System.exit(0)
  }

  def showWelcomeLog() {
    debug("Showing welcome log messages")
    Log.welcome("HearthStats " + t("Companion") + " v" + OldConfig.getVersionWithOs)
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

  def createAndShowGui() {
    debug("Creating GUI")
    val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
    setIconImage(icon)
    setLocation(windowX, windowY)
    setSize(windowWidth, windowHeight)
    add(tabbedPane)
    tabbedPane.add(logScroll, t("tab.log"))
    tabbedPane.add(matchPanel, t("tab.current_match"))
    tabbedPane.add(new DecksTab(this), t("tab.decks"))
    tabbedPane.add(optionsPanel, t("tab.options"))
    tabbedPane.add(new AboutPanel(), t("tab.about"))
    matchPanel.updateCurrentMatchUi()
    enableMinimizeToTray()
    setMinimumSize(new Dimension(500, 600))
    setVisible(true)
    if (OldConfig.startMinimized) setState(ICONIFIED)
    updateTitle()
  }

  def setNotificationQueue(_notificationQueue: NotificationQueue) {
    this.notificationQueue = notificationQueue
  }

  def notify(header: String) {
    notify(header, "")
  }

  def notify(header: String, message: String) {
    if (notifyOverall) notificationQueue.add(header, message, false)
  }

  def updateTitle() {
    var title = "HearthStats.net Uploader"
    if (monitor._hearthstoneDetected) {
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

  def getLogPane: LogPane = logText

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

  def enableMinimizeToTray() {
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
          if (OldConfig.minimizeToTray) {
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

  def checkForUserKey(): Boolean = {
    val userKeySet = OldConfig.getUserKey != "your_userkey_here"
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
        OldConfig.setUserKey(userkey)
        try {
          optionsPanel.setUserKey(userkey)
          OldConfig.save()
          Log.info(t("UserkeyStored"))
        } catch {
          case e: Exception => Log.warn("Error occurred trying to write settings file, your settings may not be saved", e)
        }
        true
      }
    }
  }

  def checkForUpdates() {
    if (OldConfig.checkForUpdates()) {
      Log.info(t("checking_for_updates..."))
      try {
        var latestRelease = Updater.getLatestRelease
        if (latestRelease != null) {
          Log.info(t("latest_v_available") + " " + latestRelease.getVersion)
          if (!latestRelease.getVersion.equalsIgnoreCase("v" + OldConfig.getVersion)) {
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
                OldConfig.setCheckForUpdates(false)
              }
            }
          }
        } else Log.warn("Unable to determine latest available version")
      } catch {
        case e: Exception => {
          e.printStackTrace(System.err)
          notify("Update Checking Error", "Unable to determine the latest available version")
        }
      }
    }
  }
}