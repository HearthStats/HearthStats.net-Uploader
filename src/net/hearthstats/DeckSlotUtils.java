package net.hearthstats;

import java.io.IOException;
import java.util.List;

import org.json.simple.JSONObject;

public class DeckSlotUtils {

	private static List<JSONObject> _decks;
	private static API _api = new API(); 
	
	public static void updateDecks() {
		try {
			_decks = _api.getDecks();
		} catch (IOException e) {
			Main.logException(e);
		}	
	}
	
	public static JSONObject getDeckFromSlot(Integer slotNum) {
		getDecks();
		for(int i = 0; i < _decks.size(); i++) {
			if(_decks.get(i).get("slot") != null && _decks.get(i).get("slot").toString().equals(slotNum.toString()))
				return _decks.get(i); 
		}
		return null;
	}
	
	public static List<JSONObject> getDecks() {
		if(_decks == null)
			updateDecks();
		return _decks;
	}
	
}
