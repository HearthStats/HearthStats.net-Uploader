package net.hearthstats;

import java.net.URI;
import java.net.URISyntaxException;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import net.miginfocom.swing.MigLayout;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;

@SuppressWarnings("serial")
public class Monitor extends JFrame implements Observer, WindowListener {

	protected API _api = new API();
	protected HearthstoneAnalyzer _analyzer = new HearthstoneAnalyzer();
	protected ProgramHelper _hsHelper = new ProgramHelper("Hearthstone", "Hearthstone.exe");
	protected int _pollingIntervalInMs = 50;
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
	
	public void start() throws IOException {
		if(Config.analyticsEnabled()) {
			_analytics = new JGoogleAnalyticsTracker("HearthStats.net Uploader", Config.getVersion(), "UA-45442103-3");
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
		
		_log("Waiting for Hearthstone (in windowed mode) ...");

	}
	
	private void _showWelcomeLog() {
		_log("<strong>HearthStats.net Uploader v" + Config.getVersion() + "</strong>");
		_log("\nThis is a pre-release. It may have glitches. Your stats will be synced with the live site but most information is only visible on <a href=\"http://BETA.HearthStats.net\">http://BETA.HearthStats.net</a> for the moment.\n");
		_log("1 - <a href=\"http://beta.hearthstats.net/decks/active_decks\">Set your deck slots here</a>");
		_log("2 - Run Hearthstone in <strong>WINDOWED</strong> mode");
		_log("3 - Look for event notifications in this log and bottom right of screen");
		_log("4 - <a href=\"http://goo.gl/lMbdzg\">Submit feedback here</a> (please copy and paste this log)\n");
	}
	
	private boolean _checkForUserKey() {
		if(Config.getUserKey().equals("your_userkey_here")) {
			_log("Userkey yet not entered");
			
			JOptionPane.showMessageDialog(null, 
					"HearthStats.net Uploader Error:\n\n" +
					"You need to enter your User Key\n\n" +
					"Get it at http://beta.hearthstats.net/profiles");
			
			// Create Desktop object
			Desktop d = Desktop.getDesktop();
			// Browse a URL, say google.com
			try {
				d.browse(new URI("http://beta.hearthstats.net/profiles"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String[] options = {"OK", "Cancel"};
			JPanel panel = new JPanel();
			JLabel lbl = new JLabel("User Key");
			JTextField txt = new JTextField(10);
			panel.add(lbl);
			panel.add(txt);
			int selectedOption = JOptionPane.showOptionDialog(null, panel, "Enter your user key", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
			if(selectedOption == 0) {
			    String userkey = txt.getText();
			    if(userkey.isEmpty()) {
			    	_checkForUserKey();
			    } else {
			    	Config.setUserKey(userkey);
			    	Config.save();
			    	_userKeyField.setText(userkey);
			    	_log("Userkey stored");
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
		
		JTabbedPane tabbedPane = new JTabbedPane();
		add(tabbedPane);
		
		// log
		_logText = new JEditorPane();
		_logText.setContentType("text/html");
		_logText.setEditable(false);
		_logText.setText("Event Log:\n");
		_logText.setEditable(false);
		_logText.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
					if (Desktop.isDesktopSupported()) {
						// Create Desktop object
						Desktop d = Desktop.getDesktop();
						// Browse a URL, say google.com
						try {
							d.browse(new URI(e.getURL().toString()));
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (URISyntaxException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		});
		_logScroll = new JScrollPane (_logText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tabbedPane.add(_logScroll, "Main");
		
		tabbedPane.add(_createOptionsUi(), "Options");
		tabbedPane.add(_createAboutUi(), "About");
		
		
		_enableMinimizeToTray();
		
		setVisible(true);
		
		if(Config.startMinimized())
			setState(JFrame.ICONIFIED);
		
		_updateTitle();
	}

	private JPanel _createAboutUi() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		
		MigLayout layout = new MigLayout("");
		panel.setLayout(layout);
		
		JEditorPane text = new JEditorPane();
		text.setContentType("text/html");
		text.setEditable(false);
		text.setBackground(Color.WHITE);
		text.setText("<html><body style=\"font-family:arial,sans-serif; font-size:10px;\">" +
				"<h2 style=\"font-weight:normal\"><a href=\"http://hearthstats.net\">HearthStats.net</a> uploader v" + Config.getVersion() + "</h2>" +
				"<p><strong>Author:</strong> Jerome Dane (<a href=\"https://plus.google.com/+JeromeDane\">Google+</a>, <a href=\"http://twitter.com/JeromeDane\">Twitter</a>)</p>" + 
				"<p>This utility uses screen grab analysis of your Hearthstone window and does not do any packet sniffing, monitoring, or network modification of any kind.</p>" +
				"<p>This project is and always will be open source so that you can do your own builds and see exactly what's happening within the program.</p>" +
				"<p>&bull; <a href=\"https://github.com/JeromeDane/HearthStats.net-Uploader/\">Project source on GitHub</a><br/>" +
				"&bull; <a href=\"https://github.com/JeromeDane/HearthStats.net-Uploader/releases\">Latest releases & changelog</a><br/>" +
				"&bull; <a href=\"https://github.com/JeromeDane/HearthStats.net-Uploader/issues\">Feedback and suggestions</a><br/>" +
				"&bull; <a href=\"http://redd.it/1wa4rc/\">Reddit thread</a> (please up-vote)</p>" +
				"<p>Support this project:</p>" +
				"</body></html>"
			);
		
	    text.addHyperlinkListener(new HyperlinkListener() {
	        @Override
	        public void hyperlinkUpdate(HyperlinkEvent e) {
	            if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
	            	if (Desktop.isDesktopSupported()) {
	            		// Create Desktop object
	        			Desktop d = Desktop.getDesktop();
	        			// Browse a URL, say google.com
	        			try {
	        				d.browse(new URI(e.getURL().toString()));
	        			} catch (IOException e1) {
	        				// TODO Auto-generated catch block
	        				e1.printStackTrace();
	        			} catch (URISyntaxException e1) {
	        				// TODO Auto-generated catch block
	        				e1.printStackTrace();
	        			}
	            	}
	            }
	        }
	    });
	    
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
    			} catch (IOException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			} catch (URISyntaxException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}
	    	}
	    });
	    donateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    panel.add(donateButton);
		return panel;
	}
	private JPanel _createOptionsUi() {
		JPanel panel = new JPanel();
		
		MigLayout layout = new MigLayout();
		panel.setLayout(layout);
		
		panel.add(new JLabel(" "), "wrap");
		
		// user key
		panel.add(new JLabel("User Key: "), "skip,right");
		_userKeyField = new JTextField();
		_userKeyField.setText(Config.getUserKey());
		panel.add(_userKeyField, "wrap");
		
		// check for updates
		panel.add(new JLabel("Updates: "), "skip,right");
		_checkUpdatesField = new JCheckBox("Check for updates when starting the app");
		_checkUpdatesField.setSelected(Config.checkForUpdates());
		panel.add(_checkUpdatesField, "wrap");
		
		// show notifications
		panel.add(new JLabel("Notifications: "), "skip,right");
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
		_showHsFoundField = new JCheckBox("Hearthstone found");
		_showHsFoundField.setSelected(Config.showHsFoundNotification());
		panel.add(_showHsFoundField, "wrap");
		
		// show HS closed notification
		panel.add(new JLabel(""), "skip,right");
		_showHsClosedField = new JCheckBox("Hearthstone closed");
		_showHsClosedField.setSelected(Config.showHsClosedNotification());
		panel.add(_showHsClosedField, "wrap");
		
		// show game screen notification
		panel.add(new JLabel(""), "skip,right");
		_showScreenNotificationField = new JCheckBox("Game screen detection");
		_showScreenNotificationField.setSelected(Config.showScreenNotification());
		panel.add(_showScreenNotificationField, "wrap");
		
		// show game mode notification
		panel.add(new JLabel(""), "skip,right");
		_showModeNotificationField = new JCheckBox("Game mode detection");
		_showModeNotificationField.setSelected(Config.showModeNotification());
		panel.add(_showModeNotificationField, "wrap");
		
		// show deck notification
		panel.add(new JLabel(""), "skip,right");
		_showDeckNotificationField = new JCheckBox("Deck detection");
		_showDeckNotificationField.setSelected(Config.showDeckNotification());
		panel.add(_showDeckNotificationField, "wrap");
		
		_updateNotificationCheckboxes();
		
		// minimize to tray
		panel.add(new JLabel("Interface: "), "skip,right");
		_minToTrayField = new JCheckBox("Minimize to system tray");
		_minToTrayField.setSelected(Config.checkForUpdates());
		panel.add(_minToTrayField, "wrap");
		
		// start minimized
		panel.add(new JLabel(""), "skip,right");
		_startMinimizedField = new JCheckBox("Start minimized");
		_startMinimizedField.setSelected(Config.startMinimized());
		panel.add(_startMinimizedField, "wrap");

		// analytics
		panel.add(new JLabel("Analytics: "), "skip,right");
		_analyticsField = new JCheckBox("Submit anonymous useage stats");
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
		JButton saveOptionsButton = new JButton("Save Options");
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
	
	private void _checkForUpdates() {
		if(Config.checkForUpdates()) {
			_log("Checking for updates ...");
			try {
				String availableVersion = Updater.getAvailableVersion();
				if(availableVersion != null) {
					_log("Latest version available: " + availableVersion);
					
					if(!availableVersion.matches(Config.getVersion())) {
						int dialogButton = JOptionPane.YES_NO_OPTION;
						int dialogResult = JOptionPane.showConfirmDialog(null, 
								"A new version of this uploader is available\n\n" +
								//"v" + Config.getVersion() + " is your current version\n\n" +
								//"v" + availableVersion + " is the latest version\n\n" +
								Updater.getRecentChanges() +
								"\n\nWould you install this update now?"
								,
								"HearthStats.net Uploader Update Available",
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
									"Would you like to disable automatic update checking?",
									"Disable update checking",
									dialogButton);
							if(dialogResult == JOptionPane.YES_OPTION){
								String[] options = {"OK"};
								JPanel panel = new JPanel();
								JLabel lbl = new JLabel("You can re-enable update checking by editing config.ini");
								panel.add(lbl);
								JOptionPane.showOptionDialog(null, panel, "Automatic Update Checking Disabled", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
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

	protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

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

	protected void _notify(String header) {
		_notify(header, "");
	}

	protected void _notify(String header, String message) {
		if (!Config.showNotifications())
			return;	//Notifications disabled
		
		_notificationQueue.add(new net.hearthstats.Notification(header, message));
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

	protected void _updateImageFrame() {
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

	protected void _submitMatchResult() throws IOException {
		HearthstoneMatch hsMatch = new HearthstoneMatch();
		hsMatch.setMode(_analyzer.getMode());
		hsMatch.setUserClass(_analyzer.getYourClass());
		hsMatch.setDeckSlot(_analyzer.getDeckSlot());
		hsMatch.setOpponentClass(_analyzer.getOpponentClass());
		hsMatch.setOpponentName(_analyzer.getOpponentName());
		hsMatch.setCoin(_analyzer.getCoin());
		hsMatch.setRankLevel(_analyzer.getRankLevel());
		hsMatch.setResult(_analyzer.getResult());
		
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
				if(!_analyzer.isAnalyzing())
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
				
				if (_hsHelper.foundProgram())
					_handleHearthstoneFound();
				else
					_handleHearthstoneNotFound();

				_updateTitle();
				
				_pollHearthstone();		// repeat the process
				
				return "";
			}
		}, _pollingIntervalInMs, TimeUnit.MILLISECONDS);
	}

	protected void _handleAnalyzerEvent(Object changed) throws IOException {
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
				_notify("Deck Slot " + _analyzer.getDeckSlot() + " Detected");
				_log("Deck Slot " + _analyzer.getDeckSlot() + " Detected");
				break;
			case "mode":
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
				_notify(_analyzer.getResult() + " Detected");
				_log(_analyzer.getResult() + " Detected");
				_submitMatchResult();
				break;
			case "screen":
				if(_analyzer.getScreen() != "Result" && Config.showScreenNotification()) {
					if(_analyzer.getScreen() == "Practice")
						_notify(_analyzer.getScreen() + " Screen Detected", "Results are not tracked in practice mode");
					else
						_notify(_analyzer.getScreen() + " Screen Detected");
				}
				if(_analyzer.getScreen() == "Practice")
					_log(_analyzer.getScreen() + " Screen Detected. Result tracking disabled.");
				else
					_log(_analyzer.getScreen() + " Screen Detected");
				break;
			case "yourClass":
				_notify("Playing as " + _analyzer.getYourClass());
				_log("Playing as " + _analyzer.getYourClass());
				break;
			default:
				_notify(changed.toString());
				_log(changed.toString());
		}
	}
	
	protected void _clearLog() {
		File file = new File("log.txt");
		file.delete();
	}
	protected void _log(String str) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter("log.txt", true)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println(str);
		out.close();
		
		// read in log
		String logText = "<html><body style=\"font-family:arial,sans-serif; font-size:10px;\">";
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get("log.txt"), Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (String line : lines) {
			logText += line + "<br/>";
        }
		logText += "</body></html>";
		_logText.setText(logText);
		
		_logText.setCaretPosition(_logText.getDocument().getLength());
	}
	
	protected void _handleApiEvent(Object changed) {
		switch(changed.toString()) {
			case "error":
				_notify("API Error", _api.getMessage());
				_log("API Error: " + _api.getMessage());
				break;
			case "result":
				_notify("API Result", _api.getMessage());
				_log("API Result: " + _api.getMessage());
				break;
		}
	}
	
	protected void _handleProgramHelperEvent(Object changed) {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
				_notify("Exception", e.getMessage());
			}
		if(dispatcher.getClass().toString().matches(".*API"))
			_handleApiEvent(changed);
		
		if(dispatcher.getClass().toString().matches(".*ProgramHelper"))
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

	private void _saveOptions() {
		Config.setUserKey(_userKeyField.getText());
		Config.setCheckForUpdates(_checkUpdatesField.isSelected());
		Config.setShowNotifications(_notificationsEnabledField.isSelected());
		Config.setShowHsFoundNotification(_showHsFoundField.isSelected());
		Config.setShowHsClosedNotification(_showHsClosedField.isSelected());
		Config.setShowScreenNotification(_showScreenNotificationField.isSelected());
		Config.setShowModeNotification(_showModeNotificationField.isSelected());
		Config.setShowDeckNotification(_showDeckNotificationField.isSelected());
		Config.setAnalyticsEnabled(_analyticsField.isSelected());
		Config.setMinToTray(_minToTrayField.isSelected());
		Config.setStartMinimized(_startMinimizedField.isSelected());
		Config.save();
		JOptionPane.showMessageDialog(null, "Options Saved");
	}
	
	//http://stackoverflow.com/questions/7461477/how-to-hide-a-jframe-in-system-tray-of-taskbar
	TrayIcon trayIcon;
    SystemTray tray;
    private void _enableMinimizeToTray(){
        System.out.println("creating instance");
        try{
            System.out.println("setting look and feel");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception e){
            System.out.println("Unable to set LookAndFeel");
        }
        if(SystemTray.isSupported()){
        	
            System.out.println("system tray supported");
            tray = SystemTray.getSystemTray();

            ActionListener exitListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Exiting....");
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
	                        System.out.println("added to SystemTray");
	                    } catch (AWTException ex) {
	                        System.out.println("unable to add to tray");
	                    }
	                }
			        if(e.getNewState()==7){
			            try{
			            	tray.add(trayIcon);
			            	setVisible(false);
			            	System.out.println("added to SystemTray");
			            }catch(AWTException ex){
				            System.out.println("unable to add to system tray");
				        }
		            }
			        if(e.getNewState()==MAXIMIZED_BOTH){
		                    tray.remove(trayIcon);
		                    setVisible(true);
		                    System.out.println("Tray icon removed");
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
