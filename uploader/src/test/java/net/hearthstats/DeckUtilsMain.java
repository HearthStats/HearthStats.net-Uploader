package net.hearthstats;

import java.awt.HeadlessException;
import java.net.MalformedURLException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.hearthstats.ui.ClickableDeckBox;

public class DeckUtilsMain {
	public static void main(String[] args) throws HeadlessException,
			MalformedURLException {
		Deck deck = DeckUtils.getDeck(21994);
		System.out.printf("Deck : %s %n", deck);
		JOptionPane.showMessageDialog(new JFrame(),
				ClickableDeckBox.makeBox(deck));
	}
}
