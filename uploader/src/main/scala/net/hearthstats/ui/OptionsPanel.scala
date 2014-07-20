package net.hearthstats.ui

import net.hearthstats.util.Translations.t
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing._
import net.hearthstats.Config
import net.hearthstats.config.GameLanguage
import net.hearthstats.config.MatchPopup
import net.hearthstats.config.MonitoringMethod
import net.hearthstats.log.Log
import net.hearthstats.util.TranslationCard
import net.miginfocom.swing.MigLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import net.hearthstats.config.Environment

//TODO : this is a rough conversion from java to scala, needs tuning to be clean code
class OptionsPanel(var mainFrame: CompanionFrame) extends JPanel {

  private var _userKeyField: JTextField = new JTextField()

  private var monitoringMethodField = new JComboBox(Array(t("options.label.monitoring.screen"), t("options.label.monitoring.log")))

  private var gameLanguageField = new JComboBox(Array(t("options.label.game.language.eu"), t("options.label.game.language.fr")))

  private var _checkUpdatesField: JCheckBox = new JCheckBox(t("options.check_updates"))

  private var _notificationsEnabledField: JCheckBox = new JCheckBox("Show notifications")

  private var _notificationsFormat: JComboBox[String] = _

  private var _showHsFoundField: JCheckBox = new JCheckBox(t("options.notification.hs_found"))

  private var _showHsClosedField: JCheckBox = new JCheckBox(t("options.notification.hs_closed"))

  private var _showScreenNotificationField: JCheckBox = new JCheckBox(t("options.notification.screen"))

  private var _showModeNotificationField: JCheckBox = new JCheckBox(t("options.notification.mode"))

  private var _showDeckNotificationField: JCheckBox = new JCheckBox(t("options.notification.deck"))

  private var showMatchPopupField = new JComboBox(Array(t("options.label.matchpopup.always"), t("options.label.matchpopup.incomplete"), t("options.label.matchpopup.never")))

  private var _analyticsField: JCheckBox = new JCheckBox(t("options.submit_stats"))

  private var _minToTrayField: JCheckBox = new JCheckBox(t("options.notification.min_to_tray"))

  private var _startMinimizedField: JCheckBox = new JCheckBox(t("options.notification.start_min"))

  private var _showYourTurnNotificationField: JCheckBox = new JCheckBox(t("options.notification.turn"))

  private var _showDeckOverlay: JCheckBox = new JCheckBox(t("options.ui.deckOverlay"))

  private var debugLog: Logger = LoggerFactory.getLogger(getClass)

  setLayout(new MigLayout)

  add(new JLabel(" "), "wrap")

  add(new JLabel(t("options.label.userkey") + " "), "skip,right")

  _userKeyField.setText(Config.getUserKey)

  add(_userKeyField, "wrap")

  add(new JLabel(t("options.label.monitoring")), "skip,right")

  monitoringMethodField.setSelectedIndex(Config.monitoringMethod().ordinal())

  add(monitoringMethodField, "")

  val monitoringHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Monitoring",
    "Help on monitoring options")

  add(monitoringHelpIcon, "wrap")

  add(new JLabel(t("options.label.game.language")), "skip,right")

  gameLanguageField.setSelectedIndex(Config.gameLanguage().ordinal())

  gameLanguageField.addActionListener(new ActionListener() {

    override def actionPerformed(e: ActionEvent) {
      _updateGameLanguage()
    }
  })

  _updateGameLanguage()

  add(gameLanguageField, "")

  val gameLanguageHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Game-Language",
    "Help on game language options")

  add(gameLanguageHelpIcon, "wrap")

  add(new JLabel(t("options.label.updates") + " "), "skip,right")

  _checkUpdatesField.setSelected(Config.checkForUpdates())

  add(_checkUpdatesField, "wrap")

  add(new JLabel(t("options.label.notifications") + " "), "skip,right")

  _notificationsEnabledField.setSelected(Config.showNotifications())

  _notificationsEnabledField.addActionListener(new ActionListener() {

    override def actionPerformed(e: ActionEvent) {
      _updateNotificationCheckboxes()
    }
  })

  add(_notificationsEnabledField, "wrap")

  if (mainFrame.monitor.environment.osxNotificationsSupported) {
    add(new JLabel(""), "skip,right")
    val notificationsFormatLabel = new JLabel(t("options.label.notifyformat.label"))
    add(notificationsFormatLabel, "split 2, gapleft 27")
    _notificationsFormat = new JComboBox(Array(t("options.label.notifyformat.osx"), t("options.label.notifyformat.hearthstats")))
    _notificationsFormat.setSelectedIndex(if (Config.useOsxNotifications()) 0 else 1)
    add(_notificationsFormat, "")
    val osxNotificationsHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-OS-X-Notifications",
      "Help on notification style options")
    add(osxNotificationsHelpIcon, "wrap")
  }

  add(new JLabel(""), "skip,right")

  _showHsFoundField.setSelected(Config.showHsFoundNotification())

  add(_showHsFoundField, "wrap")

  add(new JLabel(""), "skip,right")

  _showHsClosedField.setSelected(Config.showHsClosedNotification())

  add(_showHsClosedField, "wrap")

  add(new JLabel(""), "skip,right")

  _showScreenNotificationField.setSelected(Config.showScreenNotification())

  add(_showScreenNotificationField, "wrap")

  add(new JLabel(""), "skip,right")

  _showModeNotificationField.setSelected(Config.showModeNotification())

  add(_showModeNotificationField, "wrap")

  add(new JLabel(""), "skip,right")

  _showDeckNotificationField.setSelected(Config.showDeckNotification())

  add(_showDeckNotificationField, "wrap")

  add(new JLabel(""), "skip,right")

  _showYourTurnNotificationField.setSelected(Config.showYourTurnNotification())

  add(_showYourTurnNotificationField, "wrap")

  _updateNotificationCheckboxes()

  add(new JLabel(""), "skip,right")

  _showDeckOverlay.setSelected(Config.showDeckOverlay())

  add(_showDeckOverlay, "")

  val deckOverlayHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Deck-Overlay",
    "Help on the show deck overlay option")

  add(deckOverlayHelpIcon, "wrap")

  add(new JLabel(t("options.label.matchpopup")), "skip,right")

  showMatchPopupField.setSelectedIndex(Config.showMatchPopup().ordinal())

  add(showMatchPopupField, "")

  val matchPopupHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Match-Popup",
    "Help on the match popup options")

  add(matchPopupHelpIcon, "wrap")

  add(new JLabel("Interface: "), "skip,right")

  _minToTrayField.setSelected(Config.checkForUpdates())

  add(_minToTrayField, "wrap")

  add(new JLabel(""), "skip,right")

  _startMinimizedField.setSelected(Config.startMinimized())

  add(_startMinimizedField, "wrap")

  add(new JLabel("Analytics: "), "skip,right")

  _analyticsField.addActionListener(new ActionListener() {

    override def actionPerformed(e: ActionEvent) {
      if (!_analyticsField.isSelected) {
        val dialogResult = JOptionPane.showConfirmDialog(OptionsPanel.this, "A lot of work has gone into this uploader.\n" +
          "It is provided for free, and all we ask in return\n" +
          "is that you let us track basic, anonymous statistics\n" +
          "about how frequently it is being used." +
          "\n\nAre you sure you want to disable analytics?", "Please reconsider ...", JOptionPane.YES_NO_OPTION)
        if (dialogResult == JOptionPane.NO_OPTION) {
          _analyticsField.setSelected(true)
        }
      }
    }
  })

  _analyticsField.setSelected(Config.analyticsEnabled())

  add(_analyticsField, "wrap")

  add(new JLabel(""), "skip,right")

  val saveOptionsButton = new JButton(t("button.save_options"))

  saveOptionsButton.addActionListener(new ActionListener() {

    override def actionPerformed(e: ActionEvent) {
      _saveOptions(mainFrame.monitor.environment)
    }
  })

  add(saveOptionsButton, "wrap")

  private def _updateNotificationCheckboxes() {
    val isEnabled = _notificationsEnabledField.isSelected
    if (_notificationsFormat != null) {
      _notificationsFormat.setEnabled(isEnabled)
    }
    _showHsFoundField.setEnabled(isEnabled)
    _showHsClosedField.setEnabled(isEnabled)
    _showScreenNotificationField.setEnabled(isEnabled)
    _showModeNotificationField.setEnabled(isEnabled)
    _showDeckNotificationField.setEnabled(isEnabled)
  }

  private def _saveOptions(environment: Environment) {
    debugLog.debug("Saving options...")
    val monitoringMethod = MonitoringMethod.values()(monitoringMethodField.getSelectedIndex)
    val gameLanguage = GameLanguage.values()(gameLanguageField.getSelectedIndex)
    Config.setUserKey(_userKeyField.getText)
    Config.setMonitoringMethod(monitoringMethod)
    Config.setGameLanguage(gameLanguage)
    Config.setCheckForUpdates(_checkUpdatesField.isSelected)
    Config.setShowNotifications(_notificationsEnabledField.isSelected)
    Config.setShowHsFoundNotification(_showHsFoundField.isSelected)
    Config.setShowHsClosedNotification(_showHsClosedField.isSelected)
    Config.setShowScreenNotification(_showScreenNotificationField.isSelected)
    Config.setShowModeNotification(_showModeNotificationField.isSelected)
    Config.setShowDeckNotification(_showDeckNotificationField.isSelected)
    Config.setShowYourTurnNotification(_showYourTurnNotificationField.isSelected)
    Config.setShowDeckOverlay(_showDeckOverlay.isSelected)
    Config.setShowMatchPopup(MatchPopup.values()(showMatchPopupField.getSelectedIndex))
    Config.setAnalyticsEnabled(_analyticsField.isSelected)
    Config.setMinToTray(_minToTrayField.isSelected)
    Config.setStartMinimized(_startMinimizedField.isSelected)
    if (_notificationsFormat != null) {
      Config.setUseOsxNotifications(_notificationsFormat.getSelectedIndex == 0)
      mainFrame.setNotificationQueue(environment.newNotificationQueue(Config.notificationType()))
    }
    mainFrame.monitor.setupLogMonitoring()
    try {
      Config.save()
      debugLog.debug("...save complete")
      JOptionPane.showMessageDialog(this, "Options Saved")
    } catch {
      case e: Throwable => {
        Log.warn("Error occurred trying to write settings file, your settings may not be saved", e)
        JOptionPane.showMessageDialog(null, "Error occurred trying to write settings file, your settings may not be saved")
      }
    }
  }

  private def _updateGameLanguage() {
    TranslationCard.changeTranslation()
  }

  def setUserKey(userkey: String) {
    _userKeyField.setText(userkey)
  }
}
