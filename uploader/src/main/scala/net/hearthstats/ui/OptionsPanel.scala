package net.hearthstats.ui

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{Dimension, Font}
import javax.swing._
import javax.swing.event.{DocumentEvent, DocumentListener}

import net.hearthstats.config._
import net.hearthstats.util.TranslationCard
import net.hearthstats.util.Translations.t
import net.miginfocom.swing.MigLayout

import scala.swing.Swing._

class OptionsPanel(val mainFrame: CompanionFrame) extends JPanel {

  import mainFrame.environment.config._

  private var notificationsFormat: JComboBox[String] = _

  // Create the UI
  setLayout(new MigLayout)

  add(new JLabel(" "), "wrap")

  // User Key
  addLabel(t("options.label.userkey") + " ")
  val userKeyFieldSize = new Dimension(280, 28)
  var userKeyField: JTextField = new JTextField()
  userKeyField.setText(configUserKey)
  userKeyField.setMinimumSize(userKeyFieldSize)
  userKeyField.setPreferredSize(userKeyFieldSize)
  userKeyField.getDocument.addDocumentListener(new DocumentListener {
    def insertUpdate(e: DocumentEvent): Unit = handleChange
    def changedUpdate(e: DocumentEvent): Unit = handleChange
    def removeUpdate(e: DocumentEvent): Unit = handleChange
    def handleChange = onEDT({
      if (configUserKey.get != userKeyField.getText) {
        configUserKey.set(userKeyField.getText)
      }
    })
  })
  add(userKeyField, "wrap")

  // Monitoring Method
  addLabel(t("options.label.monitoring") + " ")
  addComboBox[MonitoringMethod](
    Array(t("options.label.monitoring.screen"), t("options.label.monitoring.log")),
    optionMonitoringMethod, optionMonitoringMethod.set, "",
    (value: MonitoringMethod) => {
      mainFrame.monitor.setupLogMonitoring()
    })

  val monitoringHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Monitoring",
    "Help on monitoring options")
  add(monitoringHelpIcon, "wrap")

  // Game Language
  addLabel(t("options.label.game.language"))
  addComboBox[GameLanguage](Array(t("options.label.game.language.eu"), t("options.label.game.language.fr"), t("options.label.game.language.ru")),
    optionGameLanguage, optionGameLanguage.set, "", TranslationCard.changeTranslation)

  val gameLanguageHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Game-Language",
    "Help on game language options")
  add(gameLanguageHelpIcon, "wrap")

  // Check for Updates
  addLabel(t("options.label.updates"))
  addCheckBox(t("options.check_updates"), enableUpdateCheck, enableUpdateCheck.set)

  // Show Notifications
  addLabel(t("options.label.notifications"))
  private val notificationsEnabledField =
    addCheckBox("Show notifications", notifyOverall, notifyOverall.set, "wrap", updateNotificationCheckboxes)

  // Notifications Format (only for OS X)
  if (mainFrame.monitor.environment.osxNotificationsSupported) {
    add(new JLabel(""), "skip,right")
    val notificationsFormatLabel = new JLabel(t("options.label.notifyformat.label"))
    add(notificationsFormatLabel, "split 2, gapleft 27")
    notificationsFormat = addComboBox[NotificationType](
      Array(t("options.label.notifyformat.hearthstats"), t("options.label.notifyformat.osx")),
      optionNotificationType, optionNotificationType.set, "",
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
    addCheckBox(t("options.notification.hs_found"), notifyHsFound, notifyHsFound.set)

  // Hearthstone Closed Notification
  addLabel()
  private val showHsClosedField =
    addCheckBox(t("options.notification.hs_closed"), notifyHsClosed, notifyHsClosed.set)

  // Game Screen Changed Notification
  addLabel()
  private val showScreenNotificationField =
    addCheckBox(t("options.notification.screen"), notifyScreen, notifyScreen.set)

  // Game Mode Changed Notification
  addLabel()
  private val showModeNotificationField =
    addCheckBox(t("options.notification.mode"), notifyMode, notifyMode.set)

  // Deck Changed Notification
  addLabel()
  private val showDeckNotificationField =
    addCheckBox(t("options.notification.deck"), notifyDeck, notifyDeck.set)

  // Your Turn Notification
  addLabel()
  private val showYourTurnNotificationField =
    addCheckBox(t("options.notification.turn"), notifyTurn, notifyTurn.set)

  // Now that all the notification fields have been created, enable or disable them as appropriate
  updateNotificationCheckboxes(notificationsEnabledField.isSelected)

  // Deck Overlay
  addLabel(t("options.label.deckoverlay"))
  addCheckBox(t("options.ui.deckoverlay"), enableDeckOverlay, enableDeckOverlay.set, "")

  val deckOverlayHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Deck-Overlay",
    "Help on the show deck overlay option")
  add(deckOverlayHelpIcon, "wrap")

  // Match Popup
  addLabel(t("options.label.matchpopup"))
  addComboBox[MatchPopup](Array(t("options.label.matchpopup.always"), t("options.label.matchpopup.incomplete"), t("options.label.matchpopup.never")),
    optionMatchPopup, optionMatchPopup.set, "")

  val matchPopupHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Match-Popup",
    "Help on the match popup options")
  add(matchPopupHelpIcon, "wrap")

  // Minimize to System Tray
  addLabel("Interface: ")
  addCheckBox(t("options.notification.min_to_tray"), enableMinToTray, enableMinToTray.set)

  // Start Minimized
  addLabel()
  addCheckBox(t("options.notification.start_min"), enableStartMin, enableStartMin.set)

  // Analytics
  addLabel("Analytics: ")
  var analyticsField = addCheckBox(t("options.submit_stats"), enableAnalytics, enableAnalytics.set, "wrap", showAnalyticsNotification)

  // Note about saving
  addLabel()
  val saveNoteLabel = new JLabel(t("options.save_automatically"))
  saveNoteLabel.setFont(saveNoteLabel.getFont.deriveFont(Font.ITALIC))
  add(saveNoteLabel, "wrap")

  addLabel()
  val resetButton = new JButton(t("button.reset_default"))
  resetButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent) = onEDT({
      UserConfig.clearPreferences()
      // Recreate the options panel to cause it to reload all the defaults
      mainFrame.tabbedPane.remove(3)
      mainFrame.optionsPanel = new OptionsPanel(mainFrame)
      mainFrame.tabbedPane.insertTab(t("tab.options"), null, mainFrame.optionsPanel, null, 3)
      mainFrame.tabbedPane.setSelectedIndex(3)
    })
  })
  add(resetButton, "wrap")


  private def updateNotificationCheckboxes(isEnabled: Boolean) {
    if (notificationsFormat != null) {
      notificationsFormat.setEnabled(isEnabled)
    }
    showHsFoundField.setEnabled(isEnabled)
    showHsClosedField.setEnabled(isEnabled)
    showScreenNotificationField.setEnabled(isEnabled)
    showModeNotificationField.setEnabled(isEnabled)
    showDeckNotificationField.setEnabled(isEnabled)
  }

  private def showAnalyticsNotification(isEnabled: Boolean) {
    if (!isEnabled) {
      val dialogResult = JOptionPane.showConfirmDialog(OptionsPanel.this, "A lot of work has gone into this uploader.\n" +
        "It is provided for free, and all we ask in return\n" +
        "is that you let us track basic, anonymous statistics\n" +
        "about how frequently it is being used." +
        "\n\nAre you sure you want to disable analytics?", "Please reconsider...", JOptionPane.YES_NO_OPTION)
      if (dialogResult == JOptionPane.NO_OPTION) {
        analyticsField.setSelected(true)
      }
    }
  }

  private def _updateGameLanguage() {
    TranslationCard.changeTranslation(optionGameLanguage)
  }

  def setUserKey(userkey: String) {
    userKeyField.setText(userkey)
  }

  def addLabel(label: String = ""): JLabel = {
    val control = new JLabel(label + " ")
    add(control, "skip,right")
    control
  }

  def addCheckBox(label: String, getter: => Boolean, setter: Boolean => Unit, constraints: String = "wrap",
    onChange: Boolean => Unit = (value: Boolean) => {}): JCheckBox = {
    val checkBox = new JCheckBox(label)

    // Set the checkbox to the current value from the config
    checkBox.setSelected(getter)

    // When the checkbox is clicked, update the config value
    checkBox.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) = onEDT({
        setter(checkBox.isSelected)
        onChange(checkBox.isSelected)
      })
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
      def actionPerformed(e: ActionEvent) = onEDT({
        // This is a lazy way to get an instance of the class, the old value isn't actually used
        val oldValue = getter;
        val newValue = oldValue.getClass.getEnumConstants.apply(comboBox.getSelectedIndex)
        setter(newValue)
        onChange(newValue)
      })
    })

    add(comboBox, constraints)
    comboBox
  }

}
