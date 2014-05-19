package net.hearthstats;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true, chain = true)
public class Deck {
	private final int id;
	private final String name;
	private final List<Card> cards;

	@Data
	@Accessors(fluent = true, chain = true)
	public static class Card implements Comparable<Card> {
		private final int id;
		private final String name;
		private final int count;

		public String url() {
			return String
					.format("https://s3-us-west-2.amazonaws.com/hearthstats/cards/%s.png",
							name.replaceAll("[^a-zA-Z]", "-").toLowerCase());
		}

		@Override
		public int compareTo(Card c) {
			// TODO: sort by mana cost first
			int counts = Integer.compare(count, c.count);
			return counts != 0 ? counts : name.compareTo(c.name);
		}
	}
}
