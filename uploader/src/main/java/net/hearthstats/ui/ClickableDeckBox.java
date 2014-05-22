package net.hearthstats.ui;

import javax.swing.Box;

import net.hearthstats.Card;
import net.hearthstats.Deck;

public class ClickableDeckBox {
	private ClickableDeckBox() {
	}

	public static Box makeBox(Deck deck) {
		Box box = Box.createVerticalBox();
		for (Card card : deck.cards()) {
			box.add(new ClickableLabel(card));
		}
		return box;
	}

}