package net.hearthstats;


import net.hearthstats.analysis.AnalyserEvent;
import net.hearthstats.analysis.HearthstoneAnalyser;
import net.hearthstats.log.Log;
import net.hearthstats.log.LogPane;
import net.hearthstats.notification.DialogNotificationQueue;
import net.hearthstats.notification.NotificationQueue;
import net.hearthstats.notification.OsxNotificationQueue;
import net.hearthstats.state.Screen;
import net.hearthstats.state.ScreenGroup;
import net.hearthstats.ui.MatchEndPopup;
import net.hearthstats.util.Rank;
import net.miginfocom.swing.MigLayout;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dmurph.tracking.JGoogleAnalyticsTracker;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class Monitor extends JFrame implements Observer, WindowListener {

    private static final String PROFILES_URL = "http://hearthstats.net/profiles";
    private static final String DECKS_URL = "http://hearthstats.net/decks";
	private static final int POLLING_INTERVAL_IN_MS = 100;
    private static final int MAX_THREADS = 5;
    private static final int GC_FREQUENCY = 20;

    private static final EnumSet<Screen> DO_NOT_NOTIFY_SCREENS = EnumSet.of(Screen.COLLECTION, Screen.COLLECTION_ZOOM, Screen.MAIN_TODAYSQUESTS, Screen.TITLE);

    private static Logger debugLog = LoggerFactory.getLogger(Monitor.class);
    private static Logger perfLog = LoggerFactory.getLogger("net.hearthstats.performance");

    public static final String[] hsClassOptions = {
            "- undetected -",
            "Druid",
            "Hunter",
            "Mage",
            "Paladin",
            "Priest",
            "Rogue",
            "Shaman",
            "Warlock",
            "Warrior"
    };

    protected API _api = new API();
	protected HearthstoneAnalyser _analyzer = new HearthstoneAnalyser();
	protected ProgramHelper _hsHelper;
	
	private HyperlinkListener _hyperLinkListener = HyperLinkHandler.getInstance();
	private JTextField _currentOpponentNameField;
	private JLabel _currentMatchLabel;
	private JCheckBox _currentGameCoinField;
	private JTextArea _currentNotesField;
	private JButton _lastMatchButton;
	private HearthstoneMatch _lastMatch;
	private JComboBox<String> _deckSlot1Field;
	private JComboBox<String> _deckSlot2Field;
	private JComboBox<String> _deckSlot3Field;
	private JComboBox<String> _deckSlot4Field;
	private JComboBox<String> _deckSlot5Field;
	private JComboBox<String> _deckSlot6Field;
	private JComboBox<String> _deckSlot7Field;
	private JComboBox<String> _deckSlot8Field;
	private JComboBox<String> _deckSlot9Field;
	private JComboBox _currentOpponentClassSelect;
	private JComboBox _currentYourClassSelector;

    private int _numThreads = 0;
	private int _pollIterations = 0;
	protected boolean _hearthstoneDetected;
	protected JGoogleAnalyticsTracker _analytics;
	protected LogPane _logText;
	private JScrollPane _logScroll;
	private JTextField _userKeyField;
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
	private JTabbedPane _tabbedPane;
	private ResourceBundle _bundle = ResourceBundle.getBundle("net.hearthstats.resources.Main");

    public Monitor() throws HeadlessException {
    	
        switch (Config.os) {
            case WINDOWS:
                _hsHelper = new ProgramHelperWindows("Hearthstone.exe");
                break;
            case OSX:
                _hsHelper = new ProgramHelperOsx("unity.Blizzard Entertainment.Hearthstone");
                break;
            default:
                throw new UnsupportedOperationException(t("error.os_unsupported"));
        }
    }


    /**
     * Loads text from the main resource bundle, using the local language when available.
     * @param key the key for the desired string
     * @return The requested string
     */
    private String t(String key) {
    	return _bundle.getString(key);
    }

    /**
     * Loads text from the main resource bundle, using the local language when available, and puts the given value into the appropriate spot.
     * @param key the key for the desired string
     * @param value0 a value to place in the {0} placeholder in the string
     * @return The requested string
     */
    private String t(String key, String value0) {
        String message = _bundle.getString(key);
        return MessageFormat.format(message, value0);
    }


    public void start() throws IOException {
		if (Config.analyticsEnabled()) {
            debugLog.debug("Enabling analytics");
			_analytics = AnalyticsTracker.tracker();
			_analytics.trackEvent("app","AppStart");
		}
		addWindowListener(this);
		
		_createAndShowGui();
		_showWelcomeLog();
		_checkForUpdates();
		
		_api.addObserver(this);
		_analyzer.addObserver(this);
		_hsHelper.addObserver(this);


		if(_checkForUserKey()) {
			_pollHearthstone();
		}

        if (Config.os == Config.OS.OSX) {
            Log.info(t("waiting_for_hs"));
        } else {
            Log.info(t("waiting_for_hs_windowed"));
        }
	}


	private void _showWelcomeLog() {
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
		if(Config.getUserKey().equals("your_userkey_here")) {
            Log.warn(t("error.userkey_not_entered"));

            bringWindowToFront();

			JOptionPane.showMessageDialog(this,
					"HearthStats.net " + t("error.title") + ":\n\n" +
					t("you_need_to_enter_userkey") + "\n\n" +
					t("get_it_at_hsnet_profiles"));
			
			// Create Desktop object
			Desktop d = Desktop.getDesktop();
			// Browse a URL, say google.com
			try {
				d.browse(new URI(PROFILES_URL));
			} catch (IOException | URISyntaxException e) {
                Log.warn("Error launching browser with URL " + PROFILES_URL, e);
			}

			String[] options = {t("button.ok"), t("button.cancel")};
			JPanel panel = new JPanel();
			JLabel lbl = new JLabel(t("UserKey"));
			JTextField txt = new JTextField(10);
			panel.add(lbl);
			panel.add(txt);

			int selectedOption = JOptionPane.showOptionDialog(this, panel, t("enter_your_userkey"), JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if(selectedOption == 0) {
			    String userkey = txt.getText();
			    if(userkey.isEmpty()) {
			    	_checkForUserKey();
			    } else {
			    	Config.setUserKey(userkey);
                    try {
                        Config.save();
                    } catch (Throwable e) {
                        Log.warn("Error occurred trying to write settings file, your settings may not be saved", e);
                    }
			    	_userKeyField.setText(userkey);
                    Log.info(t("UserkeyStored"));
			    	_pollHearthstone();
			    }
			} else {
				System.exit(0);
			}
			return false;
		}
		return true;
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



	private void _createAndShowGui() {
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
		_tabbedPane.add(_createDecksUi(), t("tab.decks"));
		_tabbedPane.add(_createOptionsUi(), t("tab.options"));
		_tabbedPane.add(_createAboutUi(), t("tab.about"));
		
		_tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (_tabbedPane.getSelectedIndex() == 2)
                    try {
                        _updateDecksTab();
                    } catch (IOException e1) {
                        _notify(t("error.loading_decks.title"), t("error.loading_decks"));
                        Log.warn(t("error.loading_decks"), e1);
                    }
            }
        });
		
		_updateCurrentMatchUi();
		
		_enableMinimizeToTray();
		
		setMinimumSize(new Dimension(500, 600));
		setVisible(true);
		
		if(Config.startMinimized())
			setState(JFrame.ICONIFIED);
		
		_updateTitle();
	}
	
	private JScrollPane _createAboutUi() {
		
		JPanel panel = new JPanel();
		panel.setMaximumSize(new Dimension(100,100));		
		panel.setBackground(Color.WHITE);
		MigLayout layout = new MigLayout("");
		panel.setLayout(layout);
		
		JEditorPane text = new JEditorPane();
		text.setContentType("text/html");
		text.setEditable(false);
		text.setBackground(Color.WHITE);
		text.setText("<html><body style=\"font-family:'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:10px;\">" +
				"<h2 style=\"font-weight:normal\"><a href=\"http://hearthstats.net\">HearthStats.net</a> " + t("Uploader") + " v" + Config.getVersion() + "</h2>" +
				"<p><strong>" + t("Author") + ":</strong> " +
                        "Jerome Dane (<a href=\"https://plus.google.com/+JeromeDane\">Google+</a>, <a href=\"http://twitter.com/JeromeDane\">Twitter</a>), " +
                        "Charles Gutjahr (<a href=\"http://charlesgutjahr.com\">Website</a>)</p>" +
				"<p>" + t("about.utility_l1") + "<br>" +
					t("about.utility_l2") + "<br>" +
					t("about.utility_l3") + "</p>" +
				"<p>" + t("about.open_source_l1") + "<br>" +
					t("about.open_source_l2") + "</p>" +
				"<p>&bull; <a href=\"https://github.com/HearthStats/HearthStats.net-Uploader/\">" + t("about.project_source") + "</a><br/>" +
				"&bull; <a href=\"https://github.com/HearthStats/HearthStats.net-Uploader/releases\">" + t("about.releases_and_changelog") + "</a><br/>" +
				"&bull; <a href=\"https://github.com/HearthStats/HearthStats.net-Uploader/issues\">" + t("about.feedback_and_suggestions") + "</a><br/>" +
				"&bull; <a href=\"http://redd.it/1wa4rc/\">Reddit thread</a> (please up-vote)</p>" +
				"<p><strong>" + t("about.support_project") + ":</strong></p>" +
				"</body></html>"
			);
	    text.addHyperlinkListener(_hyperLinkListener);
	    panel.add(text, "wrap");
		
	    JButton donateButton = new JButton("<html><img style=\"border-style: none;\" src=\"" + getClass().getResource("/images/donate.gif") + "\"/></html>");
	    donateButton.addActionListener(new ActionListener() {
	    	@Override
	    	public void actionPerformed(ActionEvent e) {
	    		// Create Desktop object
    			Desktop d = Desktop.getDesktop();
    			// Browse a URL, say google.com
    			try {
    				d.browse(new URI("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=UJFTUHZF6WPDS"));
    			} catch (Throwable e1) {
    				Main.showErrorDialog("Error launching browser with donation URL", e1);
    			}
	    	}
	    });
	    donateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    panel.add(donateButton, "wrap");
	    
	    JEditorPane contribtorsText = new JEditorPane();
	    contribtorsText.setContentType("text/html");
	    contribtorsText.setEditable(false);
	    contribtorsText.setBackground(Color.WHITE);
	    contribtorsText.setText("<html><body style=\"font-family:arial,sans-serif; font-size:10px;\">" +
				"<p><strong>Contributors</strong> (listed alphabetically):</p>" +
				"<p>" +
                    "&bull; <a href=\"https://github.com/gtch\">Charles Gutjahr</a> - OS X version and new screen detection<br>" +
                    "&bull; <a href=\"https://github.com/jcrka\">jcrka</a> - Russian translation<br>" +
                    "&bull; <a href=\"https://github.com/JeromeDane\">Jerome Dane</a> - Original developer<br>" +
					"&bull; <a href=\"https://github.com/sargonas\">J Eckert</a> - Fixed notifications spawning taskbar icons<br>" +
					"&bull; <a href=\"https://github.com/nwalsh1995\">nwalsh1995</a> - Started turn detection development<br>" +
					"&bull; <a href=\"https://github.com/remcoros\">Remco Ros</a> (<a href=\"http://hearthstonetracker.com/\">HearthstoneTracker</a>) - Provides advice &amp; suggestins<br>" +
					"&bull; <a href=\"https://github.com/RoiyS\">RoiyS</a> - Added option to disable all notifications<br>" +
				"</p>"+
				"</body></html>"
			);
	    contribtorsText.addHyperlinkListener(_hyperLinkListener);
	    panel.add(contribtorsText, "wrap");
	    
		return new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
		
		String[] localizedClassOptions = new String[hsClassOptions.length];
		localizedClassOptions[0] = "- " + t("undetected") + " -";
		for(int i = 1; i < localizedClassOptions.length; i++)
			localizedClassOptions[i] = t(hsClassOptions[i]);
		
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
				_analyzer.getMatch().setOpponentName(_currentOpponentNameField.getText().replaceAll("(\r\n|\n)", "<br/>"));
	        }
	    });
		panel.add(_currentOpponentNameField, "wrap");
		
		
		// coin
		panel.add(new JLabel(t("match.label.coin") + " "), "skip,right");
		_currentGameCoinField = new JCheckBox(t("match.coin"));
		_currentGameCoinField.setSelected(Config.showHsClosedNotification());
		_currentGameCoinField.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				_analyzer.getMatch().setCoin(_currentGameCoinField.isSelected());
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
				_analyzer.getMatch().setNotes(_currentNotesField.getText());
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
                String url = "Arena".equals(_lastMatch.getMode()) ? "http://hearthstats.net/arenas/new" : _lastMatch.getEditUrl();
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
	private JPanel _createDecksUi() {
		JPanel panel = new JPanel();

		MigLayout layout = new MigLayout();
		panel.setLayout(layout);
		
		panel.add(new JLabel(" "), "wrap");
		panel.add(new JLabel(t("set_your_deck_slots")), "skip,span,wrap");
		panel.add(new JLabel(" "), "wrap");
		
		panel.add(new JLabel(t("deck_slot.label_1")), "skip"); 
		panel.add(new JLabel(t("deck_slot.label_2")), ""); 
		panel.add(new JLabel(t("deck_slot.label_3")), "wrap");
		
		_deckSlot1Field = new JComboBox<>();
		panel.add(_deckSlot1Field, "skip"); 
		_deckSlot2Field = new JComboBox<>();
		panel.add(_deckSlot2Field, ""); 
		_deckSlot3Field = new JComboBox<>();
		panel.add(_deckSlot3Field, "wrap");
		
		panel.add(new JLabel(" "), "wrap");
		
		panel.add(new JLabel(t("deck_slot.label_4")), "skip"); 
		panel.add(new JLabel(t("deck_slot.label_5")), ""); 
		panel.add(new JLabel(t("deck_slot.label_6")), "wrap");
		
		_deckSlot4Field = new JComboBox<>();
		panel.add(_deckSlot4Field, "skip"); 
		_deckSlot5Field = new JComboBox<>();
		panel.add(_deckSlot5Field, ""); 
		_deckSlot6Field = new JComboBox<>();
		panel.add(_deckSlot6Field, "wrap");
		
		panel.add(new JLabel(" "), "wrap");
		
		panel.add(new JLabel(t("deck_slot.label_7")), "skip"); 
		panel.add(new JLabel(t("deck_slot.label_8")), ""); 
		panel.add(new JLabel(t("deck_slot.label_9")), "wrap");
		
		_deckSlot7Field = new JComboBox<>();
		panel.add(_deckSlot7Field, "skip"); 
		_deckSlot8Field = new JComboBox<>();
		panel.add(_deckSlot8Field, ""); 
		_deckSlot9Field = new JComboBox<>();
		panel.add(_deckSlot9Field, "wrap");
		
		panel.add(new JLabel(" "), "wrap");
		panel.add(new JLabel(" "), "wrap");
		
		JButton saveButton = new JButton(t("button.save_deck_slots"));
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_saveDeckSlots();
			}
		});
		panel.add(saveButton, "skip");
		
		JButton refreshButton = new JButton(t("button.refresh"));
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					_updateDecksTab();
				} catch (IOException e1) {
					Main.showErrorDialog("Error updating decks", e1);
				}
			}
		});
		panel.add(refreshButton, "wrap,span");
		
		panel.add(new JLabel(" "), "wrap");
		panel.add(new JLabel(" "), "wrap");
		
		JButton myDecksButton = new JButton(t("manage_decks_on_hsnet"));
		myDecksButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(DECKS_URL));
				} catch (Throwable e1) {
					Main.showErrorDialog("Error launching browser with URL" + DECKS_URL, e1);
				}
			}
		});
		panel.add(myDecksButton, "skip,span");
		
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
            panel.add(_notificationsFormat, "wrap");
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


        panel.add(new JLabel(t("options.label.matchpopup")), "skip,right");

        showMatchPopupField = new JComboBox<>(new String[]{ t("options.label.matchpopup.always"), t("options.label.matchpopup.incomplete"), t("options.label.matchpopup.never")});
        showMatchPopupField.setSelectedIndex(Config.showMatchPopup().ordinal());
        panel.add(showMatchPopupField, "wrap");


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
	private void _applyDecksToSelector(JComboBox<String> selector, Integer slotNum) {
		
		selector.setMaximumSize(new Dimension(145, selector.getSize().height));
		selector.removeAllItems();
		
		selector.addItem("- Select a deck -");
		
		List<JSONObject> decks = DeckSlotUtils.getDecks();
		
		for(int i = 0; i < decks.size(); i++) {
			selector.addItem(decks.get(i).get("name") + "                                       #" + decks.get(i).get("id"));
			if(decks.get(i).get("slot") != null && decks.get(i).get("slot").toString().equals(slotNum.toString()))
				selector.setSelectedIndex(i + 1);
		}
	}
	private void _updateDecksTab() throws IOException {
		DeckSlotUtils.updateDecks();
		_applyDecksToSelector(_deckSlot1Field, 1);
		_applyDecksToSelector(_deckSlot2Field, 2);
		_applyDecksToSelector(_deckSlot3Field, 3);
		_applyDecksToSelector(_deckSlot4Field, 4);
		_applyDecksToSelector(_deckSlot5Field, 5);
		_applyDecksToSelector(_deckSlot6Field, 6);
		_applyDecksToSelector(_deckSlot7Field, 7);
		_applyDecksToSelector(_deckSlot8Field, 8);
		_applyDecksToSelector(_deckSlot9Field, 9);
	}
	private void _checkForUpdates() {
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

	protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(MAX_THREADS);

	protected boolean _drawPaneAdded = false;

	protected BufferedImage image;

	protected JPanel _drawPane = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	};

    protected NotificationQueue _notificationQueue = Config.useOsxNotifications() ? new OsxNotificationQueue() : new DialogNotificationQueue();
	private Boolean _currentMatchEnabled = false;
	private boolean _playingInMatch = false;

	protected void _notify(String header) {
		_notify(header, "");
	}

	protected void _notify(String header, String message) {
		if (!Config.showNotifications())
			return;	//Notifications disabled

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
		for (int i = 0; i < hsClassOptions.length; i++) {
			if (hsClassOptions[i].equals(cName)) {
                return i;
            }
		}
		return 0;
	}
	private void _updateCurrentMatchUi() {
        HearthstoneMatch match = _analyzer.getMatch();
		_updateMatchClassSelectorsIfSet(match);
		if(_currentMatchEnabled)
			_currentMatchLabel.setText(match.getMode() + " Match - " + " Turn " + match.getNumTurns());
		else 
			_currentMatchLabel.setText("Waiting for next match to start ...");
		_currentOpponentNameField.setText(match.getOpponentName());
		
		_currentOpponentClassSelect.setSelectedIndex(_getClassOptionIndex(match.getOpponentClass()));
		_currentYourClassSelector.setSelectedIndex(_getClassOptionIndex(match.getUserClass()));
		
		_currentGameCoinField.setSelected(match.hasCoin());
		_currentNotesField.setText(match.getNotes());
		// last match
		if(_lastMatch != null && _lastMatch.getMode() != null) {
			if(_lastMatch.getResult() != null) {
				String tooltip = (_lastMatch.getMode().equals("Arena") ? "View current arena run on" : "Edit the previous match") + " on HearthStats.net";
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
		if ("Arena".equals(hsMatch.getMode()) && _analyzer.isNewArena()) {
			ArenaRun run = new ArenaRun();
			run.setUserClass(hsMatch.getUserClass());
            Log.info("Creating new " + run.getUserClass() + "arena run");
			_notify("Creating new " + run.getUserClass() + "arena run");
			_api.createArenaRun(run);
			_analyzer.setIsNewArena(false);
		}
		
		String header = "Submitting match result";
		String message = hsMatch.toString(); 
		_notify(header, message);
        Log.matchResult(header + ": " + message);

		if(Config.analyticsEnabled()) {
			_analytics.trackEvent("app", "Submit" + hsMatch.getMode() + "Match");
		}
		
		_api.createMatch(hsMatch);
	}
	
	private void _resetMatchClassSelectors() {
		_currentYourClassSelector.setSelectedIndex(0);	
		_currentOpponentClassSelect.setSelectedIndex(0);	
	}

	private void _updateMatchClassSelectorsIfSet(HearthstoneMatch hsMatch) {
		if (_currentYourClassSelector.getSelectedIndex() > 0) {
            hsMatch.setUserClass(hsClassOptions[_currentYourClassSelector.getSelectedIndex()]);
        }
		if (_currentOpponentClassSelect.getSelectedIndex() > 0) {
            hsMatch.setOpponentClass(hsClassOptions[_currentOpponentClassSelect.getSelectedIndex()]);
        }
	}

	protected void _handleHearthstoneFound(int currentPollIteration) {
        debugLog.debug("  - Iteration {} found Hearthstone", currentPollIteration);

		// mark hearthstone found if necessary
		if (!_hearthstoneDetected) {
			_hearthstoneDetected = true;
            debugLog.debug("  - Iteration {} changed hearthstoneDetected to true", currentPollIteration);
            if (Config.showHsFoundNotification()) {
				_notify("Hearthstone found");
            }
		}
		
		// grab the image from Hearthstone
        debugLog.debug("  - Iteration {} screen capture", currentPollIteration);
		image = _hsHelper.getScreenCapture();

        if (image == null) {
            debugLog.debug("  - Iteration {} screen capture returned null", currentPollIteration);
        } else {
			// detect image stats
			if (image.getWidth() >= 1024) {
                debugLog.debug("  - Iteration {} analysing image", currentPollIteration);
				_analyzer.analyze(image);
            }
			
			if (Config.mirrorGameImage()) {
                debugLog.debug("  - Iteration {} mirroring image", currentPollIteration);
				_updateImageFrame();
            }
		}
	}
	
	protected void _handleHearthstoneNotFound(int currentPollIteration) {
		
		// mark hearthstone not found if necessary
		if (_hearthstoneDetected) {
			_hearthstoneDetected = false;
            debugLog.debug("  - Iteration {} changed hearthstoneDetected to false", currentPollIteration);
			if (Config.showHsClosedNotification()) {
				_notify("Hearthstone closed");
				_analyzer.reset();
			}
			
		}
	}
	protected void _pollHearthstone() {
        scheduledExecutorService.schedule(new Callable<Object>() {
			public Object call() throws Exception {
                _numThreads++;
                _pollIterations++;

                // A copy of pollIterations is kept in localPollIterations
                int currentPollIteration = _pollIterations;

                try {
                    debugLog.debug("--> Iteration {} started", currentPollIteration);

                    if (_hsHelper.foundProgram()) {
                        _handleHearthstoneFound(currentPollIteration);
                    } else {
                        debugLog.debug("  - Iteration {} did not find Hearthstone", currentPollIteration);
                        _handleHearthstoneNotFound(currentPollIteration);
                    }

                    _updateTitle();

                    _pollHearthstone();        // repeat the process

                    // Keep memory usage down by telling the JVM to perform a garbage collection after every eighth poll (ie GC 1-2 times per second)
                    if (_pollIterations % GC_FREQUENCY == 0 && Runtime.getRuntime().totalMemory() > 150000000) {
                        debugLog.debug("  - Iteration {} triggers GC", currentPollIteration);
                        System.gc();
                    }

                    _numThreads--;

                } catch (Throwable ex) {
                    debugLog.error("  - Iteration " + currentPollIteration + " caused exception which is not being handled:", ex);
                    while (ex.getCause() != null) {
                        ex = ex.getCause();
                    }
                    Log.error("ERROR: " + ex.getMessage() + ". You will need to restart HearthStats.net Uploader.", ex);
                } finally {
                    debugLog.debug("<-- Iteration {} finished", currentPollIteration);
                }

                return "";
            }
		}, POLLING_INTERVAL_IN_MS, TimeUnit.MILLISECONDS);
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
				_api.endCurrentArenaRun();
				break;

			case COIN:
				_notify("Coin Detected");
                Log.info("Coin Detected");
				break;

			case DECK_SLOT:
				JSONObject deck = DeckSlotUtils.getDeckFromSlot(_analyzer.getDeckSlot());
				if (deck == null) {
					_tabbedPane.setSelectedIndex(2);
                    bringWindowToFront();
                    Main.showMessageDialog(this, "Unable to determine what deck you have in slot #" + _analyzer.getDeckSlot() + "\n\nPlease set your decks in the \"Decks\" tab.");
				} else {
					_notify("Deck Detected", deck.get("name").toString());
                    Log.info("Deck Detected: " + deck.get("name") + " Detected");
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
					_resetMatchClassSelectors();
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
				_notify("API Error", _api.getMessage());
				Log.error("API Error: " + _api.getMessage());
				Main.showMessageDialog(this, "API Error: " + _api.getMessage());
				break;
			case "result":
				Log.info("API Result: " + _api.getMessage());
				_lastMatch = _analyzer.getMatch();
				_lastMatch.setId(_api.getLastMatchId());
				_setCurrentMatchEnabledi(false);
				_updateCurrentMatchUi();
				// new line after match result
				if(_api.getMessage().matches(".*(Edit match|Arena match successfully created).*")) {
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
		if (dispatcher.getClass().isAssignableFrom(HearthstoneAnalyser.class))
			try {
				handleAnalyserEvent((AnalyserEvent) changed);
			} catch (IOException e) {
				Main.showErrorDialog("Error handling analyzer event", e);
			}
		if(dispatcher.getClass().isAssignableFrom(API.class))
			_handleApiEvent(changed);
		
		if(dispatcher.getClass().toString().matches(".*ProgramHelper(Windows|Osx)?"))
			_handleProgramHelperEvent(changed);
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
        debugLog.debug("closed");
	}

	@Override
	public void windowClosing(WindowEvent e) {
		Point p = getLocationOnScreen();
		Config.setX(p.x);
		Config.setY(p.y);
		Dimension rect = getSize();
		Config.setWidth((int) rect.getWidth());
		Config.setHeight((int) rect.getHeight());
        try {
            Config.save();
        } catch (Throwable t) {
            Log.warn("Error occurred trying to write settings file, your settings may not be saved", t);
        }
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	private Integer _getDeckSlotDeckId(JComboBox selector) {
		Integer deckId = null;
		String deckStr = (String) selector.getItemAt(selector.getSelectedIndex());
		Pattern pattern = Pattern.compile("[^0-9]+([0-9]+)$");
		Matcher matcher = pattern.matcher(deckStr);
		if(matcher.find()) {
			deckId = Integer.parseInt(matcher.group(1));
		}
		return deckId;
	}
	private void _saveDeckSlots() {
		
		try {
			_api.setDeckSlots(
				_getDeckSlotDeckId(_deckSlot1Field),
				_getDeckSlotDeckId(_deckSlot2Field),
				_getDeckSlotDeckId(_deckSlot3Field),
				_getDeckSlotDeckId(_deckSlot4Field),
				_getDeckSlotDeckId(_deckSlot5Field),
				_getDeckSlotDeckId(_deckSlot6Field),
				_getDeckSlotDeckId(_deckSlot7Field),
				_getDeckSlotDeckId(_deckSlot8Field),
				_getDeckSlotDeckId(_deckSlot9Field)
			);
			Main.showMessageDialog(this, _api.getMessage());
			_updateDecksTab();
		} catch (Throwable e) {
			Main.showErrorDialog("Error saving deck slots", e);
		}
	}
	
	private void _saveOptions() {
        debugLog.debug("Saving options...");

		Config.setUserKey(_userKeyField.getText());
		Config.setCheckForUpdates(_checkUpdatesField.isSelected());
		Config.setShowNotifications(_notificationsEnabledField.isSelected());
		Config.setShowHsFoundNotification(_showHsFoundField.isSelected());
		Config.setShowHsClosedNotification(_showHsClosedField.isSelected());
		Config.setShowScreenNotification(_showScreenNotificationField.isSelected());
		Config.setShowModeNotification(_showModeNotificationField.isSelected());
		Config.setShowDeckNotification(_showDeckNotificationField.isSelected());
		Config.setShowYourTurnNotification(_showYourTurnNotificationField.isSelected());
        Config.setShowMatchPopup(Config.MatchPopup.values()[showMatchPopupField.getSelectedIndex()]);
		Config.setAnalyticsEnabled(_analyticsField.isSelected());
		Config.setMinToTray(_minToTrayField.isSelected());
		Config.setStartMinimized(_startMinimizedField.isSelected());

        if (_notificationsFormat != null) {
            // This control only appears on OS X machines, will be null on Windows machines
            Config.setUseOsxNotifications(_notificationsFormat.getSelectedIndex() == 0);
            _notificationQueue = Config.useOsxNotifications() ? new OsxNotificationQueue() : new DialogNotificationQueue();
        }

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
			        if (e.getNewState()==7) {
			            try{
			            	tray.add(trayIcon);
			            	setVisible(false);
			            } catch(AWTException ex){
				        }
		            }
			        if (e.getNewState()==MAXIMIZED_BOTH) {
		                    tray.remove(trayIcon);
		                    setVisible(true);
		                }
		                if (e.getNewState()==NORMAL) {
		                    tray.remove(trayIcon);
		                    setVisible(true);
                            debugLog.debug("Tray icon removed");
		                }
		            }
            	}
       		});
        
    }
    
}
