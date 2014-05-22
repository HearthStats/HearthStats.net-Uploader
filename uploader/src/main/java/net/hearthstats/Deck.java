package net.hearthstats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Builder;

import org.json.simple.JSONObject;

@Value
@Builder
@Accessors(fluent = true)
public class Deck {
	private int id;
	private String name;
	private List<Card> cards;

	public boolean isValid() {
		if (cards != null) {
			int count = 0;
			for (Card c : cards) {
				count += c.count();
			}
			return count == 30;
		} else
			return false;
	}

	public static Deck fromJson(JSONObject json) {
		int id = Integer.parseInt(json.get("id").toString());
		Object string = json.get("cardstring");

		List<Card> cardList = null;
		if (string != null) {
			String cardString = string.toString().trim();
			if (cardString.length() > 0) {
				cardList = parseDeckString(cardString);
			}
		}
		return new Deck(id, json.get("name").toString(), cardList);

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
