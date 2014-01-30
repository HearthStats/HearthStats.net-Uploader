package net.hearthstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

public class Updater {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JOptionPane.showMessageDialog(null, "updater: " + Config.getVersion());
	}
	
	public static String getAvailableVersion() {
		URL url = null;
		try {
			url = new URL("https://raw.github.com/JeromeDane/HearthStats.net-Uploader/master/src/version");
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(null, "Exception Updater: " + e.toString());
		}
		BufferedReader reader = null;
		String availableVersion = null;
		try {
		    reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		    availableVersion = "";
		    for (String line; (line = reader.readLine()) != null;) {
		    	availableVersion += line;
		    }
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Exception Updater: " + e.toString());
		} finally {
		    if (reader != null) try { reader.close(); } catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Exception Updater: Unable to get available version.\n\nAre you connected to the internet?\n\nIs GitHub down?");
		    }
		}
		return availableVersion;
	}

}
