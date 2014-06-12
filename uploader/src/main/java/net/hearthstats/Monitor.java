package net.hearthstats;


import com.dmurph.tracking.JGoogleAnalyticsTracker;
import net.hearthstats.analysis.AnalyserEvent;
import net.hearthstats.analysis.HearthstoneAnalyser;
import net.hearthstats.log.Log;
import net.hearthstats.log.LogPane;
import net.hearthstats.logmonitor.HearthstoneLogMonitor;
import net.hearthstats.notification.DialogNotificationQueue;
import net.hearthstats.notification.NotificationQueue;
import net.hearthstats.state.Screen;
import net.hearthstats.state.ScreenGroup;
import net.hearthstats.ui.ClickableDeckBox;
import net.hearthstats.ui.DecksTab;
import net.hearthstats.ui.HelpIcon;
import net.hearthstats.ui.MatchEndPopup;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static net.hearthstats.util.Translations.t;

@SuppressWarnings("serial")
public class Monitor extends JFrame implements Observer {

	private static final String PROFILES_URL = "http://hearthstats.net/uploader";
	private static final int POLLING_INTERVAL_IN_MS = 100;

    private static final EnumSet<Screen> DO_NOT_NOTIFY_SCREENS = EnumSet.of(Screen.COLLECTION, Screen.COLLECTION_ZOOM, Screen.MAIN_TODAYSQUESTS, Screen.TITLE);

    private static Logger debugLog = LoggerFactory.getLogger(Monitor.class);



	protected HearthstoneAnalyser _analyzer = new HearthstoneAnalyser();
	protected ProgramHelper _hsHelper = Config.programHelper();
    protected HearthstoneLogMonitor hearthstoneLogMonitor;

	private HyperlinkListener _hyperLinkListener = HyperLinkHandler.getInstance();
	private JTextField _currentOpponentNameField;
	private JLabel _currentMatchLabel;
	private JCheckBox _currentGameCoinField;
	private JTextArea _currentNotesField;
	private JButton _lastMatchButton;
	private HearthstoneMatch _lastMatch;

	private JComboBox _currentOpponentClassSelect;
	private JComboBox _currentYourClassSelector;

	private boolean _hearthstoneDetected;
    private JGoogleAnalyticsTracker _analytics;
    private LogPane _logText;
	private JScrollPane _logScroll;
	private JTextField _userKeyField;
    private JComboBox monitoringMethodField;
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
	private JCheckBox _showDeckOverlay;
	private JTabbedPane _tabbedPane;

    public Monitor() throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        _notificationQueue = newNotificationQueue();
    }

    public void start() throws IOException {
		if (Config.analyticsEnabled()) {
            debugLog.debug("Enabling analytics");
			_analytics = AnalyticsTracker.tracker();
			_analytics.trackEvent("app","AppStart");
		}
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				handleClose();
			}
		});
		
		createAndShowGui();
		showWelcomeLog();
		checkForUpdates();

		API.addObserver(this);
		_analyzer.addObserver(this);
		_hsHelper.addObserver(this);


		if(_checkForUserKey()) {
			poller.start();
		} else {
			System.exit(1);
		}

        if (Config.os == Config.OS.OSX) {
            Log.info(t("waiting_for_hs"));
        } else {
            Log.info(t("waiting_for_hs_windowed"));
        }
	}

	public void handleClose() {
		Point p = getLocationOnScreen();
		Config.setX(p.x);
		Config.setY(p.y);
		Dimension rect = getSize();
		Config.setWidth((int) rect.getWidth());
		Config.setHeight((int) rect.getHeight());
		try {
			Config.save();
		} catch (Throwable t) {
			Log.warn(
					"Error occurred trying to write settings file, your settings may not be saved",
					t);
		}
		System.exit(0);
	}

	private void showWelcomeLog() {
        debugLog.debug("Showing welcome log messages");

        Log.welcome("HearthStats.net " + t("Uploader") + " v" + Config.getVersionWithOs());

        Log.help(t("welcome_1_set_decks"));
        if (Config.os == Config.OS.OSX) {
            Log.help(t("welcome_2_run_hearthstone"));
            Log.help(t("welcome_3_notifications"));
        } else {
            Log.help(t("welcome_2_run_hearthstone_windowed"));
            Log.help(t("welcome_3_notifications_windowed"));
        }
        String logFileLocation = Log.getLogFileLocation();
        if (logFileLocation == null) {
            Log.help(t("welcome_4_feedback"));
        } else {
            Log.help(t("welcome_4_feedback_with_log", logFileLocation));
        }

    }

	
	private boolean _checkForUserKey() {
		boolean userKeySet = !Config.getUserKey().equals("your_userkey_here");
		if(userKeySet) {
			return true;
		} else {
            Log.warn(t("error.userkey_not_entered"));

            bringWindowToFront();

			JOptionPane.showMessageDialog(this,
					"HearthStats.net " + t("error.title") + ":\n\n" +
					t("you_need_to_enter_userkey") + "\n\n" +
					t("get_it_at_hsnet_profiles"));
			
			Desktop d = Desktop.getDesktop();
			try {
				d.browse(new URI(PROFILES_URL));
			} catch (IOException | URISyntaxException e) {
                Log.warn("Error launching browser with URL " + PROFILES_URL, e);
			}

			String userkey = JOptionPane.showInputDialog(this,
					t("enter_your_userkey"));
			if (StringUtils.isEmpty(userkey)) {
				return false;
			} else {
				Config.setUserKey(userkey);
				try {
					_userKeyField.setText(userkey);
					Config.save();
					Log.info(t("UserkeyStored"));
				} catch (Throwable e) {
					Log.warn(
							"Error occurred trying to write settings file, your settings may not be saved",
							e);
				}
				return true;
			}
		}
	}


    /**
     * Brings the monitor window to the front of other windows. Should only be used for important events like a
     * modal dialog or error that we want the user to see immediately.
     */
    private void bringWindowToFront() {
        final Monitor frame = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }


    /**
     * Overridden version of setVisible based on http://stackoverflow.com/questions/309023/how-to-bring-a-window-to-the-front
     * that should ensure the window is brought to the front for important things like modal dialogs.
     */
    @Override
    public void setVisible(final boolean visible) {
        // let's handle visibility...
        if (!visible || !isVisible()) { // have to check this condition simply because super.setVisible(true) invokes toFront if frame was already visible
            super.setVisible(visible);
        }
        // ...and bring frame to the front.. in a strange and weird way
        if (visible) {
            int state = super.getExtendedState();
            state &= ~JFrame.ICONIFIED;
            super.setExtendedState(state);
            super.setAlwaysOnTop(true);
            super.toFront();
            super.requestFocus();
            super.setAlwaysOnTop(false);
        }
    }


    @Override
    public void toFront() {
        super.setVisible(true);
        int state = super.getExtendedState();
        state &= ~JFrame.ICONIFIED;
        super.setExtendedState(state);
        super.setAlwaysOnTop(true);
        super.toFront();
        super.requestFocus();
        super.setAlwaysOnTop(false);
    }



	private void createAndShowGui() {
        debugLog.debug("Creating GUI");

		Image icon = new ImageIcon(getClass().getResource("/images/icon.png")).getImage();
		setIconImage(icon);
		setLocation(Config.getX(), Config.getY());
		setSize(Config.getWidth(), Config.getHeight());
		
		_tabbedPane = new JTabbedPane();
		add(_tabbedPane);
		
		// log
        _logText = new LogPane();
		_logScroll = new JScrollPane (_logText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_tabbedPane.add(_logScroll, t("tab.log"));
		
		_tabbedPane.add(_createMatchUi(), t("tab.current_match"));
		_tabbedPane.add(decksTab = new DecksTab(), t("tab.decks"));
		_tabbedPane.add(_createOptionsUi(), t("tab.options"));
		_tabbedPane.add(_createAboutUi(), t("tab.about"));
		
		_updateCurrentMatchUi();
		
		_enableMinimizeToTray();
		
		setMinimumSize(new Dimension(500, 600));
		setVisible(true);
		
		if(Config.startMinimized())
			setState(JFrame.ICONIFIED);
		
		_updateTitle();
	}
	
	private JScrollPane _createAboutUi() {
        Map<String, String> localeStrings = new HashMap<String, String>();
        localeStrings.put("Author", t("Author"));
        localeStrings.put("version", t("Uploader") + " v" + Config.getVersion());
        localeStrings.put("utility_l1", t("about.utility_l1"));
        localeStrings.put("utility_l2", t("about.utility_l2"));
        localeStrings.put("utility_l3", t("about.utility_l3"));
        localeStrings.put("open_source_l1", t("about.open_source_l1"));
        localeStrings.put("open_source_l2", t("about.open_source_l2"));
        localeStrings.put("project_source", t("about.project_source"));
        localeStrings.put("releases_and_changelog", t("about.releases_and_changelog"));
        localeStrings.put("feedback_and_suggestions", t("about.feedback_and_suggestions"));
        localeStrings.put("support_project", t("about.support_project"));
        localeStrings.put("donate_image", getClass().getResource("/images/donate.gif").toString());

	    JEditorPane contributorsText = new JEditorPane();
	    contributorsText.setContentType("text/html");
	    contributorsText.setEditable(false);
	    contributorsText.setBackground(Color.WHITE);

        try (Reader cssReader = new InputStreamReader(LogPane.class.getResourceAsStream("/net/hearthstats/about.css"))) {
            ((HTMLDocument)contributorsText.getDocument()).getStyleSheet().loadRules(cssReader, null);
        } catch (IOException e) {
            // If we can't load the About css, log a warning but continue
            Log.warn("Unable to format About tab", e);
        }

        try (Reader aboutReader = new InputStreamReader(LogPane.class.getResourceAsStream("/net/hearthstats/about.html"))) {
            String aboutText = StrSubstitutor.replace(IOUtils.toString(aboutReader), localeStrings);
            contributorsText.setText(aboutText);
        } catch (IOException e) {
            // If we can't load the About text, log a warning but continue
            Log.warn("Unable to display About tab", e);
        }
	    contributorsText.addHyperlinkListener(_hyperLinkListener);

        return new JScrollPane(contributorsText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}
	private JPanel _createMatchUi() {
		JPanel panel = new JPanel();

		MigLayout layout = new MigLayout();
		panel.setLayout(layout);
		
		// match label
		panel.add(new JLabel(" "), "wrap");
		_currentMatchLabel = new JLabel();
		panel.add(_currentMatchLabel, "skip,span,wrap");
		
		panel.add(new JLabel(" "), "wrap");
		
		String[] localizedClassOptions = new String[Constants.hsClassOptions.length];
		localizedClassOptions[0] = "- " + t("undetected") + " -";
		for(int i = 1; i < localizedClassOptions.length; i++)
			localizedClassOptions[i] = t(Constants.hsClassOptions[i]);
		
		// your class
		panel.add(new JLabel(t("match.label.your_class") + " "), "skip,right");
		_currentYourClassSelector = new JComboBox<>(localizedClassOptions);
		panel.add(_currentYourClassSelector, "wrap");
		
		// opponent class
		panel.add(new JLabel(t("match.label.opponents_class") + " "), "skip,right");
		_currentOpponentClassSelect = new JComboBox<>(localizedClassOptions);
		panel.add(_currentOpponentClassSelect, "wrap"); 
		
		// Opponent name
		panel.add(new JLabel("Opponent's Name: "), "skip,right");
		_currentOpponentNameField = new JTextField();
		_currentOpponentNameField.setMinimumSize(new Dimension(100, 1));
		_currentOpponentNameField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				_analyzer.getMatch().opponentName_$eq(
						_currentOpponentNameField.getText().replaceAll(
								"(\r\n|\n)", "<br/>"));
	        }
	    });
		panel.add(_currentOpponentNameField, "wrap");
		
		
		// coin
		panel.add(new JLabel(t("match.label.coin") + " "), "skip,right");
		_currentGameCoinField = new JCheckBox(t("match.coin"));
		_currentGameCoinField.setSelected(Config.showHsClosedNotification());
		_currentGameCoinField.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				_analyzer.getMatch().coin_$eq(
						_currentGameCoinField.isSelected());
			}
		});
		panel.add(_currentGameCoinField, "wrap");
		
		// notes
		panel.add(new JLabel(t("match.label.notes") + " "), "skip,wrap");
		_currentNotesField = new JTextArea();
        _currentNotesField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
		_currentNotesField.setMinimumSize(new Dimension(350, 150));
	    _currentNotesField.setBackground(Color.WHITE);
	    _currentNotesField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				_analyzer.getMatch().notes_$eq(_currentNotesField.getText());
	        }
	    });
	    panel.add(_currentNotesField, "skip,span");

	    panel.add(new JLabel(" "), "wrap");
	    
	    // last match
	    panel.add(new JLabel(t("match.label.previous_match") + " "), "skip,wrap");
	    _lastMatchButton = new JButton("[n/a]");
	    _lastMatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
				String url = "Arena".equals(_lastMatch.mode()) ? "http://hearthstats.net/arenas/new"
						: _lastMatch.editUrl();
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Throwable e) {
                    Main.showErrorDialog("Error launching browser with URL " + url, e);
                }
            }
        });
	    _lastMatchButton.setEnabled(false);
	    panel.add(_lastMatchButton, "skip,wrap,span");
	    
	    return panel;
	}



	private JPanel _createOptionsUi() {
		JPanel panel = new JPanel();
		
		MigLayout layout = new MigLayout();
		panel.setLayout(layout);
		
		panel.add(new JLabel(" "), "wrap");
		
		// user key
		panel.add(new JLabel(t("options.label.userkey") + " "), "skip,right");
		_userKeyField = new JTextField();
		_userKeyField.setText(Config.getUserKey());
		panel.add(_userKeyField, "wrap");

        // monitoring method
        panel.add(new JLabel(t("options.label.monitoring")), "skip,right");
        monitoringMethodField = new JComboBox<>(new String[]{ t("options.label.monitoring.screen"), t("options.label.monitoring.log")});
        monitoringMethodField.setSelectedIndex(Config.monitoringMethod().ordinal());
        panel.add(monitoringMethodField, "");

        HelpIcon monitoringHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Monitoring", "Help on monitoring options");
        panel.add(monitoringHelpIcon, "wrap");


        // check for updates
		panel.add(new JLabel(t("options.label.updates") + " "), "skip,right");
		_checkUpdatesField = new JCheckBox(t("options.check_updates"));
		_checkUpdatesField.setSelected(Config.checkForUpdates());
		panel.add(_checkUpdatesField, "wrap");
		
		// show notifications
		panel.add(new JLabel(t("options.label.notifications") + " "), "skip,right");
		_notificationsEnabledField = new JCheckBox("Show notifications");
		_notificationsEnabledField.setSelected(Config.showNotifications());
		_notificationsEnabledField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	_updateNotificationCheckboxes();
            }
        });
        panel.add(_notificationsEnabledField, "wrap");


        // When running on Mac OS X 10.8 or later, the format of the notifications can be changed
        if (Config.isOsxNotificationsSupported()) {
            panel.add(new JLabel(""), "skip,right");
            JLabel notificationsFormatLabel = new JLabel(t("options.label.notifyformat.label"));
            panel.add(notificationsFormatLabel, "split 2, gapleft 27");
            _notificationsFormat = new JComboBox<>(new String[]{ t("options.label.notifyformat.osx"), t("options.label.notifyformat.hearthstats")});
            _notificationsFormat.setSelectedIndex(Config.useOsxNotifications() ? 0 : 1);
            panel.add(_notificationsFormat, "");

            HelpIcon osxNotificationsHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-OS-X-Notifications", "Help on notification style options");
            panel.add(osxNotificationsHelpIcon, "wrap");
        }

		// show HS found notification
		panel.add(new JLabel(""), "skip,right");
		_showHsFoundField = new JCheckBox(t("options.notification.hs_found"));
		_showHsFoundField.setSelected(Config.showHsFoundNotification());
		panel.add(_showHsFoundField, "wrap");
		
		// show HS closed notification
		panel.add(new JLabel(""), "skip,right");
		_showHsClosedField = new JCheckBox(t("options.notification.hs_closed"));
		_showHsClosedField.setSelected(Config.showHsClosedNotification());
		panel.add(_showHsClosedField, "wrap");
		
		// show game screen notification
		panel.add(new JLabel(""), "skip,right");
		_showScreenNotificationField = new JCheckBox(t("options.notification.screen"));
		_showScreenNotificationField.setSelected(Config.showScreenNotification());
		panel.add(_showScreenNotificationField, "wrap");
		
		// show game mode notification
		panel.add(new JLabel(""), "skip,right");
		_showModeNotificationField = new JCheckBox(t("options.notification.mode"));
		_showModeNotificationField.setSelected(Config.showModeNotification());
		panel.add(_showModeNotificationField, "wrap");
		
		// show deck notification
		panel.add(new JLabel(""), "skip,right");
		_showDeckNotificationField = new JCheckBox(t("options.notification.deck"));
		_showDeckNotificationField.setSelected(Config.showDeckNotification());
		panel.add(_showDeckNotificationField, "wrap");
		
		// show your turn notification
		panel.add(new JLabel(""), "skip,right");
		_showYourTurnNotificationField = new JCheckBox(t("options.notification.turn"));
		_showYourTurnNotificationField.setSelected(Config.showYourTurnNotification());
		panel.add(_showYourTurnNotificationField, "wrap");
		
		_updateNotificationCheckboxes();

		// show deck overlay
		panel.add(new JLabel(""), "skip,right");
		_showDeckOverlay = new JCheckBox(t("options.ui.deckOverlay"));
		_showDeckOverlay.setSelected(Config.showDeckOverlay());
		panel.add(_showDeckOverlay, "");

        HelpIcon deckOverlayHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Deck-Overlay", "Help on the show deck overlay option");
        panel.add(deckOverlayHelpIcon, "wrap");

        // match popup
        panel.add(new JLabel(t("options.label.matchpopup")), "skip,right");

        showMatchPopupField = new JComboBox<>(new String[]{ t("options.label.matchpopup.always"), t("options.label.matchpopup.incomplete"), t("options.label.matchpopup.never")});
        showMatchPopupField.setSelectedIndex(Config.showMatchPopup().ordinal());
        panel.add(showMatchPopupField, "");

        HelpIcon matchPopupHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Match-Popup", "Help on the match popup options");
        panel.add(matchPopupHelpIcon, "wrap");


        // minimize to tray
		panel.add(new JLabel("Interface: "), "skip,right");
		_minToTrayField = new JCheckBox(t("options.notification.min_to_tray"));
		_minToTrayField.setSelected(Config.checkForUpdates());
		panel.add(_minToTrayField, "wrap");
		
		// start minimized
		panel.add(new JLabel(""), "skip,right");
		_startMinimizedField = new JCheckBox(t("options.notification.start_min"));
		_startMinimizedField.setSelected(Config.startMinimized());
		panel.add(_startMinimizedField, "wrap");

		// analytics
		panel.add(new JLabel("Analytics: "), "skip,right");
		_analyticsField = new JCheckBox(t("options.submit_stats"));

        final Monitor frame = this;
		_analyticsField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!_analyticsField.isSelected()) {
					int dialogResult = JOptionPane.showConfirmDialog(frame,
							"A lot of work has gone into this uploader.\n" +
							"It is provided for free, and all we ask in return\n" +
							"is that you let us track basic, anonymous statistics\n" +
							"about how frequently it is being used." +
							"\n\nAre you sure you want to disable analytics?"
							,
							"Please reconsider ...",
							JOptionPane.YES_NO_OPTION);		
					if (dialogResult == JOptionPane.NO_OPTION){
						_analyticsField.setSelected(true);
					}
				}
			}
		});
		_analyticsField.setSelected(Config.analyticsEnabled());
		panel.add(_analyticsField, "wrap");
		
		// Save button
		panel.add(new JLabel(""), "skip,right");
		JButton saveOptionsButton = new JButton(t("button.save_options"));
		saveOptionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	_saveOptions();
            }
        });
		panel.add(saveOptionsButton, "wrap");
		
		
		return panel;
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

	private void checkForUpdates() {
		if(Config.checkForUpdates()) {
            Log.info(t("checking_for_updates..."));
			try {
				String availableVersion = Updater.getAvailableVersion();
				if (availableVersion != null) {
                    Log.info(t("latest_v_available") + " " + availableVersion);
					
					if (!availableVersion.matches(Config.getVersion())) {

                        bringWindowToFront();

						int dialogButton = JOptionPane.YES_NO_OPTION;
						int dialogResult = JOptionPane.showConfirmDialog(this,
								"A new version of this uploader is available\n\n" +
								Updater.getRecentChanges() +
								"\n\n" + t("would_u_like_to_install_update")
								,
								"HearthStats.net " + t("uploader_updates_avail"),
								dialogButton);		
						if (dialogResult == JOptionPane.YES_OPTION){
							/*
							// Create Desktop object
							Desktop d = Desktop.getDesktop();
							// Browse a URL, say google.com
							d.browse(new URI("https://github.com/JeromeDane/HearthStats.net-Uploader/releases"));
							System.exit(0);
							*/
							Updater.run();
						} else {
							dialogResult = JOptionPane.showConfirmDialog(null,
									t("would_you_like_to_disable_updates"),
									t("disable_update_checking"),
									dialogButton);
							if(dialogResult == JOptionPane.YES_OPTION){
								String[] options = { t("button.ok") };
								JPanel panel = new JPanel();
								JLabel lbl = new JLabel(t("reenable_updates_any_time"));
								panel.add(lbl);
								JOptionPane.showOptionDialog(this, panel, t("updates_disabled_msg"), JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
								Config.setCheckForUpdates(false);
							}
						}
					}
				} else {
                    Log.warn("Unable to determine latest available version");
				}
			} catch (Throwable e) {
                e.printStackTrace(System.err);
				_notify("Update Checking Error", "Unable to determine the latest available version");
			}
		}
	}


    /**
     * Sets up the Hearthstone log monitoring if enabled, or stops if it is disabled
     */
    private void setupLogMonitoring() {
        setMonitorHearthstoneLog(Config.monitoringMethod() == Config.MonitoringMethod.SCREEN_LOG);
    }


	protected boolean _drawPaneAdded = false;

	protected BufferedImage image;

	protected JPanel _drawPane = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	};

    protected NotificationQueue _notificationQueue;

    private Boolean _currentMatchEnabled = false;
	private boolean _playingInMatch = false;

	protected void _notify(String header) {
		_notify(header, "");
	}

	protected void _notify(String header, String message) {
		if (Config.showNotifications())
			_notificationQueue.add(header, message, false);
	}


    protected void _updateTitle() {
		 String title = "HearthStats.net Uploader";
		if (_hearthstoneDetected) {
			if (_analyzer.getScreen() != null) {
				title += " - " + _analyzer.getScreen().title;
//                if (_analyzer.getScreen() == "Play" && _analyzer.getMode() != null) {
                if (_analyzer.getScreen() == Screen.PLAY_LOBBY && _analyzer.getMode() != null) {
					title += " " + _analyzer.getMode();
				}
				if (_analyzer.getScreen() == Screen.FINDING_OPPONENT) {
					if (_analyzer.getMode() != null) {
						title += " for " + _analyzer.getMode() + " Game";
					}
				}

                // TODO: replace with enum values
				if ("Match Start".equals(_analyzer.getScreen().title)  || "Playing".equals(_analyzer.getScreen().title)) {
					title += " " + (_analyzer.getMode() == null ? "[undetected]" : _analyzer.getMode());
					title += " " + (_analyzer.getCoin() ? "" : "No ") + "Coin";
					title += " " + (_analyzer.getYourClass() == null ? "[undetected]" : _analyzer.getYourClass());
					title += " VS. " + (_analyzer.getOpponentClass() == null ? "[undetected]" : _analyzer.getOpponentClass());
				}
			}
		} else {
			title += " - Waiting for Hearthstone ";
		}
		setTitle(title);
	}

	private int _getClassOptionIndex(String cName) {
		for (int i = 0; i < Constants.hsClassOptions.length; i++) {
			if (Constants.hsClassOptions[i].equals(cName)) {
                return i;
            }
		}
		return 0;
	}
	private void _updateCurrentMatchUi() {
        HearthstoneMatch match = _analyzer.getMatch();
		_updateMatchClassSelectorsIfSet(match);
		if(_currentMatchEnabled)
			_currentMatchLabel.setText(match.mode() + " Match - " + " Turn "
					+ match.numTurns());
		else 
			_currentMatchLabel.setText("Waiting for next match to start ...");
		_currentOpponentNameField.setText(match.opponentName());
		
		_currentOpponentClassSelect.setSelectedIndex(_getClassOptionIndex(match
				.opponentClass()));
		_currentYourClassSelector.setSelectedIndex(_getClassOptionIndex(match
				.userClass()));
		
		_currentGameCoinField.setSelected(match.coin());
		_currentNotesField.setText(match.notes());
		// last match
		if (_lastMatch != null && _lastMatch.mode() != null) {
			if (_lastMatch.result() != null) {
				String tooltip = (_lastMatch.mode().equals("Arena") ? "View current arena run on"
						: "Edit the previous match")
						+ " on HearthStats.net";
				_lastMatchButton.setToolTipText(tooltip);
				_lastMatchButton.setText(_lastMatch.toString());
				_lastMatchButton.setEnabled(true);
			}
		}
	}
	private void _updateImageFrame() {
		if (!_drawPaneAdded) {
			add(_drawPane);
		}
		if (image.getWidth() >= 1024) {
			setSize(image.getWidth(), image.getHeight());
		}
		_drawPane.repaint();
		invalidate();
		validate();
		repaint();
	}

	private void _submitMatchResult(HearthstoneMatch hsMatch) throws IOException {
		// check for new arena run
		if ("Arena".equals(hsMatch.mode()) && _analyzer.isNewArena()) {
			ArenaRun run = new ArenaRun();
			run.setUserClass(hsMatch.userClass());
            Log.info("Creating new " + run.getUserClass() + "arena run");
			_notify("Creating new " + run.getUserClass() + "arena run");
			API.createArenaRun(run);
			_analyzer.setIsNewArena(false);
		}
		
		String header = "Submitting match result";
		String message = hsMatch.toString(); 
		_notify(header, message);
        Log.matchResult(header + ": " + message);

		if(Config.analyticsEnabled()) {
			_analytics.trackEvent("app", "Submit" + hsMatch.mode() + "Match");
		}
		
		API.createMatch(hsMatch);
	}
	
	private void _resetMatchClassSelectors() {
		_currentYourClassSelector.setSelectedIndex(0);	
		_currentOpponentClassSelect.setSelectedIndex(0);	
	}

	private void _updateMatchClassSelectorsIfSet(HearthstoneMatch hsMatch) {
		if (_currentYourClassSelector.getSelectedIndex() > 0) {
			hsMatch.userClass_$eq(Constants.hsClassOptions[_currentYourClassSelector
					.getSelectedIndex()]);
        }
		if (_currentOpponentClassSelect.getSelectedIndex() > 0) {
			hsMatch.opponentClass_$eq(Constants.hsClassOptions[_currentOpponentClassSelect
					.getSelectedIndex()]);
        }
	}

	protected void _handleHearthstoneFound() {
		// mark hearthstone found if necessary
		if (!_hearthstoneDetected) {
			_hearthstoneDetected = true;
			debugLog.debug("  - hearthstoneDetected");
            if (Config.showHsFoundNotification()) {
				_notify("Hearthstone found");
            }
            if (hearthstoneLogMonitor == null) {
				hearthstoneLogMonitor = new HearthstoneLogMonitor();
            }
            setupLogMonitoring();
		}

        // grab the image from Hearthstone
		debugLog.debug("  - screen capture");
		image = _hsHelper.getScreenCapture();

        if (image == null) {
			debugLog.debug("  - screen capture returned null");
        } else {
			// detect image stats
			if (image.getWidth() >= 1024) {
				debugLog.debug("  - analysing image");
				_analyzer.analyze(image);
            }
			
			if (Config.mirrorGameImage()) {
				debugLog.debug("  - mirroring image");
				_updateImageFrame();
            }
		}
	}
	
	protected void _handleHearthstoneNotFound() {
		
		// mark hearthstone not found if necessary
		if (_hearthstoneDetected) {
			_hearthstoneDetected = false;
			debugLog.debug("  - changed hearthstoneDetected to false");
			if (Config.showHsClosedNotification()) {
				_notify("Hearthstone closed");
				_analyzer.reset();
			}
        }
	}

	private void pollHsImpl() {
		boolean error = false;
		while (!error) {
			try {
				if (_hsHelper.foundProgram()) {
					_handleHearthstoneFound();
				} else {
					debugLog.debug("  - did not find Hearthstone");
					_handleHearthstoneNotFound();
				}
				_updateTitle();
				Thread.sleep(POLLING_INTERVAL_IN_MS);
			} catch (Throwable ex) {
				ex.printStackTrace(System.err);
				debugLog.error("  - exception which is not being handled:", ex);
				while (ex.getCause() != null) {
					ex = ex.getCause();
				}
				Log.error(
						"ERROR: "
								+ ex.getMessage()
								+ ". You will need to restart HearthStats.net Uploader.",
						ex);
				error = true;
			} finally {
				debugLog.debug("<-- finished");
			}

		}
	}
	
	/**
     * Checks whether the match result is complete, showing a popup if necessary to fix the match data,
     * and then submits the match when ready.
     *
     * @param match The match to check and submit.
     */
	private void checkMatchResult(final HearthstoneMatch match) {

        _updateMatchClassSelectorsIfSet(match);

        final Config.MatchPopup matchPopup = Config.showMatchPopup();
        final boolean showPopup;

        switch (matchPopup) {
            case ALWAYS:
                showPopup = true;
                break;
            case INCOMPLETE:
                showPopup = !match.isDataComplete();
                break;
            case NEVER:
                showPopup = false;
                break;
            default:
                throw new UnsupportedOperationException("Unknown config option " + Config.showMatchPopup());
        }

        if (showPopup) {
            // Show a popup allowing the user to edit their match before submitting
            final Monitor monitor = this;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                       boolean matchHasValidationErrors = !match.isDataComplete();
                       String infoMessage = null;
                       do {
                           if (infoMessage == null) {
                               infoMessage = (matchPopup == Config.MatchPopup.INCOMPLETE)
                                       ? t("match.popup.message.incomplete")
                                       : t("match.popup.message.always");
                           }
                           bringWindowToFront();
                           MatchEndPopup.Button buttonPressed = MatchEndPopup.showPopup(monitor, match, infoMessage, t("match.popup.title"));
                           matchHasValidationErrors = !match.isDataComplete();
                           switch (buttonPressed) {
                               case SUBMIT:
                                   if (matchHasValidationErrors) {
                                       infoMessage = "Some match information is incomplete.<br>Please update these details then click Submit to submit the match to HearthStats:";
                                   } else {
                                       _submitMatchResult(match);
                                   }
                                   break;
                               case CANCEL:
                                   return;
                           }

                       } while (matchHasValidationErrors);
                    } catch (IOException e) {
                       Main.showErrorDialog("Error submitting match result", e);
                    }
                }
            });

        } else {
            // Don't show a popup, submit the match directly
            try {
                _submitMatchResult(match);
            } catch (IOException e) {
                Main.showErrorDialog("Error submitting match result", e);
            }
        }
	}


	private void handleAnalyserEvent(AnalyserEvent changed) throws IOException {
		switch(changed) {
			case ARENA_END:
				_notify("End of Arena Run Detected");
                Log.info("End of Arena Run Detected");
			API.endCurrentArenaRun();
				break;

			case COIN:
				_notify("Coin Detected");
                Log.info("Coin Detected");
				break;

			case DECK_SLOT:
				Deck deck = DeckUtils.getDeckFromSlot(_analyzer.getDeckSlot());
				if (deck == null) {
					_tabbedPane.setSelectedIndex(2);
                    bringWindowToFront();
                    Main.showMessageDialog(this, "Unable to determine what deck you have in slot #" + _analyzer.getDeckSlot() + "\n\nPlease set your decks in the \"Decks\" tab.");
				} else {
				_notify("Deck Detected", deck.name());
				Log.info("Deck Detected: " + deck.name() + " Detected");
				}
				
				break;

			case MODE:
				_playingInMatch = false;
				_setCurrentMatchEnabledi(false);
				if (Config.showModeNotification()) {
                    debugLog.debug(_analyzer.getMode() + " level " + _analyzer.getRankLevel());
					if ("Ranked".equals(_analyzer.getMode())) {
                        _notify(_analyzer.getMode() + " Mode Detected", "Rank Level " + _analyzer.getRankLevel());
                    } else {
                        _notify(_analyzer.getMode() + " Mode Detected");
                    }
				}
				if ("Ranked".equals(_analyzer.getMode())) {
                    Log.info(_analyzer.getMode() + " Mode Detected - Level " + _analyzer.getRankLevel());
                } else {
                    Log.info(_analyzer.getMode() + " Mode Detected");
                }
                break;

			case NEW_ARENA:
				if(_analyzer.isNewArena())
					_notify("New Arena Run Detected");
                Log.info("New Arena Run Detected");
				break;

			case OPPONENT_CLASS:
				_notify("Playing vs " + _analyzer.getOpponentClass());
                Log.info("Playing vs " + _analyzer.getOpponentClass());
				break;

			case OPPONENT_NAME:
				_notify("Opponent: " + _analyzer.getOpponentName());
                Log.info("Opponent: " + _analyzer.getOpponentName());
				break;

			case RESULT:
				_playingInMatch = false;
				_setCurrentMatchEnabledi(false);
				_notify(_analyzer.getResult() + " Detected");
                Log.info(_analyzer.getResult() + " Detected");
				checkMatchResult(_analyzer.getMatch());
				break;

			case SCREEN:

				boolean inGameModeScreen = (_analyzer.getScreen() == Screen.ARENA_LOBBY || _analyzer.getScreen() == Screen.ARENA_END || _analyzer.getScreen() == Screen.PLAY_LOBBY);
				if (inGameModeScreen) {
					if (_playingInMatch &&  _analyzer.getResult() == null) {
						_playingInMatch = false;
						_notify("Detection Error", "Match result was not detected.");
                        Log.info("Detection Error: Match result was not detected.");
						checkMatchResult(_analyzer.getMatch());
					}
					_playingInMatch = false;
				} 
				
				if (_analyzer.getScreen() == Screen.FINDING_OPPONENT) {
                    // Ensure that log monitoring is running before starting the match because Hearthstone may only have created the log file
                    // after the HearthStats Uploader started up. In that case log monitoring won't yet be running.
                    setupLogMonitoring();
					_resetMatchClassSelectors();
					//TODO : also display the overlay for Practice mode (usefull for tests)
                    if (Config.showDeckOverlay() && !"Arena".equals(_analyzer.getMode())) {
                        Deck selectedDeck = DeckUtils.getDeckFromSlot(_analyzer.getDeckSlot());
					if (selectedDeck != null && selectedDeck.isValid()
							&& hearthstoneLogMonitor != null) {
						ClickableDeckBox.showBox(
								selectedDeck,
								hearthstoneLogMonitor
									.cardEvents());
                        } else {
                            String message;
                            if (selectedDeck == null) {
                                message = "Invalid or empty deck, edit it on HearthStats.net to display deck overlay (you will need to restart the uploader)";
                            } else {
                                message = String.format("Invalid or empty deck, <a href='http://hearthstats.net/decks/%s/edit'>edit it on HearthStats.net</a> to display deck overlay (you will need to restart the uploader)", selectedDeck.slug());
                            }
                            _notify(message);
                            Log.info(message);
                        }
                    }
				}

				if (_analyzer.getScreen().group == ScreenGroup.MATCH_START) {
					_setCurrentMatchEnabledi(true);
					_playingInMatch = true;
				}

                if (_analyzer.getScreen().group != ScreenGroup.MATCH_END && !DO_NOT_NOTIFY_SCREENS.contains(_analyzer.getScreen())
                        && Config.showScreenNotification()) {
					if (_analyzer.getScreen() == Screen.PRACTICE_LOBBY) {
                        _notify(_analyzer.getScreen().title + " Screen Detected", "Results are not tracked in practice mode");
                    } else {
                        _notify(_analyzer.getScreen().title + " Screen Detected");
                    }
				}

				if (_analyzer.getScreen() == Screen.PRACTICE_LOBBY) {
                    Log.info(_analyzer.getScreen().title + " Screen Detected. Result tracking disabled.");
                } else {
					if (_analyzer.getScreen() == Screen.MATCH_VS) {
                        Log.divider();
                    }
                    Log.info(_analyzer.getScreen().title + " Screen Detected");
				}
				break;

			case YOUR_CLASS:
				_notify("Playing as " + _analyzer.getYourClass());
                Log.info("Playing as " + _analyzer.getYourClass());
				break;

			case YOUR_TURN:
				if (Config.showYourTurnNotification()) {
                    _notify((_analyzer.isYourTurn() ? "Your" : "Opponent") + " turn detected");
                }
                Log.info((_analyzer.isYourTurn() ? "Your" : "Opponent") + " turn detected");
				break;

            case ERROR_ANALYSING_IMAGE:
                _notify("Error analysing opponent name image");
                Log.info("Error analysing opponent name image");
                break;

			default:
				_notify("Unhandled event");
                Log.info("Unhandled event");
		}
		_updateCurrentMatchUi();
	}
	
    public LogPane getLogPane() {
        return _logText;
    }

	private void _handleApiEvent(Object changed) {
		switch(changed.toString()) {
			case "error":
			_notify("API Error", API.message());
			Log.error("API Error: " + API.message());
			Main.showMessageDialog(this, "API Error: " + API.message());
				break;
			case "result":
			Log.info("API Result: " + API.message());
				_lastMatch = _analyzer.getMatch();
			_lastMatch.id_$eq(API.lastMatchId());
				_setCurrentMatchEnabledi(false);
				_updateCurrentMatchUi();
				// new line after match result
			if (API.message().matches(
					".*(Edit match|Arena match successfully created).*")) {
					_analyzer.resetMatch();
					_resetMatchClassSelectors();
                    Log.divider();
				}
				break;
		}
	}
	
	private void _handleProgramHelperEvent(Object changed) {
        Log.info(changed.toString());
		if (changed.toString().matches(".*minimized.*")) {
            _notify("Hearthstone Minimized", "Warning! No detection possible while minimized.");
        }
		if (changed.toString().matches(".*fullscreen.*")) {
            JOptionPane.showMessageDialog(this, "Hearthstats.net Uploader Warning! \n\nNo detection possible while Hearthstone is in fullscreen mode.\n\nPlease set Hearthstone to WINDOWED mode and close and RESTART Hearthstone.\n\nSorry for the inconvenience.");
        }
        if (changed.toString().matches(".*restored.*")) {
            _notify("Hearthstone Restored", "Resuming detection ...");
        }
	}
	
	@Override
	public void update(Observable dispatcher, Object changed) {
        String dispatcherClass = dispatcher == null ? "" : dispatcher.getClass().getCanonicalName();
		if (dispatcherClass.startsWith("net.hearthstats.analysis.HearthstoneAnalyser"))
			try {
				handleAnalyserEvent((AnalyserEvent) changed);
			} catch (IOException e) {
				Main.showErrorDialog("Error handling analyzer event", e);
			}
		if (dispatcherClass.startsWith("net.hearthstats.API"))
			_handleApiEvent(changed);
		
		if (dispatcherClass.matches(".*ProgramHelper(Windows|Osx)?"))
			_handleProgramHelperEvent(changed);
	}

	

	private void _saveOptions() {
        debugLog.debug("Saving options...");

        Config.MonitoringMethod monitoringMethod = Config.MonitoringMethod.values()[monitoringMethodField.getSelectedIndex()];

		Config.setUserKey(_userKeyField.getText());
        Config.setMonitoringMethod(monitoringMethod);
		Config.setCheckForUpdates(_checkUpdatesField.isSelected());
		Config.setShowNotifications(_notificationsEnabledField.isSelected());
		Config.setShowHsFoundNotification(_showHsFoundField.isSelected());
		Config.setShowHsClosedNotification(_showHsClosedField.isSelected());
		Config.setShowScreenNotification(_showScreenNotificationField.isSelected());
		Config.setShowModeNotification(_showModeNotificationField.isSelected());
		Config.setShowDeckNotification(_showDeckNotificationField.isSelected());
		Config.setShowYourTurnNotification(_showYourTurnNotificationField.isSelected());
		Config.setShowDeckOverlay(_showDeckOverlay.isSelected());
        Config.setShowMatchPopup(Config.MatchPopup.values()[showMatchPopupField.getSelectedIndex()]);
		Config.setAnalyticsEnabled(_analyticsField.isSelected());
		Config.setMinToTray(_minToTrayField.isSelected());
		Config.setStartMinimized(_startMinimizedField.isSelected());

        if (_notificationsFormat != null) {
            // This control only appears on OS X machines, will be null on Windows machines
            Config.setUseOsxNotifications(_notificationsFormat.getSelectedIndex() == 0);
            _notificationQueue = newNotificationQueue();
        }

        setupLogMonitoring();

        try {
            Config.save();
            debugLog.debug("...save complete");
            JOptionPane.showMessageDialog(this, "Options Saved");
        } catch (Throwable e) {
            Log.warn("Error occurred trying to write settings file, your settings may not be saved", e);
            JOptionPane.showMessageDialog(null, "Error occurred trying to write settings file, your settings may not be saved");
        }
	}
	
	private void _setCurrentMatchEnabledi(Boolean enabled){
		_currentMatchEnabled = enabled;
		_currentYourClassSelector.setEnabled(enabled);
		_currentOpponentClassSelect.setEnabled(enabled);
		_currentGameCoinField.setEnabled(enabled);
		_currentOpponentNameField.setEnabled(enabled);
		_currentNotesField.setEnabled(enabled);
	}
	
	//http://stackoverflow.com/questions/7461477/how-to-hide-a-jframe-in-system-tray-of-taskbar
	TrayIcon trayIcon;
    SystemTray tray;
	private Thread poller = new Thread(new Runnable() {
		@Override
		public void run() {
			pollHsImpl();
		}
	});
	private DecksTab decksTab;

    private void _enableMinimizeToTray(){
        if(SystemTray.isSupported()){
        	
            tray = SystemTray.getSystemTray();

            ActionListener exitListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            };
            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem("Restore");
            defaultItem.setFont(new Font("Arial",Font.BOLD,14));
            defaultItem.addActionListener(new ActionListener() {
            	public void actionPerformed(ActionEvent e) {
            		setVisible(true);
            		setExtendedState(JFrame.NORMAL);
            	}
            });
            popup.add(defaultItem);
            defaultItem = new MenuItem("Exit");
            defaultItem.addActionListener(exitListener);
            defaultItem.setFont(new Font("Arial",Font.PLAIN,14));
            popup.add(defaultItem);
            Image icon = new ImageIcon(getClass().getResource("/images/icon.png")).getImage();
            trayIcon = new TrayIcon(icon, "HearthStats.net Uploader", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter(){
            	public void mousePressed(MouseEvent e){
            		if(e.getClickCount() >= 2){
            			setVisible(true);
                		setExtendedState(JFrame.NORMAL);
            		}
            	}
            });
        } else {
            debugLog.debug("system tray not supported");
        }
        addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                if (Config.minimizeToTray()) {
                    if (e.getNewState() == ICONIFIED) {
                        try {
                            tray.add(trayIcon);
                            setVisible(false);
                        } catch (AWTException ex) {
                        }
                    }
                    if (e.getNewState() == 7) {
                        try {
                            tray.add(trayIcon);
                            setVisible(false);
                        } catch (AWTException ex) {
                        }
                    }
                    if (e.getNewState() == MAXIMIZED_BOTH) {
                        tray.remove(trayIcon);
                        setVisible(true);
                    }
                    if (e.getNewState() == NORMAL) {
                        tray.remove(trayIcon);
                        setVisible(true);
                        debugLog.debug("Tray icon removed");
                    }
                }
            }
        });
        
    }


    public void setMonitorHearthstoneLog(boolean monitorHearthstoneLog) {
        debugLog.debug("setMonitorHearthstoneLog({})", monitorHearthstoneLog);

        if (monitorHearthstoneLog) {
            // Ensure that the Hearthstone log.config file has been created
            Boolean configWasCreated = _hsHelper.createConfig();
           
            // Start monitoring the Hearthstone log immediately if Hearthstone is already running
            if (_hearthstoneDetected) {
                if (configWasCreated) {
                    // Hearthstone won't actually be logging yet because the log.config was created after Hearthstone started up
                    Log.help("Hearthstone log.config changed &mdash; please restart Hearthstone so that it starts generating logs");
				} else if (hearthstoneLogMonitor == null)
                	hearthstoneLogMonitor=new HearthstoneLogMonitor();
            }
        } else {
            // Stop monitoring the Hearthstone log
            if (hearthstoneLogMonitor != null) {
                hearthstoneLogMonitor.stop();
                hearthstoneLogMonitor = null;
            }
        }
    }


    private static NotificationQueue newNotificationQueue() {
        if (Config.useOsxNotifications()) {
            try {
                return (NotificationQueue) Class.forName("net.hearthstats.osx.OsxNotificationQueue").newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not create OsxNotificationQueue instance due to " + e.getMessage(), e);
            }
        } else {
            return new DialogNotificationQueue();
        }
    }


}
