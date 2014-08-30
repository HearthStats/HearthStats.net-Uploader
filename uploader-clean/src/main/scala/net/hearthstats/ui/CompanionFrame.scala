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
import net.hearthstats.ProgramHelper
import net.hearthstats.hstatsapi.HearthStatsUrls._
import net.hearthstats.util.Browse
import net.hearthstats.companion.CompanionState
import net.hearthstats.game.MatchState

/**
 * Main Frame for HearthStats Companion.
 */
class CompanionFrame(environment: Environment,
  config: UserConfig,
  uiLog: Log,
  programHelper: ProgramHelper,
  companionState: CompanionState,
  matchState: MatchState,
  api: API,
  deckUtils: DeckUtils,
  translation: Translation) extends JFrame with GeneralUI with Logging {

  import config._
  import translation.t
  val logText = new LogPane
  val logScroll = new JScrollPane(logText, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val tabbedPane = new JTabbedPane
  val optionsPanel: OptionsPanel = wire[OptionsPanel]
  val aboutPanel: AboutPanel = wire[AboutPanel]
  val matchPanel: MatchPanel = wire[MatchPanel]
  val decksTab: DecksTab = wire[DecksTab]

  createAndShowGui()

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

}