package net.hearthstats;

import java.net.URI;
import java.net.URISyntaxException;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
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

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;
import com.sun.org.apache.bcel.internal.generic.NEWARRAY;

@SuppressWarnings("serial")
public class Monitor extends JFrame implements Observer {

	protected API _api = new API();
	protected HearthstoneAnalyzer _analyzer = new HearthstoneAnalyzer();
	protected ProgramHelper _hsHelper = new ProgramHelper("Hearthstone", "Hearthstone.exe");
	protected int _pollingIntervalInMs = 100;
	protected boolean _hearthstoneDetected;
	protected JGoogleAnalyticsTracker _analytics;
	protected JTextPane _logText;
	private JScrollPane _logScroll;
	
	public void start() throws IOException {
		
		_clearLog();
		if(Config.analyticsEnabled()) {
			_analytics = new JGoogleAnalyticsTracker("HearthStats.net Uploader", Config.getVersion(), "UA-45442103-3");
			_analytics.trackAsynchronously(new FocusPoint("AppStart"));
		}
		
		_createAndShowGui();
		_log("Starting HearthStats.net Uploader v" + Config.getVersion());
		_checkForUpdates();

		
		_api.addObserver(this);
		_analyzer.addObserver(this);
		_hsHelper.addObserver(this);
		
		if(_checkForUserKey()) {
			_pollHearthstone();
		}

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
	
	private void _getFileContents(String path) {
		
	}
	private void _createAndShowGui() {
		Image icon = new ImageIcon("images/icon.png").getImage();
		
		f.setIconImage(icon);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setLocation(20, 20);
		f.setSize(600, 700);
		
		JLabel statusLabel = new JLabel("Status", JLabel.LEFT);
		f.getContentPane().add(statusLabel);
		
		// log
		_logText = new JTextPane ();
		_logText.setText("Event Log:\n");
		_logText.setEditable(false);
		_logScroll = new JScrollPane (_logText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		f.getContentPane().add(_logScroll);
		
		f.setVisible(true);
		_updateTitle();
	}

	private void _checkForUpdates() {
		if(Config.checkForUpdates()) {
			_log("Checking for updates (installed version: " + Config.getVersion() + ")");
			try {
				URL url = new URL("https://raw.github.com/JeromeDane/HearthStats.net-Uploader/master/version");
				BufferedReader reader = null;
				String availableVersion = "";
				try {
				    reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
				    
				    for (String line; (line = reader.readLine()) != null;) {
				    	availableVersion += line;
				    }
				    _log("Latest version available: " + availableVersion);
				    
				} finally {
				    if (reader != null) try { reader.close(); } catch (IOException e) {
				    	_notify("Exception", e.getMessage());
				    	_log("Exception: " + e.getMessage());
				    }
				}
				if(!availableVersion.matches(Config.getVersion())) {
					if(Config.alertUpdates()) {
						int dialogButton = JOptionPane.YES_NO_OPTION;
						int dialogResult = JOptionPane.showConfirmDialog(null, 
								"A new version of HearthStats.net is available\n\n" +
								"v" + Config.getVersion() + " is your current version\n" +
								"v" + availableVersion + " is the latest version\n\n" +
								"Would you exit and go to the download page?\n",
								"HearthStats.net Uploader Update Available",
								dialogButton);		
						if(dialogResult == JOptionPane.YES_OPTION){
							// Create Desktop object
							Desktop d = Desktop.getDesktop();
							// Browse a URL, say google.com
							d.browse(new URI("https://github.com/JeromeDane/HearthStats.net-Uploader/releases"));
							System.exit(0);
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

				}
			} catch(Exception e) {
				_notify("Update Checking Error", "Unable to determine the latest available version");
			}
		}
	}

	protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

	protected JFrame f = new JFrame();

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
		f.setTitle(title);
	}

	protected void _updateImageFrame() {
		if (!_drawPaneAdded) {
			f.add(_drawPane);
		}
		if (image.getWidth() >= 1024) {
			f.setSize(image.getWidth(), image.getHeight());
		}
		_drawPane.repaint();
		f.invalidate();
		f.validate();
		f.repaint();
	}

	protected void _submitMatchResult() throws IOException {
		HearthstoneMatch hsMatch = new HearthstoneMatch();
		hsMatch.setMode(_analyzer.getMode());
		hsMatch.setUserClass(_analyzer.getYourClass());
		hsMatch.setDeckSlot(_analyzer.getDeckSlot());
		hsMatch.setOpponentClass(_analyzer.getOpponentClass());
		hsMatch.setCoin(_analyzer.getCoin());
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
		if (_hearthstoneDetected != true) {
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
			if(Config.showHsClosedNotification())
				_notify("Hearthstone closed");
			
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
				if(Config.showModeNotification())
					_notify(_analyzer.getMode() + " Mode Detected");
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
			case "result":
				_notify(_analyzer.getResult() + " Detected");
				_log(_analyzer.getResult() + " Detected");
				_submitMatchResult();
				break;
			case "screen":
				if(_analyzer.getScreen() != "Result" && Config.showScreenNotification())
					_notify(_analyzer.getScreen() + " Screen Detected");
				_log(_analyzer.getScreen() + " Screen Detected");
				break;
			case "yourClass":
				_notify("Playing as " + _analyzer.getYourClass());
				_log("Playing as " + _analyzer.getYourClass());
				break;
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
		String logText = "";
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get("log.txt"), Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (String line : lines) {
			logText += line + "\n";
        }
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
			_log((String) changed);
	}


}
