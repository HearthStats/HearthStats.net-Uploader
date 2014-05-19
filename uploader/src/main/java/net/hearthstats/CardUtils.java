package net.hearthstats;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.hearthstats.log.Log;

import org.json.simple.JSONObject;

public final class CardUtils {
	private CardUtils() {
	}

	private static Map<Integer, Card> cards;
	private static API _api = new API();

	private static void updateCards() {
		if (cards == null) {
			cards = new HashMap<Integer, Card>();
			try {
				for (JSONObject json : _api.getCards()) {
					int id = Integer.parseInt(json.get("id").toString());
					int cost = Integer.parseInt(json.get("mana").toString());
					cards.put(
							id,
							Card.builder().id(id).cost(cost)
									.name(json.get("name").toString()).build());
				}
			} catch (IOException e) {
				Log.warn(
						"Error occurred while loading cards data from HearthStats.net",
						e);
			}
		}
	}

	public static Map<Integer, Card> getCards() {
		updateCards();
		return cards;
	}
}
