package net.hearthstats;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Builder;
import lombok.experimental.Wither;

@Value
@Builder
@Accessors(fluent = true)
public class Card implements Comparable<Card>, Cloneable {
	private static String imageCacheFolder = Config.getImageCacheFolder();

	private int id;
	private String name;
	@Wither
	private int count;
	private int cost;
	private int rarity;

	public static final int LEGENDARY = 5;

	public boolean isLegendary() {
		return rarity == LEGENDARY;
	}

	public String fileName() {
		return String.format("%s.png", name.replaceAll("[^a-zA-Z]", "-")
				.toLowerCase());
	}

	public URL localURL() {
		try {
			return localFile().toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Invalid URL for " + name, e);
		}
	}

	public File localFile() {
		return new File(imageCacheFolder, fileName());
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