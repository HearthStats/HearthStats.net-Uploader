package net.hearthstats;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import net.hearthstats.log.Log;

import org.apache.commons.lang3.StringUtils;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

    private final static Logger debugLog = LoggerFactory.getLogger(Config.class);

    public static final OS os = parseOperatingSystem();

	private static String _version;
	
	private static Wini _ini = null;

	private static String _userkey;

    private static MonitoringMethod monitoringMethod;

	private static boolean _checkForUpdates;

    private static boolean _useOsxNotifications;

	private static boolean _showNotifications;

	private static boolean _showHsFoundNotification;

	private static boolean _showHsClosedNotification;

	private static boolean _showScreenNotification;

	private static boolean _showModeNotification;

	private static boolean _showDeckNotification;

	private static boolean _showDeckOverlay;

    private static MatchPopup showMatchPopup;

	private static boolean _analyticsEnabled;

	private static boolean _minToTray;

	private static boolean _startMinimized;

	private static int _x;

	private static int _y;

	private static int _width;

	private static int _height;

	private static String _defaultApiBaseUrl = "http://hearthstats.net/api/v1/";

	private static String _apiBaseUrl;

	private static ProgramHelper helper;
	
	public static void rebuild() {
        debugLog.debug("Building config");

		storePreviousValues();

		getIni().clear();

		// api
		setUserKey("your_userkey_here");
		setApiBaseUrl(_defaultApiBaseUrl );

        // monitoring method
        setMonitoringMethod(MonitoringMethod.getDefault());
		
		// updates
		setCheckForUpdates(true);
		
		// notifications
        setUseOsxNotifications(isOsxNotificationsSupported());
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
		
		restorePreviousValues();
		
        try {
            save();
        } catch (Throwable e) {
            Log.warn("Error occurred trying to write settings file, your settings may not be saved", e);
        }

	}

	public static String getApiBaseUrl() {
		return getStringSetting("API", "baseurl", _defaultApiBaseUrl);
	}
	private static void setApiBaseUrl(String baseUrl) {
		setStringValue("API", "baseurl", baseUrl);
	}

	public static String getImageCacheFolder() {
		File file = new File("cache/cardimages");
		file.mkdirs();
		return file.getAbsolutePath();
	}

	public static String getUserKey() {
		return getStringSetting("API", "userkey", "your_userkey_here");
	}
	
	public static int getX() {
		return getIntegerSetting("ui", "x", 0);
	}
	
	public static int getY() {
		return getIntegerSetting("ui", "y", 0);
	}
	
	public static int getWidth() {
		return getIntegerSetting("ui", "width", 600);
	}
	
	public static int getHeight() {
		return getIntegerSetting("ui", "height", 700);
	}

	public static boolean startMinimized() {
		return getBooleanSetting("ui", "startminimized", false);
	}
	
	public static boolean analyticsEnabled() {
		return getBooleanSetting("analytics", "enabled", true);
	}
	
	public static boolean showEventLog() {
		return getBooleanSetting("ui", "eventlog", true);
	}
	
	public static boolean mirrorGameImage() {
		return getBooleanSetting("ui", "mirrorgame", false);
	}
	
	public static boolean checkForUpdates() {
		return getBooleanSetting("updates", "check", true);
	}
	
	public static boolean showDeckNotification() {
		return getBooleanSetting("notifications", "deck", true);
	}
	
	public static boolean showDeckOverlay() {
		return getBooleanSetting("ui", "deckOverlay", false);
		// since feature is still new, do not activate by default
	}

	public static boolean showScreenNotification() {
		return getBooleanSetting("notifications", "screen", true);
	}
	
	public static boolean showHsFoundNotification() {
		return getBooleanSetting("notifications", "hsfound", true);
	}
	
	public static boolean showModeNotification() {
		return getBooleanSetting("notifications", "mode", true);
	}
	public static boolean showYourTurnNotification() {
		return getBooleanSetting("notifications", "yourturn", true);
	}
	
	public static boolean showHsClosedNotification() {
		return getBooleanSetting("notifications", "hsclosed", true);
	}
	
	public static boolean minimizeToTray() {
		return getBooleanSetting("ui", "mintotray", true);
	}

    public static boolean useOsxNotifications() {
        try {
            return getBooleanSetting("notifications", "osx", isOsxNotificationsSupported());
        } catch (Exception e) {
            debugLog.warn("Ignoring exception reading OS X notifications settings, assuming they are disabled", e);
            return false;
        }
    }

	public static boolean showNotifications() {
		return getBooleanSetting("notifications", "enabled", true);
	}


    public static MatchPopup showMatchPopup() {
        String stringValue = getStringSetting("ui", "matchpopup", MatchPopup.getDefault().name());
        if (StringUtils.isBlank(stringValue)) {
            return MatchPopup.getDefault();
        } else {
            try {
                return MatchPopup.valueOf(stringValue);
            } catch (IllegalArgumentException e) {
                debugLog.debug("Could not parse matchpopup value \"{}\", using default instead", stringValue);
                return MatchPopup.getDefault();
            }
        }
    }

    public static MonitoringMethod monitoringMethod() {
        String stringValue = getStringSetting("ui", "monitoringmethod", MatchPopup.getDefault().name());
        if (StringUtils.isBlank(stringValue)) {
            return MonitoringMethod.getDefault();
        } else {
            try {
                return MonitoringMethod.valueOf(stringValue);
            } catch (IllegalArgumentException e) {
                debugLog.debug("Could not parse MonitoringMethod value \"{}\", using default instead", stringValue);
                return MonitoringMethod.getDefault();
            }
        }
    }

    public static String getVersion() {
		if(_version == null) {
			_version = "";
			String versionFile = "/version";
//			if(Config.os.toString().equals("OSX")) {
//				versionFile += "-osx";
//			}
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

    public static void setUseOsxNotifications(boolean val) {
        setBooleanValue("notifications", "osx", val);
    }

    public static Boolean isOsxNotificationsSupported() {
        try {
            if (Config.os == OS.OSX) {
                String osVersion = Config.getSystemProperty("os.version");
                String osVersionSplit[] = osVersion.split("\\.");
                if (osVersionSplit[0].equals("10")) {
                    // This is OS X
                    int version = Integer.parseInt(osVersionSplit[1]);
                    if (version >= 8) {
                        // This is OS X 10.8 or later
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            debugLog.warn("Unable to determine if OS X notifications are supported, assuming false", ex);
        }
        return false;
    }

    public static void setShowNotifications(boolean val) {
		setBooleanValue("notifications", "enabled", val);
	}
	
	public static void setAnalyticsEnabled(boolean val) {
		setBooleanValue("analytics", "enabled", val);
	}
	public static void setShowHsFoundNotification(boolean val) {
		setBooleanValue("notifications", "hsfound", val);
	}
	public static void setShowHsClosedNotification(boolean val) {
		setBooleanValue("notifications", "hsclosed", val);
	}
	public static void setShowScreenNotification(boolean val) {
		setBooleanValue("notifications", "screen", val);
	}
	public static void setShowYourTurnNotification(boolean val) {
		setBooleanValue("notifications", "yourturn", val);
	}
	public static void setShowModeNotification(boolean val) {
		setBooleanValue("notifications", "mode", val);
	}
	public static void setShowDeckNotification(boolean val) {
		setBooleanValue("notifications", "deck", val);
	}

	public static void setShowDeckOverlay(boolean val) {
		setBooleanValue("ui", "deckOverlay", val);
	}

    public static void setShowMatchPopup(MatchPopup showMatchPopup) {
        setStringValue("ui", "matchpopup", showMatchPopup == null ? "" : showMatchPopup.name());
    }

    public static void setMonitoringMethod(MonitoringMethod monitoringMethod) {
        setStringValue("ui", "monitoringmethod", monitoringMethod == null ? "" : monitoringMethod.name());
    }

    public static void setCheckForUpdates(boolean val) {
		setBooleanValue("updates", "check", val);
	}
	public static void setMinToTray(boolean val) {
		setBooleanValue("ui", "mintotray", val);
	}
	public static void setStartMinimized(boolean val) {
		setBooleanValue("ui", "startminimized", val);
	}
	
	public static void setUserKey(String userkey) {
		setStringValue("API", "userkey", userkey);
	}
	
	private static void createConfigIniIfNecessary() {
		File configFile = new File(getConfigPath());
		if (!configFile.exists()) {
            if (Config.os == OS.OSX) {
                // The location has moved on Macs, so move the old config.ini to the new location if there is one
                File oldConfigFile = new File("config.ini");
                if (oldConfigFile.exists()) {
                    debugLog.info("Found old config.ini file in {}, moving it to {}", oldConfigFile.getAbsolutePath(), configFile.getAbsolutePath());
                    boolean renameSuccessful = oldConfigFile.renameTo(configFile);
                    if (renameSuccessful) {
                        debugLog.debug("Moved successfully");
                        return;
                    } else {
                        debugLog.warn("Unable to move config.ini file to {}, creating a new file", configFile.getAbsolutePath());
                    }
                }
            }

			try {
				configFile.createNewFile();
			} catch (IOException e) {
                Log.warn("Error occurred while creating config.ini file", e);
			}
		}
	}

    private static String getConfigPath() {
        if (Config.os == OS.OSX) {
            return getSystemProperty("user.home") + "/Library/Preferences/net.hearthstats.HearthStatsUploader.ini";
        } else {
            return "config.ini";
        }
    }
	
	private static void setStringValue(String group, String key, String val) {
		getIni().put(group, key, val);
		try {
			getIni().store();
		} catch (IOException e) {
            Log.warn("Error occurred while setting key " + key + " in config.ini", e);
		}
	}
	
	private static void setBooleanValue(String group, String key, boolean val) {
		getIni().put(group, key, val);
	}
	
	public static void setX(int val) {
		setIntVal("ui", "x", val);
	}
	public static void setY(int val) {
		setIntVal("ui", "y", val);
	}
	public static void setWidth(int val) {
		setIntVal("ui", "width", val);
	}
	public static void setHeight(int val) {
		setIntVal("ui", "height", val);
	}
	
	private static Wini getIni() {
		if(_ini == null) {
			createConfigIniIfNecessary();
			try {
				_ini = new Wini(new File(getConfigPath()));
			} catch (Exception e) {
                Log.warn("Error occurred trying to read settings file, your settings may not be loaded correctly", e);
			}
		}
		return _ini;
	}
	
	private static boolean getBooleanSetting(String group, String key, boolean deflt) {
		String setting = getIni().get(group, key);
		return setting == null ? deflt : setting.equals("true");
	}
	
	private static int getIntegerSetting(String group, String key, int deflt) {
		String setting = getIni().get(group, key);
		return setting == null ? deflt : Integer.parseInt(setting); 
	}
	
	private static String getStringSetting(String group, String key, String deflt) {
		String setting = getIni().get(group, key);
		return setting == null ? deflt : setting;
	}

	private static void restorePreviousValues() {
		setUserKey(_userkey);
		setApiBaseUrl(_apiBaseUrl);
        setMonitoringMethod(monitoringMethod);
		setCheckForUpdates(_checkForUpdates);
        setUseOsxNotifications(_useOsxNotifications);
		setShowNotifications(_showNotifications);
		setShowHsFoundNotification(_showHsFoundNotification);
		setShowHsClosedNotification(_showHsClosedNotification);
		setShowScreenNotification(_showScreenNotification);
		setShowModeNotification(_showModeNotification);
		setShowDeckNotification(_showDeckNotification);
		setShowDeckOverlay(_showDeckOverlay);
        setShowMatchPopup(showMatchPopup);
		setAnalyticsEnabled(_analyticsEnabled);
		setMinToTray(_minToTray);
		setStartMinimized(_startMinimized);
		setX(_x);
		setY(_y);
		setWidth(_width);
		setHeight(_height);
	}
	
	private static void storePreviousValues() {
		_userkey = getUserKey();
		_apiBaseUrl = getApiBaseUrl();
        monitoringMethod = monitoringMethod();
		_checkForUpdates = checkForUpdates();
        _useOsxNotifications = useOsxNotifications();
		_showNotifications = showNotifications();
		_showHsFoundNotification = showHsFoundNotification();
		_showHsClosedNotification = showHsClosedNotification();
		_showScreenNotification = showScreenNotification();
		_showModeNotification = showModeNotification();
		_showDeckNotification = showDeckNotification();
		_showDeckOverlay = showDeckOverlay();
        showMatchPopup = showMatchPopup();
		_analyticsEnabled = analyticsEnabled();
		_minToTray = minimizeToTray();
		_startMinimized = startMinimized();
		_x = getX();
		_y = getY();
		_width = getWidth();
		_height = getHeight();
	}
	
	private static void setIntVal(String group, String key, int val) {
		getIni().put(group, key, val + "");
	}
	
	public static void save() throws IOException {
        getIni().store();
	}

    public static String getJavaLibraryPath() {
        return getSystemProperty("java.library.path");
    }

    public static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // Some system properties may not be available if the user has their security settings locked down
            debugLog.warn("Caught a SecurityException reading the system property '" + property + "', defaulting to blank string.");
            return "";
        }
    }

    /**
     * Parses the os.name system property to determine what operating system we are using.
     * This method is private because you should use the cached version {@link Config#os)} which is faster.
     * @return The current OS
     */
    private static OS parseOperatingSystem() {
        String osString = getSystemProperty("os.name");
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


    public static String getExtractionFolder() {
        if (os == OS.OSX) {
            File libFolder = new File(getSystemProperty("user.home") + "/Library/Application Support/HearthStatsUploader");
            libFolder.mkdir();
            return libFolder.getAbsolutePath();

        } else {
            String path = "tmp";
            (new File(path)).mkdirs();
            return path;
        }
    }

	public static ProgramHelper programHelper() {
		if (helper == null) {
			String className;
			switch (Config.os) {
			case WINDOWS:
				className = "net.hearthstats.win.ProgramHelperWindows";
				break;
			case OSX:
				className = "net.hearthstats.osx.ProgramHelperOsx";
				break;
			default:
				throw new UnsupportedOperationException("unsupported OS");
			}

			try {
				helper = (ProgramHelper) Class.forName(className).newInstance();
			} catch (Exception e) {
				throw new RuntimeException("bug creating " + className, e);
			}
		}
		return helper;
	}

    public static enum OS {
        WINDOWS, OSX, UNSUPPORTED;
    }

    public static enum MatchPopup {
        ALWAYS, INCOMPLETE, NEVER;

        static MatchPopup getDefault() {
            return INCOMPLETE;
        }
    }

    public static enum MonitoringMethod {
        SCREEN, SCREEN_LOG;

        static MonitoringMethod getDefault() {
            return SCREEN;
        }
    }
}
