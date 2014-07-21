package net.hearthstats;

import net.hearthstats.config.*;
import net.hearthstats.log.Log;
import org.apache.commons.lang3.StringUtils;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


@Deprecated
public class OldConfig {

  private final static Logger debugLog = LoggerFactory.getLogger(OldConfig.class);

  private static Wini _ini = null;


  /**
   * Migrates the existing config.ini file, if found, to the new configuration system.
   * The config.ini file is deleted after migration.
   * If there is no config.ini this step is skipped.
   * @param environment
   */
  public static void migrateOldConfig(Environment environment) {
    debugLog.debug("Looking for old config file to migrate...");

    if (getIni() == null) {
      debugLog.debug("Old config file was not found, settings will not be migrated.");
      return;
    }

    debugLog.info("Old config file was found, settings are being migrated now...");

    try {
      Config config = environment.config();

      setIfNotNull(config.configApiBaseUrl(), getApiBaseUrl());
      setIfNotNull(config.configUserKey(), getUserKey());

      setIfNotNull(config.enableAnalytics(), analyticsEnabled());
      setIfNotNull(config.enableDeckOverlay(), showDeckOverlay());
      setIfNotNull(config.enableStartMin(), startMinimized());
      setIfNotNull(config.enableMinToTray(), minimizeToTray());
      setIfNotNull(config.enableUpdateCheck(), checkForUpdates());

      setIfNotNull(config.optionGameLanguage(), gameLanguage());
      setIfNotNull(config.optionMatchPopup(), showMatchPopup());
      setIfNotNull(config.optionMonitoringMethod(), monitoringMethod());
      setIfNotNull(config.optionNotificationType(), notificationType());

      setIfNotNull(config.notifyOverall(), showNotifications());
      setIfNotNull(config.notifyHsFound(), showHsFoundNotification());
      setIfNotNull(config.notifyHsClosed(), showHsClosedNotification());
      setIfNotNull(config.notifyScreen(), showScreenNotification());
      setIfNotNull(config.notifyMode(), showModeNotification());
      setIfNotNull(config.notifyDeck(), showDeckNotification());
      setIfNotNull(config.notifyTurn(), showYourTurnNotification());

      setIfNotNull(config.windowX(), getX());
      setIfNotNull(config.windowY(), getY());
      setIfNotNull(config.windowWidth(), getWidth());
      setIfNotNull(config.windowHeight(), getHeight());

      setIfNotNull(config.deckX(), getDeckX());
      setIfNotNull(config.deckY(), getDeckY());
      setIfNotNull(config.deckWidth(), getDeckWidth());
      setIfNotNull(config.deckHeight(), getDeckHeight());

      File configFile = new File(getConfigPath());
      if (configFile.exists()) {
        debugLog.info("Deleting old config file {}", configFile.getAbsolutePath());
        configFile.delete();
      }

    } catch (Throwable e) {
      throw new RuntimeException("Unable to load your old configuration file. If this problem persists, try deleting your .ini file");
    }

  }

  private static <T> void setIfNotNull(ConfigValue<T> configValue, T oldValue) {
    if (oldValue != null) {
      configValue.set(oldValue);
    }
  }


  private static String getApiBaseUrl() {
    return getStringSetting("API", "baseurl", API.DefaultApiBaseUrl());
  }


  private static String getUserKey() {
    return getStringSetting("API", "userkey", "your_userkey_here");
  }


  private static Integer getX() {
    return getIntegerSetting("ui", "x", 0);
  }


  private static Integer getY() {
    return getIntegerSetting("ui", "y", 0);
  }


  private static Integer getWidth() {
    return getIntegerSetting("ui", "width", 600);
  }


  private static Integer getHeight() {
    return getIntegerSetting("ui", "height", 700);
  }


  private static Integer getDeckX() {
    return getIntegerSetting("ui", "deckx", 0);
  }


  private static Integer getDeckY() {
    return getIntegerSetting("ui", "decky", 0);
  }


  private static Integer getDeckWidth() {
    return getIntegerSetting("ui", "deckwidth", 485);
  }


  private static Integer getDeckHeight() {
    return getIntegerSetting("ui", "deckheight", 600);
  }


  private static Boolean startMinimized() {
    return getBooleanSetting("ui", "startminimized", false);
  }


  private static Boolean analyticsEnabled() {
    return getBooleanSetting("analytics", "enabled", true);
  }


  private static Boolean checkForUpdates() {
    return getBooleanSetting("updates", "check", true);
  }


  private static Boolean showDeckNotification() {
    return getBooleanSetting("notifications", "deck", true);
  }


  private static Boolean showDeckOverlay() {
    return getBooleanSetting("ui", "deckOverlay", false);
    // since feature is still new, do not activate by default
  }


  private static Boolean showScreenNotification() {
    return getBooleanSetting("notifications", "screen", true);
  }


  private static Boolean showHsFoundNotification() {
    return getBooleanSetting("notifications", "hsfound", true);
  }


  private static Boolean showModeNotification() {
    return getBooleanSetting("notifications", "mode", true);
  }


  private static Boolean showYourTurnNotification() {
    return getBooleanSetting("notifications", "yourturn", true);
  }


  private static Boolean showHsClosedNotification() {
    return getBooleanSetting("notifications", "hsclosed", true);
  }


  private static Boolean minimizeToTray() {
    return getBooleanSetting("ui", "mintotray", true);
  }


  private static Boolean useOsxNotifications() {
    try {
      return getBooleanSetting("notifications", "osx", false);
    } catch (Exception e) {
      debugLog.warn("Ignoring exception reading OS X notifications settings, assuming they are disabled", e);
      return false;
    }
  }


  private static NotificationType notificationType() {
    return useOsxNotifications() ? NotificationType.OSX : NotificationType.HEARTHSTATS;
  }


  private static Boolean showNotifications() {
    return getBooleanSetting("notifications", "enabled", true);
  }


  private static MatchPopup showMatchPopup() {
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


  private static MonitoringMethod monitoringMethod() {
    String stringValue = getStringSetting("ui", "monitoringmethod", MonitoringMethod.getDefault().name());
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


  private static GameLanguage gameLanguage() {
    String stringValue = getStringSetting("ui", "gamelanguage", GameLanguage.getDefault().name());
    if (StringUtils.isBlank(stringValue)) {
      return GameLanguage.getDefault();
    } else {
      // Game language 'EU' is being replaced with the more accurate 'EN'
      if ("EU".equals(stringValue)) stringValue = "EN";
      try {
        return GameLanguage.valueOf(stringValue);
      } catch (IllegalArgumentException e) {
        debugLog.debug("Could not parse MonitoringMethod value \"{}\", using default instead", stringValue);
        return GameLanguage.getDefault();
      }
    }
  }


  private static String getConfigPath() {
    if (parseOperatingSystem() == OS.OSX) {
      return getSystemProperty("user.home") + "/Library/Preferences/net.hearthstats.HearthStatsUploader.ini";
    } else {
      return "config.ini";
    }
  }


  private static Wini getIni() {
    if (_ini == null) {
      File configFile = new File(getConfigPath());
      if (configFile.exists()) {
        try {
          _ini = new Wini(configFile);
        } catch (Exception e) {
          Log.warn("Error occurred trying to read settings file, your settings may not be loaded correctly", e);
        }
      }
    }
    return _ini;
  }


  private static Boolean getBooleanSetting(String group, String key, boolean deflt) {
    String setting = getIni().get(group, key);
    return setting == null ? null : setting.equals("true");
  }


  private static Integer getIntegerSetting(String group, String key, int deflt) {
    String setting = getIni().get(group, key);
    return setting == null ? null : Integer.parseInt(setting);
  }


  private static String getStringSetting(String group, String key, String deflt) {
    String setting = getIni().get(group, key);
    return setting;
  }


  private static String getSystemProperty(String property) {
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
   *
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

}
