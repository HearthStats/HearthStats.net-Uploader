package net.hearthstats.updater;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.prefs.Preferences;


public class UpdaterConfiguration {

  public static final String GITHUB_API_RELEASES = "github.api.releases";
  public static final String GITHUB_API_ASSET = "github.api.asset";
  public static final String GITHUB_OWNER = "github.owner";
  public static final String GITHUB_REPO = "github.repo";

  private static Properties properties;

  static {
    // Load the properties on startup
    try (Reader propertiesReader = new InputStreamReader(UpdaterConfiguration.class.getResourceAsStream("/net/hearthstats/updater/updater.properties"), "UTF-8")) {
      properties = new Properties();
      properties.load(propertiesReader);
    } catch (UnsupportedEncodingException e) {
      System.out.println("Cannot load properties because UTF-8 encoding is unsupported, this should not be possible!");
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("Cannot load properties due to exception reading updater.properties file");
      e.printStackTrace();
    }
  }


  /**
   * Convenient, shorter version of calling properties.getProperty().
   *
   * @param key The key of the property located in updater.properties
   * @return the value of that property, or an empty string if the key is invalid
   */
  private static String prop(String key) {
    String result = properties.getProperty(key);
    return result == null ? "" : result.trim();
  }


  public static String getReleasesApiUrl() {
    return MessageFormat.format(prop(GITHUB_API_RELEASES), prop(GITHUB_OWNER), prop(GITHUB_REPO));
  }

  public static String getAssetApiUrl(long assetId) {
    return MessageFormat.format(prop(GITHUB_API_ASSET), prop(GITHUB_OWNER), prop(GITHUB_REPO), String.valueOf(assetId));
  }


  public static String getClientUserAgent() {
    return prop("client.useragent");
  }

  public static String getClientAccept() {
    return prop("client.accept");
  }


  public static String getGitHubOAuthToken() {
    return prop("github.oauthtoken");
  }


  public static boolean getIncludeDraftReleases() {
    try {
      return Boolean.valueOf(prop("updater.includeDraftReleases"));
    } catch (Exception ex) {
      System.err.println("Ignoring exception parsing updater.includeDraftReleases: " + ex.getMessage());
      return false;
    }
  }

}
