package net.hearthstats.ui;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.hearthstats.Card;
import net.hearthstats.Deck;

public class ClickableDeckBox {
	private ClickableDeckBox() {
	}

	public static Box makeBox(Deck deck) {
		Box container = Box.createHorizontalBox();
		Box box = Box.createVerticalBox();
		JLabel imageLabel = new JLabel();
		imageLabel.setPreferredSize(new Dimension(289, 398));
		for (Card card : deck.cards()) {
			ClickableLabel cardLabel = new ClickableLabel(card);
			box.add(cardLabel);
			cardLabel.addMouseListener(new MouseHandler(card, imageLabel));
		}
		container.add(box);
		container.add(imageLabel);
		return container;
	}

	private static class MouseHandler extends MouseAdapter {
		private Card card;
		private JLabel imageLabel;

		public MouseHandler(Card card, JLabel imageLabel) {
			this.card = card;
			this.imageLabel = imageLabel;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						imageLabel.setIcon(new ImageIcon(new URL(card.url())));
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

}