package net.hearthstats.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public final class Translations {
  private Translations() {
  } // singleton

  private static ResourceBundle _bundle = ResourceBundle.getBundle("net.hearthstats.resources.Main");

  /**
   * Loads text from the main resource bundle, using the local language when
   * available.
   * 
   * @param key
   *          the key for the desired string
   * @return The requested string
   */
  public static String t(String key) {
    return _bundle.getString(key);
  }

  /**
   * Loads text from the main resource bundle, using the local language when
   * available, and puts the given value into the appropriate spot.
   * 
   * @param key
   *          the key for the desired string
   * @param value0
   *          a value to place in the {0} placeholder in the string
   * @return The requested string
   */
  public static String t(String key, Object value0) {
    String message = _bundle.getString(key);
    return MessageFormat.format(message, value0);
  }

  public static String t(String key, Object... values) {
    String message = _bundle.getString(key);
    return MessageFormat.format(message, values);
  }

}
