package net.hearthstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.json.simple.JSONArray;
//http://www.mkyong.com/java/json-simple-example-read-and-write-json/
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class API extends Observable {

	private String _key;
	private String _message;
	
	public API() {
		 
	}
	
	public void endCurrentArenaRun() throws IOException {
		
		JSONObject resultObj = (JSONObject) _get("arena_runs/end");
		ArenaRun arenaRun = resultObj == null ? null : new ArenaRun(resultObj);
		if(arenaRun != null)
			_dispatchResultMessage("Ended " + arenaRun.getUserClass() + " arena run");
	}
	
	public ArenaRun createArenaRun(ArenaRun arenaRun) throws IOException {
		
		JSONObject result = _post("arena_runs/new", arenaRun.toJsonObject());
		
		ArenaRun resultingArenaRun = null;
		if(result != null) {
			resultingArenaRun = new ArenaRun(result);
			_dispatchResultMessage(resultingArenaRun.getUserClass() + " run created");
		}
		
		return resultingArenaRun;
	}
	
	
	private int _lastMatchId = 0;
	
	public int getLastMatchId() {
		return _lastMatchId;
	}
	public void setDeckSlots(Integer slot1, Integer slot2, Integer slot3, Integer slot4, Integer slot5, Integer slot6, Integer slot7, Integer slot8, Integer slot9) throws IOException {
		JSONObject jsonData = new JSONObject();
		jsonData.put("slot_1", slot1);
		jsonData.put("slot_2", slot2);
		jsonData.put("slot_3", slot3);
		jsonData.put("slot_4", slot4);
		jsonData.put("slot_5", slot5);
		jsonData.put("slot_6", slot6);
		jsonData.put("slot_7", slot7);
		jsonData.put("slot_8", slot8);
		jsonData.put("slot_9", slot9);
		
		_post("decks/slots", jsonData);
	}
	public void createMatch(HearthstoneMatch hsMatch) throws IOException {
		
		JSONObject result = null;
		
		result = _post("matches/new", hsMatch.toJsonObject());
			
		if(result != null) {
			try {
				_lastMatchId = Math.round((Long) result.get("id"));
			} catch(Exception e) {
				Main.logException(e);
			}
			if(hsMatch.getMode() != "Arena")
				_dispatchResultMessage("Success. <a href=\"http://hearthstats.net/constructeds/" + result.get("id") + "/edit\">Edit match #" + result.get("id") + " on HearthStats.net</a>");
			else
				_dispatchResultMessage("Arena match successfully created");
		}
		
	}
	public ArenaRun getLastArenaRun() throws IOException {
		
		JSONObject resultObj = (JSONObject) _get("arena_runs/show");
		ArenaRun arenaRun =  resultObj == null ? null : new ArenaRun(resultObj);
		if(arenaRun != null) {
			_dispatchResultMessage("Fetched current " + arenaRun.getUserClass() + " arena run");
		}
		return arenaRun;
	}
	
	private Object _get(String method) throws MalformedURLException, IOException {
		URL url = new URL(Config.getApiBaseUrl() + method + "?userkey=" + _getKey());
		BufferedReader reader = null;
		String resultString = "";
		try {
		    reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		    
		    for (String line; (line = reader.readLine()) != null;) {
		    	resultString += line;
		    }
		} catch(Exception e) {
			Main.logException(e, false);
			_throwError("Error communicating with HearthStats.net");
		} finally {
		    if (reader != null) try { reader.close(); } catch (IOException ignore) {}
		}
		return _parseResult(resultString);		
	}
	
	private Object _parseResult(String resultString) {
		JSONParser parser = new JSONParser();
		JSONObject result = null;
		try {
			result = (JSONObject) parser.parse(resultString);
		} catch(Exception ignore) {
			_throwError("Error parsing reply");
		}
		if(result.get("status").toString().matches("success")) {
			try {
				_message = result.get("message").toString();
			} catch(Exception e) {
				_message = null;
			}
			try {
				return (JSONObject) result.get("data");
			} catch(Exception e) {
				try {
					return (JSONArray) result.get("data");
				} catch(Exception e1) {
					// eat it ... there's just no data to parse
					return null;
				}
			}
		} else {
			_throwError((String) result.get("message"));
			return null;
		}
	}
	
	private JSONObject _post(String method, JSONObject jsonData) throws IOException {
		
		URL url = new URL(Config.getApiBaseUrl() + method + "?userkey=" + _getKey());
		
		HttpURLConnection httpcon = null;
		try {
			httpcon = (HttpURLConnection) (url.openConnection());
			
			httpcon.setDoOutput(true);
			httpcon.setRequestProperty("Content-Type", "application/json");
			httpcon.setRequestProperty("Accept", "application/json");
			httpcon.setRequestMethod("POST");
			httpcon.connect();
	
			// send JSON
			byte[] outputBytes = jsonData.toJSONString().getBytes("UTF-8");
			OutputStream os = httpcon.getOutputStream();
			os.write(outputBytes);
			os.close();
			
			// get response
			InputStream instr = httpcon.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(instr));
			String resultString = "";
			String lin;
			while((lin = br.readLine())!=null){
				resultString += lin;
			}
			return (JSONObject) _parseResult(resultString);	
		} catch(Exception e) {
			Main.logException(e, false);
			_throwError("Error communicating with HearthStats.net");
		}
		return null;		
	}
	
	public String getMessage() {
		return _message;
	}
	
	private String _getKey() throws IOException {
		return Config.getUserKey();
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

	public List<JSONObject> getDecks() throws IOException {
		JSONArray resultArray = (JSONArray) _get("decks/show");
		if(resultArray != null) {
			_dispatchResultMessage("Fetched your decks from HearthStats.net");
		}
		
		List<JSONObject> jsonValues = new ArrayList<JSONObject>();
		for (int i = 0; i < resultArray.size(); i++)
		   jsonValues.add((JSONObject) resultArray.get(i));
		return jsonValues;
		
	}

}
