package net.hearthstats;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import sun.misc.IOUtils;

public class Config {

	private static String _version;
	
	private static Wini _ini = null;
	
	public static String getUserKey() {
		return  _getIni().get("API", "userkey", String.class);
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

	public static boolean analyticsEnabled() {
		return _getIni().get("analytics", "enabled").toString().matches("1");
	}
	
	public static boolean checkForUpdates() {
		return _getIni().get("updates", "check").toString().matches("1");
	}
	
	public static boolean alertUpdates() {
		return _getIni().get("updates", "alert").toString().matches("1");
	}
	
	public static String getVersion() throws IOException {
		if(_version == null) {
			_version = "";
			List<String> lines = Files.readAllLines(Paths.get("version"), Charset.defaultCharset());
			for (String line : lines) {
                _version += line;
            }
		}
		return _version;
	}
}
