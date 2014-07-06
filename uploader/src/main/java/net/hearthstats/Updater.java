package net.hearthstats;

import net.hearthstats.config.Environment;
import net.hearthstats.log.Log;
import net.hearthstats.updater.api.GitHubReleases;
import net.hearthstats.updater.api.model.Release;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.net.URI;


public final class Updater {
	private Updater() {} // never instantiated

  public static final String MANUAL_DOWNLOAD_URL = "http://hearthstats.net/uploader";

  private final static Logger debugLog = LoggerFactory.getLogger(Updater.class);

  private static Release cachedlatestRelease;


  public static Release getLatestRelease() {
    if (cachedlatestRelease == null) {
      try {
        debugLog.debug("Loading latest release information from GitHub");
        cachedlatestRelease = GitHubReleases.getLatestRelease(true);
        if (cachedlatestRelease == null) {
          Log.warn("Unable to check latest release of HearthStats Uploader");
        }
      } catch (Exception e) {
        Log.warn("Unable to check latest release of HearthStats Uploader due to error: " + e.getMessage(), e);
      }
    }
    return cachedlatestRelease;
  }


  public static String getAvailableVersion() {
    Release release = getLatestRelease();
    return release == null ? null : release.getVersion();
  }


  public static String getRecentChanges() {
    Release release = getLatestRelease();
    return release == null ? null : release.getBody();
  }


  public static void run(Environment environment, Release release) {
    Log.info("Extracting and running updater ...");

    String errorMessage = environment.performApplicationUpdate(release);

    if (errorMessage == null) {
      // There was no error, so the updater should be running now
      System.exit(0);

    } else {
      // There was an error
      Main.showMessageDialog(null, errorMessage + "\n\nYou will now be taken to the download page.");
      try {
        Desktop.getDesktop().browse(new URI(MANUAL_DOWNLOAD_URL));
      } catch (Exception e) {
        Main.showErrorDialog("Error launching browser with URL " + MANUAL_DOWNLOAD_URL, e);
      }
      Log.warn("Updater Error: " + errorMessage);
    }


  }


  public static void cleanUp() {
		removeFile(Config.getExtractionFolder() + "/updater.jar");
		removeFile(Config.getExtractionFolder() + "/update.zip");
	}


	private static void removeFile(String path) {
		File file = new File(path);
		if (file.isFile()) {
			file.delete();
		}
	}

}
