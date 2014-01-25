package net.hearthstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.util.Observable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class API extends Observable {

	private String _key;
	private String _message;
	private String _baseURL = "http://192.168.1.114:3000/api/v1/";
	
	public API() {
		 
	}
	
	public ArenaRun getCurrentArenaRun() throws IOException {
		
		JSONObject resultObj = _get("arena_runs/show");
		ArenaRun arenaRun =  resultObj == null ? null : new ArenaRun(resultObj);
		if(arenaRun != null) {
			_dispatchResultMessage("Fetched current " + arenaRun.userclass + " arena run");
		}
		return arenaRun;
	}
	
	private JSONObject _get(String method) throws MalformedURLException, IOException {
		URL url = new URL(_baseURL + method + "?key=" + _getKey());
		BufferedReader reader = null;
		String resultString = "";
		try {
		    reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		    
		    for (String line; (line = reader.readLine()) != null;) {
		    	resultString += line;
		    }
		} finally {
		    if (reader != null) try { reader.close(); } catch (IOException ignore) {}
		}
		return _parseResult(resultString);		
	}
	
	private JSONObject _parseResult(String resultString) {
		JSONParser parser = new JSONParser();
		JSONObject result = null;
		try {
			result = (JSONObject) parser.parse(resultString);
		} catch(Exception ignore) {
			_throwError("Error parsing reply");
		}
		Object s = result.get("status").toString();
		if(result.get("status").toString().matches("success")) {
			return (JSONObject) result.get("data");
		} else {
			_throwError((String) result.get("message"));
			return null;
		}
	}
	
	public String getMessage() {
		return _message;
	}
	
	private String _getKey() throws IOException {
		if(_key != null)
			return _key;
		try {
			_key = Files.readAllLines(Paths.get("appkey.txt"), Charset.defaultCharset()).get(0);
		} catch(IOException e) {
			_throwError("File " + e.getMessage() + " not found");
		}
		return _key;
	}
	
	private void _dispatchResultMessage(String message) {
		_setMessage(message);
		setChanged();
		notifyObservers("result");
	}
	
	private void _throwError(String message) {
		_setMessage(message);
		setChanged();
		notifyObservers("error");
	}
	
	private void _setMessage(String message) {
		_message = message;
	}

}
