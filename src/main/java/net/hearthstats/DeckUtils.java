package net.hearthstats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.hearthstats.Deck.Card;
import net.hearthstats.log.Log;

import org.json.simple.JSONObject;

public final class DeckUtils {
	private DeckUtils() {
	}

	private static List<JSONObject> _decks;
	private static API _api = new API(); 
	
	public static void updateDecks() {
		try {
			_decks = _api.getDecks();
		} catch (IOException e) {
            Log.warn("Error occurred while loading deck list from HearthStats.net", e);
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
	
	public static List<Deck> getDeckLists() {
		List<Deck> deckLists = new ArrayList<>();
		for (JSONObject deck : getDecks()) {
			int id = Integer.parseInt(deck.get("id").toString());
			Object string = deck.get("cardstring");
			if (string != null) {
				String cardString = string.toString().trim();
				if (cardString.length() > 0) {
					deckLists.add(new Deck(id, parseDeckString(cardString)));
				}
			}
		}
		return deckLists;
	}


	private static List<Card> parseDeckString(String ds) {
		List<Card> cards = new ArrayList<>();
		for (String card : ds.split(",")) {
			int u = card.indexOf('_');
			String cardId = card.substring(0, u);
			String count = card.substring(u + 1);
			cards.add(new Card(Integer.parseInt(cardId), "", Integer
					.parseInt(count)));
		}
		return cards;
	}

}
