package net.hearthstats;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.hearthstats.ui.ClickableDeckBox;

public class DeckUtilsMain {
	public static void main(String[] args) {
		Deck deck = DeckUtils.getDeck(21994);
		System.out.printf("Deck : %s %n", deck);
		JOptionPane.showMessageDialog(new JFrame(),
				ClickableDeckBox.makeBox(deck));
	}
}
