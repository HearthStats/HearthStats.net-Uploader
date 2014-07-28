package net.hearthstats.ui

import java.awt.Frame._
import java.awt.{ AWTException, Desktop, Dimension, Font, MenuItem, PopupMenu, SystemTray, TrayIcon }
import java.awt.event.{ ActionEvent, ActionListener, MouseAdapter, MouseEvent, WindowAdapter, WindowEvent, WindowStateListener }
import java.io.IOException
import java.net.URI
import javax.swing.JOptionPane._
import javax.swing.ScrollPaneConstants._
import javax.swing.{ ImageIcon, JFrame, JPanel, JScrollPane, JTabbedPane, _ }
import grizzled.slf4j.Logging
import net.hearthstats.config.{ Application, Environment }
import org.apache.commons.lang3.StringUtils
import scala.swing.Swing
import net.hearthstats.config.UserConfig
import net.hearthstats.ui.log.LogPane
import net.hearthstats.util.Translation
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.ui.log.Log
import net.hearthstats.hstatsapi.API
import net.hearthstats.hstatsapi.DeckUtils
import net.hearthstats.game.GameState
import net.hearthstats.ProgramHelper
import net.hearthstats.hstatsapi.HearthStatsUrls._
import net.hearthstats.util.Browse

/**
 * Main Frame for HearthStats Companion.
 */
class CompanionFrame(environment: Environment,
  config: UserConfig,
  uiLog: Log,
  programHelper: ProgramHelper,
  gameState: GameState,
  api: API,
  deckUtils: DeckUtils,
  translation: Translation) extends JFrame with Logging {

  import config._
  import translation.t
  val logText = new LogPane
  val logScroll = new JScrollPane(logText, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val tabbedPane = new JTabbedPane
  val optionsPanel: OptionsPanel = wire[OptionsPanel]
  val aboutPanel: AboutPanel = wire[AboutPanel]
  val matchPanel: MatchPanel = wire[MatchPanel]
  val decksTab: DecksTab = wire[DecksTab]
  var notificationQueue: NotificationQueue = environment.newNotificationQueue(notificationType)

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent) {
      handleClose()
    }
  })
  createAndShowGui()

  def handleClose() {
    try {
      val p = getLocationOnScreen
      windowX.set(p.x)
      windowY.set(p.y)
      val rect = getSize
      windowWidth.set(rect.getWidth.toInt)
      windowHeight.set(rect.getHeight.toInt)
    } catch {
      case t: Exception => uiLog.warn("Error occurred trying to save your settings, your window position may not be saved", t)
    }
    System.exit(0)
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
    tabbedPane.add(decksTab, t("tab.decks"))
    tabbedPane.add(optionsPanel, t("tab.options"))
    tabbedPane.add(aboutPanel, t("tab.about"))
    matchPanel.updateCurrentMatchUi()
    enableMinimizeToTray()
    setMinimumSize(new Dimension(500, 600))
    setVisible(true)
    if (enableStartMin) setState(ICONIFIED)
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
    var title = "HearthStats.net Companion"
    //    if (monitor._hearthstoneDetected) {
    //      if (HearthstoneAnalyser.screen != null) {
    //        title += " - " + HearthstoneAnalyser.screen.title
    //        if (HearthstoneAnalyser.screen == PLAY_LOBBY && HearthstoneAnalyser.getMode != null) {
    //          title += " " + HearthstoneAnalyser.getMode
    //        }
    //        if (HearthstoneAnalyser.screen == FINDING_OPPONENT) {
    //          if (HearthstoneAnalyser.getMode != null) {
    //            title += " for " + HearthstoneAnalyser.getMode + " Game"
    //          }
    //        }
    //        if ("Match Start" == HearthstoneAnalyser.screen.title ||
    //          "Playing" == HearthstoneAnalyser.screen.title) {
    //          title += " " +
    //            (if (HearthstoneAnalyser.getMode == null) "[undetected]" else HearthstoneAnalyser.getMode)
    //          title += " " + (if (HearthstoneAnalyser.getCoin) "" else "No ") +
    //            "Coin"
    //          title += " " +
    //            (if (HearthstoneAnalyser.getYourClass == null) "[undetected]" else HearthstoneAnalyser.getYourClass)
    //          title += " VS. " +
    //            (if (HearthstoneAnalyser.getOpponentClass == null) "[undetected]" else HearthstoneAnalyser.getOpponentClass)
    //        }
    //      }
    //    } else {
    //      title += " - Waiting for Hearthstone "
    //    }
    setTitle(title)
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
          if (enableMinToTray) {
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
    val userKeySet = !userKey.equalsIgnoreCase("your_userkey_here")
    if (userKeySet) {
      true
    } else {
      uiLog.warn(t("error.userkey_not_entered"))
      bringWindowToFront()
      showMessageDialog(this, t("error.title") + ":\n\n" + t("you_need_to_enter_userkey") +
        "\n\n" +
        t("get_it_at_hsnet_profiles"))
      Browse(PROFILES_URL)
      val userkey = showInputDialog(this, t("enter_your_userkey"))
      if (StringUtils.isEmpty(userkey)) {
        false
      } else {
        userKey.set(userkey)
        try {
          optionsPanel.setUserKey(userkey)
          uiLog.info(t("UserkeyStored"))
        } catch {
          case e: Exception => uiLog.warn("Error occurred trying to write settings file, your settings may not be saved", e)
        }
        true
      }
    }
  }

  def checkForUpdates() {
    if (enableUpdateCheck) {
      uiLog.info(t("checking_for_updates..."))
      //      try {
      //        var latestRelease = Updater.getLatestRelease(environment)
      //        if (latestRelease != null) {
      //          uiLog.info(t("latest_v_available") + " " + latestRelease.getVersion)
      //          if (!latestRelease.getVersion.equalsIgnoreCase("v" + Application.version)) {
      //            bringWindowToFront()
      //            val dialogButton = YES_NO_OPTION
      //            var dialogResult = showConfirmDialog(
      //              this,
      //              s"""A new version of HearthStats Companion is available: ${latestRelease.getVersion}
      //                  |${latestRelease.getBody}
      //                  |            
      //                  |
      //                  | ${t("would_u_like_to_install_update")}""".stripMargin,
      //              "HearthStats " + t("uploader_updates_avail"),
      //              dialogButton)
      //            if (dialogResult == YES_OPTION) {
      //              Updater.run(environment, latestRelease)
      //            } else {
      //              dialogResult = showConfirmDialog(
      //                null,
      //                t("would_you_like_to_disable_updates"),
      //                t("disable_update_checking"),
      //                dialogButton)
      //              if (dialogResult == YES_OPTION) {
      //                val options = Array(t("button.ok"))
      //                val panel = new JPanel()
      //                val lbl = new JLabel(t("reenable_updates_any_time"))
      //                panel.add(lbl)
      //                showOptionDialog(this, panel, t("updates_disabled_msg"), NO_OPTION,
      //                  QUESTION_MESSAGE, null, options.toArray, options(0))
      //                enableUpdateCheck.set(false)
      //              }
      //            }
      //          }
      //        } else uiLog.warn("Unable to determine latest available version")
      //      } catch {
      //        case e: Exception => {
      //          e.printStackTrace(System.err)
      //          notify("Update Checking Error", "Unable to determine the latest available version")
      //        }
      //      }
    }
  }
}