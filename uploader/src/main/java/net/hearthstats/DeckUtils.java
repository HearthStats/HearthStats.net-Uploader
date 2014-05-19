package net.hearthstats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

	public static JSONObject getDeckFromSlot(Integer slotNum) {
		getDecks();
		for (int i = 0; i < _decks.size(); i++) {
			if (_decks.get(i).get("slot") != null
					&& _decks.get(i).get("slot").toString()
							.equals(slotNum.toString()))
				return _decks.get(i);
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
			int id = Integer.parseInt(deck.get("id").toString());
			Object string = deck.get("cardstring");
			if (string != null) {
				String cardString = string.toString().trim();
				if (cardString.length() > 0) {
					deckLists.add(new Deck(id, deck.get("name").toString(),
							parseDeckString(cardString)));
				}
			}
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

	private static List<Card> parseDeckString(String ds) {
		Map<Integer, Card> cardData = CardUtils.getCards();
		List<Card> cards = new ArrayList<>();
		for (String card : ds.split(",")) {
			int u = card.indexOf('_');
			String count = card.substring(u + 1);
			int id = Integer.parseInt(card.substring(0, u));
			Card cd = cardData.get(id);
			cards.add(Card.builder().id(id).name(cd.name()).cost(cd.cost())
					.count(Integer.parseInt(count)).build());
		}
		Collections.sort(cards);
		return cards;
	}

}
