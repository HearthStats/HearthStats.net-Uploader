package net.hearthstats.ui

import java.awt.Dimension
import java.awt.Frame._
import javax.swing.JOptionPane._
import javax.swing.ScrollPaneConstants._
import javax.swing.{ ImageIcon, JScrollPane, JTabbedPane }

import com.softwaremill.macwire.MacwireMacros.wire
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.companion.CompanionState
import net.hearthstats.config.{ Environment, UserConfig }
import net.hearthstats.core.{ HearthstoneMatch, HeroClass }
import net.hearthstats.game.MatchState
import net.hearthstats.hstatsapi.HearthStatsUrls._
import net.hearthstats.hstatsapi.{ API, CardUtils, DeckUtils }
import net.hearthstats.ui.log.{ Log, LogPane }
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.util.{ Browse, Translation }
import org.apache.commons.lang3.StringUtils

/**
 * Main Frame for HearthStats Companion.
 */
class CompanionFrame(val environment: Environment,
  val config: UserConfig,
  val uiLog: Log,
  val notificationQueue: NotificationQueue,
  programHelper: ProgramHelper,
  companionState: CompanionState,
  matchState: MatchState,
  api: API,
  cardUtils: CardUtils,
  deckUtils: DeckUtils,
  exportDeckBox: ExportDeckBox,
  translation: Translation) extends GeneralUI with HearthstatsPresenter with Logging {

  import config._
  import translation.t
  val logText = new LogPane
  uiLog.logPane = logText
  val logScroll = new JScrollPane(logText, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val tabbedPane = new JTabbedPane
  val optionsPanel: OptionsPanel = wire[OptionsPanel]
  val aboutPanel: AboutPanel = wire[AboutPanel]
  val matchPanel: MatchPanel = wire[MatchPanel]
  val decksTab: DecksTab = wire[DecksTab]
  val landingPanel: LandingPanel = wire[LandingPanel]
  
  def createAndShowGui() {
    debug("Creating GUI")
    //landingFrame.createAndShowGui()
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
    tabbedPane.add(landingPanel,t("tab.landing"))

    matchPanel.updateCurrentMatchUi()
    enableMinimizeToTray()
    setMinimumSize(new Dimension(500, 600))
    setVisible(true)
    if (enableStartMin) setState(ICONIFIED)
    updateTitle()
  }

  def setOpponentClass(heroClass: HeroClass) = matchPanel.setOpponentClass(heroClass)
  def setYourClass(heroClass: HeroClass) = matchPanel.setYourClass(heroClass)
  def setOpponentName(n: String) = matchPanel.setOpponentName(n)
  def setCoin(coin: Boolean) = matchPanel.setCoin(coin)
  def matchSubmitted(m: HearthstoneMatch, description: String): Unit = matchPanel.matchSubmitted(m, description)

  def updateTitle() {
    var title = "HearthStats Companion"
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