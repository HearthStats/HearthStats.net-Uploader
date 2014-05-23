package net.hearthstats;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Builder;

@Value
@Builder
@Accessors(fluent = true)
public class Card implements Comparable<Card> {
	private int id;
	private String name;
	private int count;
	private int cost;

	public String fileName() {
		return String.format("%s.png", name.replaceAll("[^a-zA-Z]", "-")
				.toLowerCase());
	}

	public String url() {
		return String.format(
				"https://s3-us-west-2.amazonaws.com/hearthstats/cards/%s",
				fileName());
	}

	@Override
	public int compareTo(Card c) {
		int costs = Integer.compare(cost, c.cost);
		if (costs != 0)
			return costs;
		int counts = Integer.compare(count, c.count);
		if (counts != 0)
			return counts;
		return name.compareTo(c.name);
	}
}