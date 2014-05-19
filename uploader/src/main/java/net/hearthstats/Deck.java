package net.hearthstats;

import java.util.List;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Builder;

@Value
@Builder
@Accessors(fluent = true)
public class Deck {
	private int id;
	private String name;
	private List<Card> cards;
}
