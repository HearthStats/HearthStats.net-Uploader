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

	private static String _version;
	
	private static Wini _ini = null;
	
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
			
			InputStream in = Config.class.getResourceAsStream("/version");
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

	public static void setCheckForUpdates(boolean val) {
		_setBooleanValue("updates", "check", val);
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
	
	private static void _setBooleanValue(String group, String key, boolean val) {
		_getIni().put(group, key, val);
		_save(group, key, val);
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
			try {
				
				// check for build/config.ini (dev environment)
				File buildConfigFile = new File("build/config.ini");
				
				_ini = new Wini(buildConfigFile.exists() ? buildConfigFile : new File("config.ini"));
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
	
	private static void _setIntVal(String group, String key, int val) {
		_getIni().put(group, key, val + "");
		_save(group, key, val);
	}
	
	private static void _save(String group, String key, Object val) {
		try {
			_getIni().store();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to write to config.ini while trying to save " + 
					group + "." + key + " = " + val + "\n\n" + e.toString()
					);
		}
	}
}
