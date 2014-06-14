package net.hearthstats;

import net.hearthstats.config.OS;
import net.hearthstats.notification.DialogNotificationQueue;
import net.hearthstats.notification.NotificationQueue;

import javax.swing.*;

import com.dmurph.tracking.JGoogleAnalyticsTracker;

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

public final class Updater {
	private Updater() {} // never instanciated
	
	private static String RAW_GITHUB_URL = "https://raw.github.com/";
	private static String GITHUB_URL = "https://github.com/";
	private static String PROJECT="HearthStats/HearthStats.net-Uploader";
	private static String SRC_URL = RAW_GITHUB_URL + PROJECT + "/master/src/";
	private static String RELEASE_URL = GITHUB_URL + PROJECT+ "/releases/download/v";
	public static final String DOWNLOAD_URL = "http://hearthstats.net/uploader";

    private static final Set<String> FILES_TO_SKIP = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "instructions-osx.txt"
    )));

    private static String _availableVersion;
	private static String _recentChanges;
	private static String _savedUserKey;
	
	private static String hearthstatsLocation;

	private static JGoogleAnalyticsTracker _analytics;

	private static void doUpdate() {

		if (Config.analyticsEnabled()) {
			_analytics = AnalyticsTracker.tracker();
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
		_removeFile(Config.getExtractionFolder() + "/update.zip");
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
		try {
			if (Config.os == OS.WINDOWS) {
				doUpdate();
			} else if (Config.os == OS.OSX) {
				// The Java library path is
				// /<folder>/HearthStats.app/Contents/MacOS
				File javaLibraryPath = new File(Config.getJavaLibraryPath());
				// The bundle path is the folder where the bundle is stored, ie
				// /<folder> (not including the HearthStats.app folder)
				File bundlePath = javaLibraryPath.getParentFile()
						.getParentFile().getParentFile();
				hearthstatsLocation=bundlePath.getAbsolutePath();
				Updater.doUpdate();
			}
		} catch (Exception e) {
			e.printStackTrace();
			_notifyException(e);
			Main.showMessageDialog(null, e.getMessage()
					+ "\n\nYou will now be taken to the download page.");
			try {
				Desktop.getDesktop().browse(new URI(DOWNLOAD_URL));
			} catch (Exception e1) {
				Main.showErrorDialog("Error launching browser with URL "
						+ DOWNLOAD_URL, e1);
			}
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

	private static void _extractZip() {

        File updateZip = new File(Config.getExtractionFolder() + "/update.zip");

        if (updateZip.isFile()) {
            switch (Config.os) {
                case WINDOWS:
                    _unZipIt(updateZip.getPath(), "./");
                    break;
                case OSX:
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
		try {
			switch(Config.os) {
				case WINDOWS:
                    JOptionPane.showMessageDialog(null, "Update complete. Attempting to restart ...");
                    _notify("Restarting ...");
					Runtime.getRuntime().exec("HearthStats.exe");
					break;
				case OSX:
                    // OS X will not run another copy of an application that is already running, so the user must restart manually
                    JOptionPane.showMessageDialog(null, "Update complete. HearthStats will now close, please reopen to run the new version.");
                    //Desktop.getDesktop().open(new File(hearthstatsLocation + "/HearthStats.app"));
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
		switch(Config.os) {
			case WINDOWS:
				 zipUrl = RELEASE_URL + getAvailableVersion() + "/HearthStats.net.Uploader.v" + getAvailableVersion() + ".zip";
				 break;
			case OSX:
				zipUrl = RELEASE_URL + getAvailableVersion() + "-osx/HearthStats.net.Uploader.v" + getAvailableVersion() + "-osx.zip";
				break;
		}
		website = new URL(zipUrl);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(Config.getExtractionFolder() + "/update.zip");
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
			String url = SRC_URL + "version";
			if(Config.os.toString().equals("OSX"))
				url += "-osx";
			_availableVersion = _readRemoteFile(url);
		}
		return _availableVersion;
	}
	
	public static String getRecentChanges() {
		if (_recentChanges == null) {
			String url = SRC_URL + "recentchanges";
			if(Config.os == OS.OSX)
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
