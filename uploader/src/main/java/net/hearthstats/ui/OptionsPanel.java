package net.hearthstats.ui;

import static net.hearthstats.util.Translations.t;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.hearthstats.Config;
import net.hearthstats.Monitor;
import net.hearthstats.config.*;
import net.hearthstats.log.Log;
import net.hearthstats.util.TranslationCard;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsPanel extends JPanel {
  private JTextField _userKeyField;
  private JComboBox monitoringMethodField;
  private JComboBox gameLanguageField;
  private JCheckBox _checkUpdatesField;
  private JCheckBox _notificationsEnabledField;
  private JComboBox _notificationsFormat;
  private JCheckBox _showHsFoundField;
  private JCheckBox _showHsClosedField;
  private JCheckBox _showScreenNotificationField;
  private JCheckBox _showModeNotificationField;
  private JCheckBox _showDeckNotificationField;
  private JComboBox showMatchPopupField;
  private JCheckBox _analyticsField;
  private JCheckBox _minToTrayField;
  private JCheckBox _startMinimizedField;
  private JCheckBox _showYourTurnNotificationField;
  private JCheckBox _opponentNameDetectionField;
  private JCheckBox _showDeckOverlay;
  private Monitor monitor;
  private Logger debugLog = LoggerFactory.getLogger(getClass());

  public OptionsPanel(final Monitor monitor) {

    this.monitor = monitor;
    MigLayout layout = new MigLayout();
    setLayout(layout);

    add(new JLabel(" "), "wrap");

    // user key
    add(new JLabel(t("options.label.userkey") + " "), "skip,right");
    _userKeyField = new JTextField();
    _userKeyField.setText(Config.getUserKey());
    add(_userKeyField, "wrap");

    // monitoring method
    add(new JLabel(t("options.label.monitoring")), "skip,right");
    monitoringMethodField = new JComboBox<>(new String[] { t("options.label.monitoring.screen"),
        t("options.label.monitoring.log") });
    monitoringMethodField.setSelectedIndex(Config.monitoringMethod().ordinal());
    add(monitoringMethodField, "");

    HelpIcon monitoringHelpIcon = new HelpIcon(
        "https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Monitoring",
        "Help on monitoring options");
    add(monitoringHelpIcon, "wrap");

    // Hearthstone game language support
    add(new JLabel(t("options.label.game.language")), "skip,right");
    gameLanguageField = new JComboBox<>(new String[] { t("options.label.game.language.eu"),
        t("options.label.game.language.fr") });
    gameLanguageField.setSelectedIndex(Config.gameLanguage().ordinal());
    gameLanguageField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _updateGameLanguage();
      }
    });
    _updateGameLanguage();

    add(gameLanguageField, "wrap");

    // check for updates
    add(new JLabel(t("options.label.updates") + " "), "skip,right");
    _checkUpdatesField = new JCheckBox(t("options.check_updates"));
    _checkUpdatesField.setSelected(Config.checkForUpdates());
    add(_checkUpdatesField, "wrap");

    // show notifications
    add(new JLabel(t("options.label.notifications") + " "), "skip,right");
    _notificationsEnabledField = new JCheckBox("Show notifications");
    _notificationsEnabledField.setSelected(Config.showNotifications());
    _notificationsEnabledField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _updateNotificationCheckboxes();
      }
    });
    add(_notificationsEnabledField, "wrap");

    // When running on Mac OS X 10.8 or later, the format of the
    // notifications can be changed
    if (monitor.environment().osxNotificationsSupported()) {
      add(new JLabel(""), "skip,right");
      JLabel notificationsFormatLabel = new JLabel(t("options.label.notifyformat.label"));
      add(notificationsFormatLabel, "split 2, gapleft 27");
      _notificationsFormat = new JComboBox<>(new String[] { t("options.label.notifyformat.osx"),
          t("options.label.notifyformat.hearthstats") });
      _notificationsFormat.setSelectedIndex(Config.useOsxNotifications() ? 0 : 1);
      add(_notificationsFormat, "");

      HelpIcon osxNotificationsHelpIcon = new HelpIcon(
          "https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-OS-X-Notifications",
          "Help on notification style options");
      add(osxNotificationsHelpIcon, "wrap");
    }

    // show HS found notification
    add(new JLabel(""), "skip,right");
    _showHsFoundField = new JCheckBox(t("options.notification.hs_found"));
    _showHsFoundField.setSelected(Config.showHsFoundNotification());
    add(_showHsFoundField, "wrap");

    // show HS closed notification
    add(new JLabel(""), "skip,right");
    _showHsClosedField = new JCheckBox(t("options.notification.hs_closed"));
    _showHsClosedField.setSelected(Config.showHsClosedNotification());
    add(_showHsClosedField, "wrap");

    // show game screen notification
    add(new JLabel(""), "skip,right");
    _showScreenNotificationField = new JCheckBox(t("options.notification.screen"));
    _showScreenNotificationField.setSelected(Config.showScreenNotification());
    add(_showScreenNotificationField, "wrap");

    // show game mode notification
    add(new JLabel(""), "skip,right");
    _showModeNotificationField = new JCheckBox(t("options.notification.mode"));
    _showModeNotificationField.setSelected(Config.showModeNotification());
    add(_showModeNotificationField, "wrap");

    // show deck notification
    add(new JLabel(""), "skip,right");
    _showDeckNotificationField = new JCheckBox(t("options.notification.deck"));
    _showDeckNotificationField.setSelected(Config.showDeckNotification());
    add(_showDeckNotificationField, "wrap");

    // show your turn notification
    add(new JLabel(""), "skip,right");
    _showYourTurnNotificationField = new JCheckBox(t("options.notification.turn"));
    _showYourTurnNotificationField.setSelected(Config.showYourTurnNotification());
    add(_showYourTurnNotificationField, "wrap");

    _updateNotificationCheckboxes();

    // show opponent name field
    add(new JLabel(""), "skip,right");
    _opponentNameDetectionField = new JCheckBox(t("options.opponent.name.detection"));
    _opponentNameDetectionField.setSelected(Config.showOpponentName());
    add(_opponentNameDetectionField, "wrap");

    // show deck overlay
    add(new JLabel(""), "skip,right");
    _showDeckOverlay = new JCheckBox(t("options.ui.deckOverlay"));
    _showDeckOverlay.setSelected(Config.showDeckOverlay());
    add(_showDeckOverlay, "");

    HelpIcon deckOverlayHelpIcon = new HelpIcon(
        "https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Deck-Overlay",
        "Help on the show deck overlay option");
    add(deckOverlayHelpIcon, "wrap");

    // match popup
    add(new JLabel(t("options.label.matchpopup")), "skip,right");

    showMatchPopupField = new JComboBox<>(new String[] { t("options.label.matchpopup.always"),
        t("options.label.matchpopup.incomplete"), t("options.label.matchpopup.never") });
    showMatchPopupField.setSelectedIndex(Config.showMatchPopup().ordinal());
    add(showMatchPopupField, "");

    HelpIcon matchPopupHelpIcon = new HelpIcon(
        "https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Match-Popup",
        "Help on the match popup options");
    add(matchPopupHelpIcon, "wrap");

    // minimize to tray
    add(new JLabel("Interface: "), "skip,right");
    _minToTrayField = new JCheckBox(t("options.notification.min_to_tray"));
    _minToTrayField.setSelected(Config.checkForUpdates());
    add(_minToTrayField, "wrap");

    // start minimized
    add(new JLabel(""), "skip,right");
    _startMinimizedField = new JCheckBox(t("options.notification.start_min"));
    _startMinimizedField.setSelected(Config.startMinimized());
    add(_startMinimizedField, "wrap");

    // analytics
    add(new JLabel("Analytics: "), "skip,right");
    _analyticsField = new JCheckBox(t("options.submit_stats"));

    _analyticsField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!_analyticsField.isSelected()) {
          int dialogResult = JOptionPane.showConfirmDialog(OptionsPanel.this,
              "A lot of work has gone into this uploader.\n"
                  + "It is provided for free, and all we ask in return\n"
                  + "is that you let us track basic, anonymous statistics\n"
                  + "about how frequently it is being used."
                  + "\n\nAre you sure you want to disable analytics?", "Please reconsider ...",
              JOptionPane.YES_NO_OPTION);
          if (dialogResult == JOptionPane.NO_OPTION) {
            _analyticsField.setSelected(true);
          }
        }
      }
    });
    _analyticsField.setSelected(Config.analyticsEnabled());
    add(_analyticsField, "wrap");

    // Save button
    add(new JLabel(""), "skip,right");
    JButton saveOptionsButton = new JButton(t("button.save_options"));
    saveOptionsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _saveOptions(monitor.environment());
      }
    });
    add(saveOptionsButton, "wrap");

  }

  private void _updateNotificationCheckboxes() {
    boolean isEnabled = _notificationsEnabledField.isSelected();
    if (_notificationsFormat != null) {
      _notificationsFormat.setEnabled(isEnabled);
    }
    _showHsFoundField.setEnabled(isEnabled);
    _showHsClosedField.setEnabled(isEnabled);
    _showScreenNotificationField.setEnabled(isEnabled);
    _showModeNotificationField.setEnabled(isEnabled);
    _showDeckNotificationField.setEnabled(isEnabled);
  }


  private void _saveOptions(Environment environment) {
    debugLog.debug("Saving options..."); 

    MonitoringMethod monitoringMethod = MonitoringMethod.values()[monitoringMethodField
        .getSelectedIndex()];
    GameLanguage gameLanguage = GameLanguage.values()[gameLanguageField
        .getSelectedIndex()];

    Config.setUserKey(_userKeyField.getText());
    Config.setMonitoringMethod(monitoringMethod);
    Config.setGameLanguage(gameLanguage);
    Config.setCheckForUpdates(_checkUpdatesField.isSelected());
    Config.setShowNotifications(_notificationsEnabledField.isSelected());
    Config.setShowHsFoundNotification(_showHsFoundField.isSelected());
    Config.setShowHsClosedNotification(_showHsClosedField.isSelected());
    Config.setShowScreenNotification(_showScreenNotificationField.isSelected());
    Config.setShowModeNotification(_showModeNotificationField.isSelected());
    Config.setShowDeckNotification(_showDeckNotificationField.isSelected());
    Config.setShowYourTurnNotification(_showYourTurnNotificationField.isSelected());
    Config.setShowDeckOverlay(_showDeckOverlay.isSelected());
    Config.setShowMatchPopup(MatchPopup.values()[showMatchPopupField.getSelectedIndex()]);
    Config.setAnalyticsEnabled(_analyticsField.isSelected());
    Config.setMinToTray(_minToTrayField.isSelected());
    Config.setStartMinimized(_startMinimizedField.isSelected());

    if (_notificationsFormat != null) {
      // This control only appears on OS X machines, will be null on Windows machines
      Config.setUseOsxNotifications(_notificationsFormat.getSelectedIndex() == 0);
      monitor.setNotificationQueue(environment.newNotificationQueue(Config.notificationType()));
    }

    monitor.setupLogMonitoring();

    try {
      Config.save();
      debugLog.debug("...save complete");
      JOptionPane.showMessageDialog(this, "Options Saved");
    } catch (Throwable e) {
      Log.warn("Error occurred trying to write settings file, your settings may not be saved", e);
      JOptionPane.showMessageDialog(null,
          "Error occurred trying to write settings file, your settings may not be saved");
    }
  }

  private void _updateGameLanguage() {
    TranslationCard.changeTranslation();

  }

  public void setUserKey(String userkey) {
    _userKeyField.setText(userkey);
  }
}
