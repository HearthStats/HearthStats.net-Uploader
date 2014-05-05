package net.hearthstats;

import net.hearthstats.notification.DialogNotificationQueue;
import net.hearthstats.notification.NotificationQueue;

import javax.swing.*;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Updater {

    private static final Set<String> FILES_TO_SKIP = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "instructions-osx.txt"
    )));
    public static final String DOWNLOAD_URL = "http://hearthstats.net/uploader";

    private static String _availableVersion;
	private static String _recentChanges;
	private static String _savedUserKey;

	private static JGoogleAnalyticsTracker _analytics;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (Config.analyticsEnabled()) {
			_analytics = AnalyticsTracker.tracker();
//			,"HearthStats.net Uploader", Config.getVersionWithOs(), "UA-45442103-3");
			_analytics.trackEvent("update","UpdateStart");
		}

		_saveSettings();

		_notify("Performing Update", "Downloading version " + getAvailableVersion() + ". Please be patient ...");
		try {
			_downloadUpdate();
			_notify("Performing Update", "v" + getAvailableVersion() + " downloaded ... extracting ...");
			_extractZip();
			_restoreSettings();
		} catch (IOException e) {
			e.printStackTrace();
			_notifyException(e);
		}

		_notify("Closing Updater ...");

		_runMain();

		System.exit(0);
	}

	public static void cleanUp() {
		_removeFile("updater.jar");
		_removeFile("HearthStatsUploader.jar");	// remove old jar since we're using an exe now
		_removeFile(Main.getExtractionFolder() + "/update.zip");
	}

	private static void _removeFile(String path) {
		File file = new File(path);
		if (file.isFile()) {
			file.delete();
		}
	}

	public static void run() {

		System.out.println("trying to run updater");

		_notify("Performing Update", "Extracting and running updater ...");

        String errorMessage = null;

        if (Config.os == Config.OS.WINDOWS) {

            File updaterFile = new File("updater.jar");
            Main.copyFileFromJarTo("/" + updaterFile.getPath(), updaterFile.getPath());
            if (updaterFile.exists()) {
                System.out.println("found updater.jar");
                try {
                    Runtime.getRuntime().exec("java -jar " + updaterFile.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                    _notifyException(e);
                }
            } else {
                errorMessage = "Unable to locate " + updaterFile.getPath();
            }

        } else if (Config.os == Config.OS.OSX) {

            File updaterFile = new File(Main.getExtractionFolder() + "/updater.jar");
            Main.copyFileFromJarTo("/updater.jar", updaterFile.getPath());

            if (updaterFile.exists()) {
                System.out.println("found updater.jar");

                // The Java library path is /<folder>/HearthStats.app/Contents/MacOS
                File javaLibraryPath = new File(Config.getJavaLibraryPath());
                // The bundle path is the folder where the bundle is stored, ie /<folder> (not including the HearthStats.app folder)
                File bundlePath = javaLibraryPath.getParentFile().getParentFile().getParentFile();

                String javaHome = System.getProperty("java.home");
                String[] command = new String[] {
                        javaHome + "/bin/java",
                        "-Dhearthstats.location=" + bundlePath.getAbsolutePath(),
                        "-jar",
                        updaterFile.getPath()
                };

                try {
                    Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    e.printStackTrace();
                    _notifyException(e);
                }
            } else {
                errorMessage = "Unable to locate " + updaterFile.getPath();
            }

        }

        if (errorMessage != null) {
            Main.showMessageDialog(null, errorMessage + "\n\nYou will now be taken to the download page.");
            try {
                Desktop.getDesktop().browse(new URI(DOWNLOAD_URL));
            } catch (Exception e) {
                Main.showErrorDialog("Error launching browser with URL " + DOWNLOAD_URL, e);
            }
            _notify("Updater Error", errorMessage);
        }

        System.exit(0);
    }


	public static void _saveSettings() {
		_savedUserKey = Config.getUserKey();
	}
	private static void _restoreSettings() {
		Config.rebuild();
		Config.setUserKey(_savedUserKey);

	}
	public static void _extractZip() {

        File updateZip = new File(Main.getExtractionFolder() + "/update.zip");

        if (updateZip.isFile()) {
            switch (Config.os) {
                case WINDOWS:
                    _unZipIt(updateZip.getPath(), "./");
                    break;
                case OSX:
                    String hearthstatsLocation = System.getProperty("hearthstats.location");
                    _unZipIt(updateZip.getPath(), hearthstatsLocation);
                    break;
            }
        } else {
            _notify("Updater Error", "Unable to locate " + updateZip.getPath());
        }

    }

	/**
	 * From http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/
	 *
	 * @param zipFile
	 * @param outputFolder
	 */
	private static void _unZipIt(String zipFile, String outputFolder) {

		byte[] buffer = new byte[1024];

		try {

			// create output directory is not exists
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}

			// get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				String fileName = ze.getName();
                if (!FILES_TO_SKIP.contains(fileName)) {
                    File newFile = new File(outputFolder + File.separator + fileName);

                    System.out.println("file unzip : " + newFile.getAbsoluteFile());

                    // create all non exists folders
                    // else you will hit FileNotFoundException for compressed folder
                    new File(newFile.getParent()).mkdirs();

                    if (!ze.isDirectory()) {
                        FileOutputStream fos = new FileOutputStream(newFile);
                        try {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        } finally {
                            fos.close();
                        }
                    }
                }

                ze = zis.getNextEntry();
            }

			zis.closeEntry();
			zis.close();

			System.out.println("Done");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void _runMain() {
		JOptionPane.showMessageDialog(null, "Update complete. Attempting to restart ...");
		_notify("Restarting ...");
		try {
			switch(Config.os.toString()) {
				case "WINDOWS":
					Runtime.getRuntime().exec("HearthStats.exe");
					break;
				case "OSX":
                    String hearthstatsLocation = System.getProperty("hearthstats.location");
                    Desktop.getDesktop().open(new File(hearthstatsLocation + "/HearthStats.app"));
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
			_notifyException(e);
		}
	}

	private static void _downloadUpdate() throws IOException {
		URL website;
		String zipUrl = null;
		switch(Config.os.toString()) {
			case "WINDOWS":
				 zipUrl = "https://github.com/HearthStats/HearthStats.net-Uploader/releases/download/v" + getAvailableVersion() + "/HearthStats.net.Uploader.v" + getAvailableVersion() + ".zip";
				 break;
			case "OSX":
				zipUrl = "https://github.com/HearthStats/HearthStats.net-Uploader/releases/download/v" + getAvailableVersion() + "-osx/HearthStats.net.Uploader.v" + getAvailableVersion() + "-osx.zip";
				break;
		}
		website = new URL(zipUrl);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(Main.getExtractionFolder() + "/update.zip");
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}

    private static NotificationQueue _notificationQueue = new DialogNotificationQueue();

	private static void _notify(String header) {
		_notify(header, "");
	}

	private static void _notify(String header, String message) {
		System.out.println(header);
		System.out.println(message);
		_notificationQueue.add(header, message, true);

	}

	private static void _notifyException(Exception e) {
		JOptionPane.showMessageDialog(null, "Exception in Updater: " + e.toString());
	}

	public static String getAvailableVersion() {
		if (_availableVersion == null) {
			String url = "https://raw.github.com/HearthStats/HearthStats.net-Uploader/master/src/version";
			if(Config.os.toString().equals("OSX"))
				url += "-osx";
			_availableVersion = _readRemoteFile(url);
		}
		return _availableVersion;
	}
	public static String getRecentChanges() {
		if (_recentChanges == null) {
			String url = "https://raw.github.com/HearthStats/HearthStats.net-Uploader/master/src/recentchanges";
			if(Config.os.toString().equals("OSX"))
				url += "-osx";
			_recentChanges = _readRemoteFile(url);
		}
		return _recentChanges;
	}

	private static String _readRemoteFile(String urlStr) {
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(null, "Exception in Updater: " + e.toString());
		}
		BufferedReader reader = null;
		String availableVersion = null;
		String returnStr = null;
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
			returnStr = "";
			for (String line; (line = reader.readLine()) != null;) {
				returnStr += line + "\n";
			}
			returnStr = returnStr.trim();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Exception in Updater: " + e.toString());
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Exception in Updater: Unable to get available version.\n\nAre you connected to the internet?\n\nIs GitHub down?");
				}
		}
		return returnStr;
	}

}
