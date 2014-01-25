package net.hearthstats;

import java.io.File;
import java.io.IOException;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

public class Config {
	
	private static Wini _ini = null;
	
	public static String getUserKey() {
		return  _getIni().get("api", "userkey", String.class);
	}

	private static Wini _getIni() {
		if(_ini == null) {
			try {
				_ini = new Wini(new File("config.ini"));
			} catch (InvalidFileFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return _ini;
	}
}
