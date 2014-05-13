package net.hearthstats;



public class DeckUtilsMain {
	public static void main(String[] args) {
		for (Deck deck : DeckUtils.getDeckLists()) {
			System.out.println(deck);
		}
	}
}
