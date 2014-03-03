package net.hearthstats;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;

import org.json.simple.JSONObject;

import net.miginfocom.swing.MigLayout;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;

@SuppressWarnings("serial")
public class Monitor extends JFrame implements Observer, WindowListener {

	protected int _pollingIntervalInMs = 200;
	protected int _maxThreads = 5;
	protected int _gcFrequency = 8;
	
	protected API _api = new API();
	protected HearthstoneAnalyzer _analyzer = new HearthstoneAnalyzer();
	protected ProgramHelper _hsHelper;
	
	private HyperlinkListener _hyperLinkListener = HyperLinkHandler.getInstance();
	private JTextField _currentOpponentNameField;
	private JLabel _currentMatchLabel;
	private JCheckBox _currentGameCoinField;
	private JTextArea _currentNotesField;
	private JButton _lastMatchButton;
	private HearthstoneMatch _lastMatch;
	private JComboBox _deckSlot1Field;
	private JComboBox _deckSlot2Field;
	private JComboBox _deckSlot3Field;
	private JComboBox _deckSlot4Field;
	private JComboBox _deckSlot5Field;
	private JComboBox _deckSlot6Field;
	private JComboBox _deckSlot7Field;
	private JComboBox _deckSlot8Field;
	private JComboBox _deckSlot9Field;
	private JComboBox _currentOpponentClassSelect;
	private JComboBox _currentYourClassSelector;
	
	private int _pollIterations = 0;
	protected boolean _hearthstoneDetected;
	protected JGoogleAnalyticsTracker _analytics;
	protected JEditorPane _logText;
	private JScrollPane _logScroll;
	private JTextField _userKeyField;
	private JCheckBox _checkUpdatesField;
	private JCheckBox _notificationsEnabledField;
	private JCheckBox _showHsFoundField;
	private JCheckBox _showHsClosedField;
	private JCheckBox _showScreenNotificationField;
	private JCheckBox _showModeNotificationField;
	private JCheckBox _showDeckNotificationField;
	private JCheckBox _analyticsField;
	private JCheckBox _minToTrayField;
	private JCheckBox _startMinimizedField;
	private JCheckBox _showYourTurnNotificationField;
	private JTabbedPane _tabbedPane;
	private ResourceBundle _bundle = ResourceBundle.getBundle("net.hearthstats.resources.Main");
	private String[] _hsClassOptions = { 
			"- " + t("undetected") + " -",
			t("Druid"),
			t("Hunter"),
			t("Mage"),
			t("Paladin"),
			t("Priest"),
			t("Rogue"),
			t("Shaman"),
			t("Warlock"),
			t("Warrior") 
		};

    public Monitor() throws HeadlessException {
    	
        switch (Config.os) {
            case WINDOWS:
                _hsHelper = new ProgramHelperWindows("Hearthstone", "Hearthstone.exe");
                break;
            case OSX:
                _hsHelper = new ProgramHelperOsx("unity.Blizzard Entertainment.Hearthstone");
                break;
            default:
                throw new UnsupportedOperationException(t("error.os_unsupported"));
        }
    }

    private String t(String key) {
    	return _bundle.getString(key);
    }
    
    public void start() throws IOException {
		if(Config.analyticsEnabled()) {
			_analytics = new JGoogleAnalyticsTracker("HearthStats.net " + t("Uploader"), Config.getVersionWithOs(), "UA-45442103-3");
			_analytics.trackAsynchronously(new FocusPoint("AppStart"));
		}
		addWindowListener(this);
		
		_createAndShowGui();
		_clearLog();
		_showWelcomeLog();
		_checkForUpdates();
		
		_api.addObserver(this);
		_analyzer.addObserver(this);
		_hsHelper.addObserver(this);
		
		
		if(_checkForUserKey()) {
			_pollHearthstone();
		}
		
		_log(t("waiting_for_hs_windowed"));
		
	}
	
	private void _showWelcomeLog() {
		_log("<strong>HearthStats.net " + t("Uploader") + " v" + Config.getVersionWithOs() + "</strong>\n");
		_log(t("welcome_1_set_decks"));
        if (Config.os == Config.OS.OSX) {
            _log(t("welcome_2_run_hearthstone"));
            _log(t("welcome_3_notifications"));
        } else {
            _log(t("welcome_2_run_hearthstone_windowed"));
            _log(t("welcome_3_notifications_windowed"));
        }
		_log(t("welcome_4_feedback") + "\n");
	}
	
	private boolean _checkForUserKey() {
		if(Config.getUserKey().equals("your_userkey_here")) {
			_log(t("error.userkey_not_entered"));
			
			JOptionPane.showMessageDialog(null, 
					"HearthStats.net " + t("error.title") + ":\n\n" +
					t("you_need_to_enter_userkey") + "\n\n" +
					t("get_it_at_hsnet_profiles"));
			
			// Create Desktop object
			Desktop d = Desktop.getDesktop();
			// Browse a URL, say google.com
			try {
				d.browse(new URI("http://hearthstats.net/profiles"));
			} catch (IOException e) {
				Main.logException(e);
			} catch (URISyntaxException e) {
				Main.logException(e);
			}
			
			String[] options = {t("button.ok"), t("button.cancel")};
			JPanel panel = new JPanel();
			JLabel lbl = new JLabel(t("UserKey"));
			JTextField txt = new JTextField(10);
			panel.add(lbl);
			panel.add(txt);
			int selectedOption = JOptionPane.showOptionDialog(null, panel, t("enter_your_userkey"), JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
			if(selectedOption == 0) {
			    String userkey = txt.getText();
			    if(userkey.isEmpty()) {
			    	_checkForUserKey();
			    } else {
			    	Config.setUserKey(userkey);
			    	Config.save();
			    	_userKeyField.setText(userkey);
			    	_log(t("UserkeyStored"));
			    	_pollHearthstone();
			    }
			} else {
				System.exit(0);
			}
			return false;
		}
		return true;
	}
	
	private void _createAndShowGui() {
		Image icon = new ImageIcon(getClass().getResource("/images/icon.png")).getImage();
		setIconImage(icon);
		setLocation(Config.getX(), Config.getY());
		setSize(Config.getWidth(), Config.getHeight());
		
		_tabbedPane = new JTabbedPane();
		add(_tabbedPane);
		
		// log
		_logText = new JEditorPane();
		_logText.setContentType("text/html");
		_logText.setEditable(false);
		_logText.setText(t("EventLog") + ":\n");
		_logText.setEditable(false);
		_logText.addHyperlinkListener(_hyperLinkListener);
		_logScroll = new JScrollPane (_logText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_tabbedPane.add(_logScroll, t("tab.log"));
		
		_tabbedPane.add(_createMatchUi(), t("tab.current_match"));
		_tabbedPane.add(_createDecksUi(), t("tab.decks"));
		_tabbedPane.add(_createOptionsUi(), t("tab.options"));
		_tabbedPane.add(_createAboutUi(), t("tab.about"));
		
		_tabbedPane.addChangeListener(new ChangeListener() {
	        public void stateChanged(ChangeEvent e) {
	            if(_tabbedPane.getSelectedIndex() == 2)
					try {
						_updateDecksTab();
					} catch (IOException e1) {
						_notify(t("error.loading_decks.title"), t("error.loading_decks"));
						_log(t("error.loading_decks"));
						Main.logException(e1, false);
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
		text.setText("<html><body style=\"font-family:arial,sans-serif; font-size:10px;\">" +
				"<h2 style=\"font-weight:normal\"><a href=\"http://hearthstats.net\">HearthStats.net</a> " + t("Uploader") + " v" + Config.getVersion() + "</h2>" +
				"<p><strong>" + t("Author") + ":</strong> Jerome Dane (<a href=\"https://plus.google.com/+JeromeDane\">Google+</a>, <a href=\"http://twitter.com/JeromeDane\">Twitter</a>)</p>" + 
				"<p>" + t("about.utility_l1") + "<br>" +
					t("about.utility_l2") + "<br>" +
					t("about.utility_l3") + "</p>" +
				"<p>" + t("about.open_source_l1") + "<br>" +
					t("about.open_source_l2") + "</p>" +
				"<p>&bull; <a href=\"https://github.com/JeromeDane/HearthStats.net-Uploader/\">" + t("about.project_source") + "</a><br/>" +
				"&bull; <a href=\"https://github.com/JeromeDane/HearthStats.net-Uploader/releases\">" + t("about.releases_and_changelog") + "</a><br/>" +
				"&bull; <a href=\"https://github.com/JeromeDane/HearthStats.net-Uploader/issues\">" + t("about.feedback_and_suggestions") + "</a><br/>" +
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
    			} catch (Exception e1) {
    				Main.logException(e1);
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
					"&bull; <a href=\"http://charlesgutjahr.com\">Charles Gutjahr</a> - Added OSx support and memory improvements<br>" +
					"&bull; <a href=\"https://github.com/sargonas\">J Eckert</a> - Fixed notifications spawning taskbar icons<br>" +
					"&bull; <a href=\"https://github.com/nwalsh1995\">nwalsh1995</a> - Started turn detection development<br>" +
					"&bull; <a href=\"https://github.com/remcoros\">Remco Ros</a> (<a href=\"http://hearthstonetracker.com/\">HearthstoneTracker</a>) - Provides advice & suggestins<br>" +
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
		
		// your class
		panel.add(new JLabel(t("match.label.your_class") + " "), "skip,right");
		_currentYourClassSelector = new JComboBox(_hsClassOptions);
		panel.add(_currentYourClassSelector, "wrap");
		
		// opponent class
		panel.add(new JLabel(t("match.label.opponents_class") + " "), "skip,right");
		_currentOpponentClassSelect = new JComboBox(_hsClassOptions);
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
		_currentNotesField.setBorder(BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.black ));
		_currentNotesField.setMinimumSize(new Dimension(350,150));
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
				try {
					String url = _lastMatch.getMode() == "Arena" ? "http://hearthstats.net/arenas/new" : _lastMatch.getEditUrl();
					Desktop.getDesktop().browse(new URI(url));
				} catch (Exception e) {
					Main.logException(e);
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
		
		_deckSlot1Field = new JComboBox();
		panel.add(_deckSlot1Field, "skip"); 
		_deckSlot2Field = new JComboBox();
		panel.add(_deckSlot2Field, ""); 
		_deckSlot3Field = new JComboBox();
		panel.add(_deckSlot3Field, "wrap");
		
		panel.add(new JLabel(" "), "wrap");
		
		panel.add(new JLabel(t("deck_slot.label_4")), "skip"); 
		panel.add(new JLabel(t("deck_slot.label_5")), ""); 
		panel.add(new JLabel(t("deck_slot.label_6")), "wrap");
		
		_deckSlot4Field = new JComboBox();
		panel.add(_deckSlot4Field, "skip"); 
		_deckSlot5Field = new JComboBox();
		panel.add(_deckSlot5Field, ""); 
		_deckSlot6Field = new JComboBox();
		panel.add(_deckSlot6Field, "wrap");
		
		panel.add(new JLabel(" "), "wrap");
		
		panel.add(new JLabel(t("deck_slot.label_7")), "skip"); 
		panel.add(new JLabel(t("deck_slot.label_8")), ""); 
		panel.add(new JLabel(t("deck_slot.label_9")), "wrap");
		
		_deckSlot7Field = new JComboBox();
		panel.add(_deckSlot7Field, "skip"); 
		_deckSlot8Field = new JComboBox();
		panel.add(_deckSlot8Field, ""); 
		_deckSlot9Field = new JComboBox();
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
					Main.logException(e1);
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
					Desktop.getDesktop().browse(new URI("http://hearthstats.net/decks"));
				} catch (Exception e1) {
					Main.logException(e1);
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
		_analyticsField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!_analyticsField.isSelected()) {
					int dialogResult = JOptionPane.showConfirmDialog(null, 
							"A lot of work has gone into this uploader.\n" +
							"It is provided for free, and all we ask in return\n" +
							"is that you let us track basic, anonymous statistics\n" +
							"about how frequently it is being used." +
							"\n\nAre you sure you want to disable analytics?"
							,
							"Please reconsider ...",
							JOptionPane.YES_NO_OPTION);		
					if(dialogResult == JOptionPane.NO_OPTION){
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
		_showHsFoundField.setEnabled(isEnabled);
		_showHsClosedField.setEnabled(isEnabled);
		_showScreenNotificationField.setEnabled(isEnabled);
		_showModeNotificationField.setEnabled(isEnabled);
		_showDeckNotificationField.setEnabled(isEnabled);
	}
	private void _applyDecksToSelector(JComboBox selector, Integer slotNum) {
		
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
			_log(t("checking_for_updates..."));
			try {
				String availableVersion = Updater.getAvailableVersion();
				if(availableVersion != null) {
					_log(t("latest_v_availabie") + " " + availableVersion);
					
					if(!availableVersion.matches(Config.getVersion())) {
						int dialogButton = JOptionPane.YES_NO_OPTION;
						int dialogResult = JOptionPane.showConfirmDialog(null, 
								"A new version of this uploader is available\n\n" +
								Updater.getRecentChanges() +
								"\n\n" + t("would_u_like_to_install_update")
								,
								"HearthStats.net " + t("uploader_updates_avail"),
								dialogButton);		
						if(dialogResult == JOptionPane.YES_OPTION){
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
								JOptionPane.showOptionDialog(null, panel, t("updates_disabled_msg"), JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
								Config.setCheckForUpdates(false);
							}
						}
					}
				} else {
					_log("Unable to determine latest available version");
				}
			} catch(Exception e) {
				_notify("Update Checking Error", "Unable to determine the latest available version");
			}
		}
	}

	private int _numThreads = 0;
	protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(_maxThreads);

	protected boolean _drawPaneAdded = false;

	protected BufferedImage image;

	protected JPanel _drawPane = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	};

	protected NotificationQueue _notificationQueue = new NotificationQueue();
	private Boolean _currentMatchEnabled = false;
	private boolean _playingInMatch = false;

	protected void _notify(String header) {
		_notify(header, "");
	}

	protected void _notify(String header, String message) {
		if (!Config.showNotifications())
			return;	//Notifications disabled
		
		_notificationQueue.add(new net.hearthstats.Notification(header, message, false));
	}

	protected void _updateTitle() {
		 String title = "HearthStats.net Uploader";
		if (_hearthstoneDetected) {
			if (_analyzer.getScreen() != null) {
				title += " - " + _analyzer.getScreen();
				if (_analyzer.getScreen() == "Play" && _analyzer.getMode() != null) {
					title += " " + _analyzer.getMode();
				}
				if (_analyzer.getScreen() == "Finding Opponent") {
					if (_analyzer.getMode() != null) {
						title += " for " + _analyzer.getMode() + " Game";
					}
				}
				if (_analyzer.getScreen() == "Match Start" || _analyzer.getScreen() == "Playing") {
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
		for(int i = 0; i < _hsClassOptions.length; i++) {
			if(_hsClassOptions[i] == cName)
				return i;
		}
		return 0;
	}
	private void _updateCurrentMatchUi() {
		_updateMatchClassSelectorsIfSet();
		HearthstoneMatch match = _analyzer.getMatch();
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

	private void _submitMatchResult() throws IOException {
		HearthstoneMatch hsMatch = _analyzer.getMatch();
		
		_updateMatchClassSelectorsIfSet();
		
		// check for new arena run
		if(hsMatch.getMode() == "Arena" && _analyzer.isNewArena()) {
			ArenaRun run = new ArenaRun();
			run.setUserClass(hsMatch.getUserClass());
			_log("Creating new " + run.getUserClass() + "arena run");
			_notify("Creating new " + run.getUserClass() + "arena run");
			_api.createArenaRun(run);
			_analyzer.setIsNewArena(false);
		}
		
		String header = "Submitting match result";
		String message = hsMatch.toString(); 
		_notify(header, message);
		_log(header + ": " + message);

		if(Config.analyticsEnabled()) {
			_analytics.trackAsynchronously(new FocusPoint("Submit" + hsMatch.getMode() + "Match"));
		}
		
		_api.createMatch(hsMatch);
	}
	
	private void _resetMatchClassSelectors() {
		_currentYourClassSelector.setSelectedIndex(0);	
		_currentOpponentClassSelect.setSelectedIndex(0);	
	}
	private void _updateMatchClassSelectorsIfSet() {
		if(_currentYourClassSelector.getSelectedIndex() > 0)
			_analyzer.getMatch().setUserClass(_currentYourClassSelector.getSelectedItem().toString());
		if(_currentOpponentClassSelect.getSelectedIndex() > 0)
			_analyzer.getMatch().setOpponentClass(_currentOpponentClassSelect.getSelectedItem().toString());
	}

	protected void _handleHearthstoneFound() {
		
		// mark hearthstone found if necessary
		if (!_hearthstoneDetected) {
			_hearthstoneDetected = true;
			if(Config.showHsFoundNotification())
				_notify("Hearthstone found");
		}
		
		// grab the image from Hearthstone
		image = _hsHelper.getScreenCapture();
		
		if(image != null) {
			// detect image stats 
			if (image.getWidth() >= 1024)
				_analyzer.analyze(image);
			
			if(Config.mirrorGameImage())
				_updateImageFrame();
		}
	}
	
	protected void _handleHearthstoneNotFound() {
		
		// mark hearthstone not found if necessary
		if (_hearthstoneDetected) {
			_hearthstoneDetected = false;
			if(Config.showHsClosedNotification()) {
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
				
				if (_hsHelper.foundProgram())
					_handleHearthstoneFound();
				else
					_handleHearthstoneNotFound();

				_updateTitle();
				
				_pollHearthstone();		// repeat the process

                // Keep memory usage down by telling the JVM to perform a garbage collection after every fifth poll (ie GC 1-2 times per second)
				if (_pollIterations % _gcFrequency == 0 && Runtime.getRuntime().totalMemory() > 150000000) {
                    System.gc();
                }
                _numThreads--;
				return "";
			}
		}, _pollingIntervalInMs, TimeUnit.MILLISECONDS);
	}
	
	private void _promptForResult() {
		Object[] options = { "Victory", "Defeat", "Draw" };
		String message = "The result of your previous match was not detected.\n\n" +
				"This often happens if you click away the result banner\n" +
				"in the game before giving the uploader a second or two\n" +
				"to recognize the victory and defeat banners.\n\n" +
				"What was the result of your last match?";
		String title = "Match Result Not Detected";
		int dialogResult = JOptionPane.showOptionDialog(null, message, title,
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, 
				null, options, null);
		if(dialogResult == JOptionPane.YES_OPTION){
			_analyzer.getMatch().setResult("Victory");
		} else if(dialogResult == JOptionPane.NO_OPTION){
			_analyzer.getMatch().setResult("Defeat");
		} else {
			_analyzer.getMatch().setResult("Draw");
		}
		try {
			_submitMatchResult();
		} catch (IOException e) {
			Main.logException(e);
		}
	}

	private void _handleAnalyzerEvent(Object changed) throws IOException {
		switch(changed.toString()) {
			case "arenaEnd":
				_notify("End of Arena Run Detected");
				_log("End of Arena Run Detected");
				_api.endCurrentArenaRun();
				break;
			case "coin":
				_notify("Coin Detected");
				_log("Coin Detected");
				break;
			case "deckSlot":
				JSONObject deck = DeckSlotUtils.getDeckFromSlot(_analyzer.getDeckSlot());
				if(deck == null) {
					_tabbedPane.setSelectedIndex(2);
					Main.showMessageDialog("Unable to determine what deck you have in slot #" + _analyzer.getDeckSlot() + "\n\nPlease set your decks in the \"Decks\" tab.");
				} else {
					_notify("Deck Detected", deck.get("name").toString());
					_log("Deck Detected: " + deck.get("name") + " Detected");
				}
				
				break;
			case "mode":
				_playingInMatch = false;
				_setCurrentMatchEnabledi(false);
				if(Config.showModeNotification()) {
					System.out.println(_analyzer.getMode() + " level " + _analyzer.getRankLevel());
					if(_analyzer.getMode() == "Ranked")
						_notify(_analyzer.getMode() + " Mode Detected", "Rank Level " + _analyzer.getRankLevel());
					else
						_notify(_analyzer.getMode() + " Mode Detected");
				}
				if(_analyzer.getMode() == "Ranked")
					_log(_analyzer.getMode() + " Mode Detected - Level " + _analyzer.getRankLevel());
				else
					_log(_analyzer.getMode() + " Mode Detected");
				break;
			case "newArena":
				if(_analyzer.isNewArena())
					_notify("New Arena Run Detected");
				_log("New Arena Run Detected");
				break;
			case "opponentClass":
				_notify("Playing vs " + _analyzer.getOpponentClass());
				_log("Playing vs " + _analyzer.getOpponentClass());
				break;
			case "opponentName":
				_notify("Opponent: " + _analyzer.getOpponentName());
				_log("Opponent: " + _analyzer.getOpponentName());
				break;
			case "result":
				_playingInMatch = false;
				_setCurrentMatchEnabledi(false);
				_notify(_analyzer.getResult() + " Detected");
				_log(_analyzer.getResult() + " Detected");
				_submitMatchResult();
				break;
			case "screen":
				
				boolean inGameModeScreen = (_analyzer.getScreen() == "Arena" || _analyzer.getScreen() == "Play");
				if(inGameModeScreen) {
					if(_playingInMatch &&  _analyzer.getResult() == null) {
						_playingInMatch = false;
						_notify("Detection Error", "Match result was not detected.");
						_log("Detection Error: Match result was not detected.");
						_promptForResult();
					}
					_playingInMatch = false;
				} 
				
				if(_analyzer.getScreen() == "Finding Opponent") {
					_resetMatchClassSelectors();
				}
				if(_analyzer.getScreen() == "Match Start") {
					_setCurrentMatchEnabledi(true);
					_playingInMatch = true;
				} if(_analyzer.getScreen() != "Result" && Config.showScreenNotification()) {
					if(_analyzer.getScreen() == "Practice")
						_notify(_analyzer.getScreen() + " Screen Detected", "Results are not tracked in practice mode");
					else
						_notify(_analyzer.getScreen() + " Screen Detected");
				}
				if(_analyzer.getScreen() == "Practice")
					_log(_analyzer.getScreen() + " Screen Detected. Result tracking disabled.");
				else {
					if(_analyzer.getScreen() == "Match Start")
						_log("\n------------------------------------------");
					_log(_analyzer.getScreen() + " Screen Detected");
				}
				break;
			case "yourClass":
				_notify("Playing as " + _analyzer.getYourClass());
				_log("Playing as " + _analyzer.getYourClass());
				break;
			case "yourTurn":
				if(Config.showYourTurnNotification())
					_notify((_analyzer.isYourTurn() ? "Your" : "Opponent") + " turn detected");
				_log((_analyzer.isYourTurn() ? "Your" : "Opponent") + " turn detected");
				break;
			default:
				_notify(changed.toString());
				_log(changed.toString());
		}
		_updateCurrentMatchUi();
	}
	
	private void _clearLog() {
		File file = new File("log.txt");
		file.delete();
	}
	private void _log(String str) {
		
		Main.log(str);
		
		// read in log
		String logText = "<html><body style=\"font-family:arial,sans-serif; font-size:10px;\">";
		logText += Main.getLogText().replaceAll("\n", "<br>");
		logText += "</body></html>";
		_logText.setText(logText);
		
		_logText.setCaretPosition(_logText.getDocument().getLength());
	}
	
	private void _handleApiEvent(Object changed) {
		switch(changed.toString()) {
			case "error":
				_notify("API Error", _api.getMessage());
				_log("API Error: " + _api.getMessage());
				Main.showMessageDialog("API Error: " + _api.getMessage());
				break;
			case "result":
				_log("API Result: " + _api.getMessage());
				_lastMatch = _analyzer.getMatch();
				_lastMatch.setId(_api.getLastMatchId());
				_setCurrentMatchEnabledi(false);
				_updateCurrentMatchUi();
				// new line after match result
				if(_api.getMessage().matches(".*(Edit match|Arena match successfully created).*")) {
					_analyzer.resetMatch();
					_resetMatchClassSelectors();
					_log("------------------------------------------\n");
				}
				break;
		}
	}
	
	private void _handleProgramHelperEvent(Object changed) {
		_log(changed.toString());
		if(changed.toString().matches(".*minimized.*")) 
			_notify("Hearthstone Minimized", "Warning! No detection possible while minimized.");
		if(changed.toString().matches(".*fullscreen.*")) 
			JOptionPane.showMessageDialog(null, "Hearthstats.net Uploader Warning! \n\nNo detection possible while Hearthstone is in fullscreen mode.\n\nPlease set Hearthstone to WINDOWED mode and close and RESTART Hearthstone.\n\nSorry for the inconvenience.");
		if(changed.toString().matches(".*restored.*")) 
			_notify("Hearthstone Restored", "Resuming detection ...");
	}
	
	@Override
	public void update(Observable dispatcher, Object changed) {
		if(dispatcher.getClass().toString().matches(".*HearthstoneAnalyzer"))
			try {
				_handleAnalyzerEvent(changed);
			} catch (IOException e) {
				Main.logException(e);
			}
		if(dispatcher.getClass().toString().matches(".*API"))
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
		System.out.println("closed");
	}

	@Override
	public void windowClosing(WindowEvent e) {
		Point p = getLocationOnScreen();
		Config.setX(p.x);
		Config.setY(p.y);
		Dimension rect = getSize();
		Config.setWidth((int) rect.getWidth());
		Config.setHeight((int) rect.getHeight());
		Config.save();
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
			Main.showMessageDialog(_api.getMessage());
			_updateDecksTab();
		} catch (Exception e) {
			Main.logException(e);
		}
	}
	
	private void _saveOptions() {
		Config.setUserKey(_userKeyField.getText());
		Config.setCheckForUpdates(_checkUpdatesField.isSelected());
		Config.setShowNotifications(_notificationsEnabledField.isSelected());
		Config.setShowHsFoundNotification(_showHsFoundField.isSelected());
		Config.setShowHsClosedNotification(_showHsClosedField.isSelected());
		Config.setShowScreenNotification(_showScreenNotificationField.isSelected());
		Config.setShowModeNotification(_showModeNotificationField.isSelected());
		Config.setShowDeckNotification(_showDeckNotificationField.isSelected());
		Config.setShowYourTurnNotification(_showYourTurnNotificationField.isSelected());
		Config.setAnalyticsEnabled(_analyticsField.isSelected());
		Config.setMinToTray(_minToTrayField.isSelected());
		Config.setStartMinimized(_startMinimizedField.isSelected());
		Config.save();
		JOptionPane.showMessageDialog(null, "Options Saved");
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
        }else{
            System.out.println("system tray not supported");
        }
        addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
            	if(Config.minimizeToTray()) {
	                if(e.getNewState() == ICONIFIED){
	                    try {
	                        tray.add(trayIcon);
	                        setVisible(false);
	                    } catch (AWTException ex) {
	                    }
	                }
			        if(e.getNewState()==7){
			            try{
			            	tray.add(trayIcon);
			            	setVisible(false);
			            }catch(AWTException ex){
				        }
		            }
			        if(e.getNewState()==MAXIMIZED_BOTH){
		                    tray.remove(trayIcon);
		                    setVisible(true);
		                }
		                if(e.getNewState()==NORMAL){
		                    tray.remove(trayIcon);
		                    setVisible(true);
		                    System.out.println("Tray icon removed");
		                }
		            }
            	}
       		});
        
    }
    
}
