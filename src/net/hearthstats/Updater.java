package net.hearthstats;

import java.awt.Container;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import com.boxysystems.jgoogleanalytics.FocusPoint;
import com.boxysystems.jgoogleanalytics.JGoogleAnalyticsTracker;

public class Updater {

	private static String _availableVersion;
	private static String _recentChanges;
	private static String _savedUserKey;

	private static JGoogleAnalyticsTracker _analytics;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (Config.analyticsEnabled()) {
			_analytics = new JGoogleAnalyticsTracker("HearthStats.net Uploader", Config.getVersionWithOs(), "UA-45442103-3");
			_analytics.trackAsynchronously(new FocusPoint("UpdateStart"));
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
			_notify("Updator Error", "Unable to locate " + updaterFile.getPath());
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
			_unZipIt(updateZip.getPath(), "./");
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
				File newFile = new File(outputFolder + File.separator + fileName);

				System.out.println("file unzip : " + newFile.getAbsoluteFile());

				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
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
					Desktop.getDesktop().open(new File("HearthStats.app"));
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
				 zipUrl = "https://github.com/JeromeDane/HearthStats.net-Uploader/releases/download/v" + getAvailableVersion() + "/HearthStats.net.Uploader.v" + getAvailableVersion() + ".zip";
				 break;
			case "OSX":
				zipUrl = "https://github.com/JeromeDane/HearthStats.net-Uploader/releases/download/v" + getAvailableVersion() + "-osx/HearthStats.net.Uploader.v" + getAvailableVersion() + "-osx.zip";
				break;
		}
		website = new URL(zipUrl);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(Main.getExtractionFolder() + "/update.zip");
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}

	private static NotificationQueue _notificationQueue = new NotificationQueue();

	private static void _notify(String header) {
		_notify(header, "");
	}

	private static void _notify(String header, String message) {
		System.out.println(header);
		System.out.println(message);
		_notificationQueue.add(new Notification(header, message));

	}

	private static void _notifyException(Exception e) {
		JOptionPane.showMessageDialog(null, "Exception in Updater: " + e.toString());
	}

	public static String getAvailableVersion() {
		if (_availableVersion == null) {
			String url = "https://raw.github.com/JeromeDane/HearthStats.net-Uploader/master/src/version";
			if(Config.os.toString().equals("OSX"))
				url += "-osx";
			_availableVersion = _readRemoteFile(url);
		}
		return _availableVersion;
	}
	public static String getRecentChanges() {
		if (_recentChanges == null) {
			String url = "https://raw.github.com/JeromeDane/HearthStats.net-Uploader/master/src/recentchanges";
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
