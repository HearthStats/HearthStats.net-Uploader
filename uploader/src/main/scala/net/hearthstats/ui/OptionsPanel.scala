package net.hearthstats.ui

import net.hearthstats.util.Translations.t
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing._
import net.hearthstats.OldConfig
import net.hearthstats.config._
import net.hearthstats.log.Log
import net.hearthstats.util.TranslationCard
import net.miginfocom.swing.MigLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

//TODO : this is a rough conversion from java to scala, needs tuning to be clean code
// Has been partially converted to use the new config, only the notification checkboxes have been updated
class OptionsPanel(val mainFrame: CompanionFrame) extends JPanel {

  import mainFrame.environment.config._

  private var _userKeyField: JTextField = new JTextField()

  private var gameLanguageField = new JComboBox(Array(t("options.label.game.language.eu"), t("options.label.game.language.fr")))

  private var _checkUpdatesField: JCheckBox = new JCheckBox(t("options.check_updates"))

  private var notificationsFormat: JComboBox[String] = _

  private var showMatchPopupField = new JComboBox(Array(t("options.label.matchpopup.always"), t("options.label.matchpopup.incomplete"), t("options.label.matchpopup.never")))

  private var _analyticsField: JCheckBox = new JCheckBox(t("options.submit_stats"))

  private var _minToTrayField: JCheckBox = new JCheckBox(t("options.notification.min_to_tray"))

  private var _startMinimizedField: JCheckBox = new JCheckBox(t("options.notification.start_min"))

  private var _showDeckOverlay: JCheckBox = new JCheckBox(t("options.ui.deckOverlay"))

  private var debugLog: Logger = LoggerFactory.getLogger(getClass)

  setLayout(new MigLayout)

  add(new JLabel(" "), "wrap")

  add(new JLabel(t("options.label.userkey") + " "), "skip,right")

  _userKeyField.setText(OldConfig.getUserKey)

  add(_userKeyField, "wrap")

  add(new JLabel(t("options.label.monitoring")), "skip,right")

  // Monitoring Method
  addComboBox[MonitoringMethod](
    Array(t("options.label.monitoring.screen"), t("options.label.monitoring.log")),
    monitoringMethod, monitoringMethod.set, "",
    (value: MonitoringMethod) => {
      mainFrame.monitor.setupLogMonitoring()
    })

  val monitoringHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Monitoring",
    "Help on monitoring options")

  add(monitoringHelpIcon, "wrap")

  add(new JLabel(t("options.label.game.language")), "skip,right")

  gameLanguageField.setSelectedIndex(OldConfig.gameLanguage().ordinal())

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

  _checkUpdatesField.setSelected(OldConfig.checkForUpdates())

  add(_checkUpdatesField, "wrap")

  // Show Notifications
  addLabel(t("options.label.notifications"))
  private val notificationsEnabledField =
    addCheckbox("Show notifications", notifyOverall, notifyOverall.set, "wrap", updateNotificationCheckboxes _)

  // Notifications Format (only for OS X)
  if (mainFrame.monitor.environment.osxNotificationsSupported) {
    add(new JLabel(""), "skip,right")
    val notificationsFormatLabel = new JLabel(t("options.label.notifyformat.label"))
    add(notificationsFormatLabel, "split 2, gapleft 27")
    notificationsFormat = addComboBox[NotificationType](
      Array(t("options.label.notifyformat.hearthstats"), t("options.label.notifyformat.osx")),
      notificationType, notificationType.set, "",
      (value: NotificationType) => {
        mainFrame.setNotificationQueue(mainFrame.environment.newNotificationQueue(value))
      })
    val osxNotificationsHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-OS-X-Notifications",
      "Help on notification style options")
    add(osxNotificationsHelpIcon, "wrap")
  }

  // Hearthstone Found Notification
  addLabel()
  private val showHsFoundField =
    addCheckbox(t("options.notification.hs_found"), notifyHsFound, notifyHsFound.set)

  // Hearthstone Closed Notification
  addLabel()
  private val showHsClosedField =
    addCheckbox(t("options.notification.hs_closed"), notifyHsClosed, notifyHsClosed.set)

  // Game Screen Changed Notification
  addLabel()
  private val showScreenNotificationField =
    addCheckbox(t("options.notification.screen"), notifyScreen, notifyScreen.set)

  // Game Mode Changed Notification
  addLabel()
  private val showModeNotificationField =
    addCheckbox(t("options.notification.mode"), notifyMode, notifyMode.set)

  // Deck Changed Notification
  addLabel()
  private val showDeckNotificationField =
    addCheckbox(t("options.notification.deck"), notifyDeck, notifyDeck.set)

  // Your Turn Notification
  addLabel()
  private val showYourTurnNotificationField =
    addCheckbox(t("options.notification.turn"), notifyTurn, notifyTurn.set)

  add(new JLabel(""), "skip,right")

  updateNotificationCheckboxes(notificationsEnabledField)

  _showDeckOverlay.setSelected(OldConfig.showDeckOverlay())

  add(_showDeckOverlay, "")

  val deckOverlayHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Deck-Overlay",
    "Help on the show deck overlay option")

  add(deckOverlayHelpIcon, "wrap")

  add(new JLabel(t("options.label.matchpopup")), "skip,right")

  showMatchPopupField.setSelectedIndex(OldConfig.showMatchPopup().ordinal())

  add(showMatchPopupField, "")

  val matchPopupHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Match-Popup",
    "Help on the match popup options")

  add(matchPopupHelpIcon, "wrap")

  add(new JLabel("Interface: "), "skip,right")

  _minToTrayField.setSelected(OldConfig.checkForUpdates())

  add(_minToTrayField, "wrap")

  add(new JLabel(""), "skip,right")

  _startMinimizedField.setSelected(OldConfig.startMinimized())

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

  _analyticsField.setSelected(OldConfig.analyticsEnabled())

  add(_analyticsField, "wrap")

  add(new JLabel(""), "skip,right")

  val saveOptionsButton = new JButton(t("button.save_options"))

  saveOptionsButton.addActionListener(new ActionListener() {

    override def actionPerformed(e: ActionEvent) {
      _saveOptions(mainFrame.monitor.environment)
    }
  })

  add(saveOptionsButton, "wrap")

  private def updateNotificationCheckboxes(notificationsCheckBox: JCheckBox) {
    val isEnabled = notificationsCheckBox.isSelected
    if (notificationsFormat != null) {
      notificationsFormat.setEnabled(isEnabled)
    }
    showHsFoundField.setEnabled(isEnabled)
    showHsClosedField.setEnabled(isEnabled)
    showScreenNotificationField.setEnabled(isEnabled)
    showModeNotificationField.setEnabled(isEnabled)
    showDeckNotificationField.setEnabled(isEnabled)
  }

  @Deprecated // TODO: this manual save should be removed because UserConfig saves automatically
  private def _saveOptions(environment: Environment) {
    debugLog.debug("Saving options...")
    //    val monitoringMethod = MonitoringMethod.values()(monitoringMethodField.getSelectedIndex)
    val gameLanguage = GameLanguage.values()(gameLanguageField.getSelectedIndex)
    OldConfig.setUserKey(_userKeyField.getText)
    //    OldConfig.setMonitoringMethod(monitoringMethod)
    OldConfig.setGameLanguage(gameLanguage)
    OldConfig.setCheckForUpdates(_checkUpdatesField.isSelected)
    //    OldConfig.setShowNotifications(_notificationsEnabledField.isSelected)
    //    environment.config.notifyOverall = _notificationsEnabledField.isSelected
    //    OldConfig.setShowHsFoundNotification(_showHsFoundField.isSelected)
    //    OldConfig.setShowHsClosedNotification(_showHsClosedField.isSelected)
    //    OldConfig.setShowScreenNotification(_showScreenNotificationField.isSelected)
    //    OldConfig.setShowModeNotification(_showModeNotificationField.isSelected)
    //    OldConfig.setShowDeckNotification(_showDeckNotificationField.isSelected)
    //    OldConfig.setShowYourTurnNotification(_showYourTurnNotificationField.isSelected)
    OldConfig.setShowDeckOverlay(_showDeckOverlay.isSelected)
    OldConfig.setShowMatchPopup(MatchPopup.values()(showMatchPopupField.getSelectedIndex))
    OldConfig.setAnalyticsEnabled(_analyticsField.isSelected)
    OldConfig.setMinToTray(_minToTrayField.isSelected)
    OldConfig.setStartMinimized(_startMinimizedField.isSelected)
    //    if (_notificationsFormat != null) {
    //      OldConfig.setUseOsxNotifications(_notificationsFormat.getSelectedIndex == 0)
    //      mainFrame.setNotificationQueue(environment.newNotificationQueue(config.notificationType))
    //    }
    //    mainFrame.monitor.setupLogMonitoring()
    try {
      OldConfig.save()
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

  def addLabel(label: String = "") {
    add(new JLabel(label), "skip,right")
  }

  def addCheckbox(label: String, getter: => Boolean, setter: Boolean => Unit, constraints: String = "wrap",
    onChange: JCheckBox => Unit = (checkbox: JCheckBox) => {}): JCheckBox = {
    val checkBox = new JCheckBox(label)

    // Set the checkbox to the current value from the config
    checkBox.setSelected(getter)

    // When the checkbox is clicked, update the config value
    checkBox.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        setter(checkBox.isSelected)
        onChange(checkBox)
      }
    })

    add(checkBox, constraints)
    checkBox
  }

  def addComboBox[T <: Enum[T]](choices: Array[String], getter: => T, setter: T => Unit, constraints: String = "wrap",
    onChange: T => Unit = (value: T) => {}): JComboBox[String] = {
    val comboBox = new JComboBox[String](choices)

    // Set the combobox to the current value from the config
    comboBox.setSelectedIndex(getter.ordinal())

    comboBox.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        // This is a lazy way to get an instance of the class, the old value isn't actually used
        val oldValue = getter;
        val newValue = oldValue.getClass.getEnumConstants.apply(comboBox.getSelectedIndex)
        setter(newValue)
        onChange(newValue)
      }
    })

    add(comboBox, constraints)
    comboBox
  }

}
