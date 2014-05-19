package net.hearthstats;

import java.util.List;

import lombok.Data;

@Data
public class Deck {
	private final int id;
	private final String name;
	private final List<Card> cards;

	@Data
	public static class Card {
		private final int id;
		private final String name;
		private final int count;

		public String url() {
			return String
					.format("https://s3-us-west-2.amazonaws.com/hearthstats/cards/%s.png",
							name.replaceAll("[^a-zA-Z]", "-").toLowerCase());
		}
	}
}
