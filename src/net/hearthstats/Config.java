package net.hearthstats;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

public class Config {

    public static final OS os = _parseOperatingSystem();

	private static String _version;
	
	private static Wini _ini = null;

	private static String _userkey;

	private static boolean _checkForUpdates;

	private static boolean _showNotifications;

	private static boolean _showHsFoundNotification;

	private static boolean _showHsClosedNotification;

	private static boolean _showScreenNotification;

	private static boolean _showModeNotification;

	private static boolean _showDeckNotification;

	private static boolean _analyticsEnabled;

	private static boolean _minToTray;

	private static boolean _startMinimized;

	private static int _x;

	private static int _y;

	private static int _width;

	private static int _height;
	
	public static void rebuild() {

		_storePreviousValues();

		_getIni().clear();

		// api
		setUserKey("your_userkey_here");
		
		// updates
		setCheckForUpdates(true);
		
		// notifications
		setShowNotifications(true);
		setShowHsFoundNotification(true);
		setShowHsClosedNotification(true);
		setShowScreenNotification(true);
		setShowModeNotification(true);
		setShowDeckNotification(true);
		setShowYourTurnNotification(true);
		
		// analytics
		setAnalyticsEnabled(true);
		
		// ui
		setMinToTray(true);
		setStartMinimized(false);
		setX(0);
		setY(0);
		setWidth(600);
		setHeight(700);
		
		_restorePreviousValues();
		
		save();
		
	}

	public static String getUserKey() {
		return _getStringSetting("API", "userkey", "your_userkey_here");
	}
	
	public static int getX() {
		return _getIntegerSetting("ui", "x", 0);
	}
	
	public static int getY() {
		return _getIntegerSetting("ui", "y", 0);
	}
	
	public static int getWidth() {
		return _getIntegerSetting("ui", "width", 600);
	}
	
	public static int getHeight() {
		return _getIntegerSetting("ui", "height", 700);
	}

	public static boolean startMinimized() {
		return _getBooleanSetting("ui", "startminimized", false);
	}
	
	public static boolean analyticsEnabled() {
		return _getBooleanSetting("analytics", "enabled", true);
	}
	
	public static boolean showEventLog() {
		return _getBooleanSetting("ui", "eventlog", true);
	}
	
	public static boolean mirrorGameImage() {
		return _getBooleanSetting("ui", "mirrorgame", false);
	}
	
	public static boolean checkForUpdates() {
		return _getBooleanSetting("updates", "check", true);
	}
	
	public static boolean showDeckNotification() {
		return _getBooleanSetting("notifications", "deck", true);
	}
	
	public static boolean showScreenNotification() {
		return _getBooleanSetting("notifications", "screen", true);
	}
	
	public static boolean showHsFoundNotification() {
		return _getBooleanSetting("notifications", "hsfound", true);
	}
	
	public static boolean showModeNotification() {
		return _getBooleanSetting("notifications", "mode", true);
	}
	public static boolean showYourTurnNotification() {
		return _getBooleanSetting("notifications", "yourturn", true);
	}
	
	public static boolean showHsClosedNotification() {
		return _getBooleanSetting("notifications", "hsclosed", true);
	}
	
	public static boolean minimizeToTray() {
		return _getBooleanSetting("ui", "mintotray", true);
	}
	
	public static boolean showNotifications() {
		return _getBooleanSetting("notifications", "enabled", true);
	}
	
	public static String getVersion() {
		if(_version == null) {
			_version = "";
			String versionFile = "/version";
			if(Config.os.toString().equals("OSX")) {
				versionFile += "-osx";
			}
			InputStream in = Config.class.getResourceAsStream(versionFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            try {
				while ((strLine = br.readLine()) != null)   {
				    _version += strLine;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Exception in Config: " + e.toString());
			}
		}
		return _version;
	}
	public static String getVersionWithOs() {
		return getVersion() + '-' + os;
	}

	public static void setShowNotifications(boolean val) {
		_setBooleanValue("notifications", "enabled", val);
	}
	
	public static void setAnalyticsEnabled(boolean val) {
		_setBooleanValue("analytics", "enabled", val);
	}
	public static void setShowHsFoundNotification(boolean val) {
		_setBooleanValue("notifications", "hsfound", val);
	}
	public static void setShowHsClosedNotification(boolean val) {
		_setBooleanValue("notifications", "hsclosed", val);
	}
	public static void setShowScreenNotification(boolean val) {
		_setBooleanValue("notifications", "screen", val);
	}
	public static void setShowYourTurnNotification(boolean val) {
		_setBooleanValue("notifications", "yourturn", val);
	}
	public static void setShowModeNotification(boolean val) {
		_setBooleanValue("notifications", "mode", val);
	}
	public static void setShowDeckNotification(boolean val) {
		_setBooleanValue("notifications", "deck", val);
	}
	
	public static void setCheckForUpdates(boolean val) {
		_setBooleanValue("updates", "check", val);
	}
	public static void setMinToTray(boolean val) {
		_setBooleanValue("ui", "mintotray", val);
	}
	public static void setStartMinimized(boolean val) {
		_setBooleanValue("ui", "startminimized", val);
	}
	
	public static void setUserKey(String userkey) {
		// TODO Auto-generated method stub
		_getIni().put("API", "userkey", userkey);
		try {
			_getIni().store();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Exception in Config: " + e.toString());
		}
	}
	
	private static void _createConfigIniIfNecessary() {
		File configFile = new File("config.ini");
		if(!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Unable to create config.ini");
				System.exit(1);
			}
		}
	}
	
	private static void _setBooleanValue(String group, String key, boolean val) {
		_getIni().put(group, key, val);
	}
	
	public static void setX(int val) {
		_setIntVal("ui", "x", val);
	}
	public static void setY(int val) {
		_setIntVal("ui", "y", val);
	}
	public static void setWidth(int val) {
		_setIntVal("ui", "width", val);
	}
	public static void setHeight(int val) {
		_setIntVal("ui", "height", val);
	}
	
	private static Wini _getIni() {
		if(_ini == null) {
			_createConfigIniIfNecessary();
			try {
				_ini = new Wini(new File("config.ini"));
			} catch (InvalidFileFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Exception in Config: " + e.toString());
			}
		}
		return _ini;
	}
	
	private static boolean _getBooleanSetting(String group, String key, boolean deflt) {
		String setting = _getIni().get(group, key);
		return setting == null ? deflt : setting.equals("true");
	}
	
	private static int _getIntegerSetting(String group, String key, int deflt) {
		String setting = _getIni().get(group, key);
		return setting == null ? deflt : Integer.parseInt(setting); 
	}
	
	private static String _getStringSetting(String group, String key, String deflt) {
		String setting = _getIni().get(group, key);
		return setting == null ? deflt : setting;
	}
	
	private static void _restorePreviousValues() {
		setUserKey(_userkey);
		setCheckForUpdates(_checkForUpdates);
		setShowNotifications(_showNotifications);
		setShowHsFoundNotification(_showHsFoundNotification);
		setShowHsClosedNotification(_showHsClosedNotification);
		setShowScreenNotification(_showScreenNotification);
		setShowModeNotification(_showModeNotification);
		setShowDeckNotification(_showDeckNotification);
		setAnalyticsEnabled(_analyticsEnabled);
		setMinToTray(_minToTray);
		setStartMinimized(_startMinimized);
		setX(_x);
		setY(_y);
		setWidth(_width);
		setHeight(_height);
	}
	
	private static void _storePreviousValues() {
		_userkey = getUserKey();
		_checkForUpdates = checkForUpdates();
		_showNotifications = showNotifications();
		_showHsFoundNotification = showHsFoundNotification();
		_showHsClosedNotification = showHsClosedNotification();
		_showScreenNotification = showScreenNotification();
		_showModeNotification = showModeNotification();
		_showDeckNotification = showDeckNotification();
		_analyticsEnabled = analyticsEnabled();
		_minToTray = minimizeToTray();
		_startMinimized = startMinimized();
		_x = getX();
		_y = getY();
		_width = getWidth();
		_height = getHeight();
	}
	
	private static void _setIntVal(String group, String key, int val) {
		_getIni().put(group, key, val + "");
	}
	
	public static void save() {
		try {
			_getIni().store();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to write to config.ini while trying to save settings");
		}
	}

    private static String _getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // Some system properties may not be available if the user has their security settings locked down
            System.err.println("Caught a SecurityException reading the system property '" + property + "', defaulting to blank string.");
            return "";
        }
    }

    /**
     * Parses the os.name system property to determine what operating system we are using.
     * This method is private because you should use the cached version {@link Config#os)} which is faster.
     * @return The current OS
     */
    private static OS _parseOperatingSystem() {
        String osString = _getSystemProperty("os.name");
        if (osString == null) {
            return OS.UNSUPPORTED;
        } else if (osString.startsWith("Windows")) {
            return OS.WINDOWS;
        } else if (osString.startsWith("Mac OS X")) {
            return OS.OSX;
        } else {
            return OS.UNSUPPORTED;
        }
    }

    public static enum OS {
        WINDOWS, OSX, UNSUPPORTED;
    }
}
