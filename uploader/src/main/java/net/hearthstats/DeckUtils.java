package net.hearthstats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
			Log.warn(
					"Error occurred while loading deck list from HearthStats.net",
					e);
		}
	}

	public static Deck getDeckFromSlot(Integer slotNum) {
		getDecks();
		for (int i = 0; i < _decks.size(); i++) {
			if (_decks.get(i).get("slot") != null
					&& _decks.get(i).get("slot").toString()
							.equals(slotNum.toString()))
				return Deck.fromJson(_decks.get(i));
		}
		return null;
	}

	public static List<JSONObject> getDecks() {
		if (_decks == null)
			updateDecks();
		return _decks;
	}

	public static List<Deck> getDeckLists() {
		List<Deck> deckLists = new ArrayList<>();
		for (JSONObject deck : getDecks()) {
			deckLists.add(Deck.fromJson(deck));
		}
		return deckLists;
	}

	public static Deck getDeck(int id) {
		for (Deck deck : getDeckLists()) {
			if (id == deck.id())
				return deck;
		}
		throw new IllegalArgumentException("No deck found for id " + id);
	}

}
