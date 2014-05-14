package net.hearthstats;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.hearthstats.Deck.Card;

public class DeckUtilsMain {
	public static void main(String[] args) {
		Box box = Box.createVerticalBox();
		for (Deck deck : DeckUtils.getDeckLists()) {
			System.out.println(deck);
			for (Card card : deck.getCards()) {
				System.out.println(card.url());
				box.add(new ClickableLabel(card));
			}
		}
		JOptionPane.showMessageDialog(new JFrame(), box);
	}

	static class ClickableLabel extends JLabel {
		private int remaining;
		private Card card;
		private Color initialColor;

		public ClickableLabel(Card card) {
			this.card = card;
			remaining = card.getCount();
			setText(label());
			setOpaque(true);

			initialColor = this.getBackground();
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							handleClick(e.getButton());
						}
					});
				}
			});
		}

		private String label() {
			return card.getName() + " - " + remaining;
		}

		private void handleClick(int button) {
			if (button == MouseEvent.BUTTON1 && remaining > 0) {
				remaining--;
			}
			if (button != MouseEvent.BUTTON1 && remaining < card.getCount()) {
				remaining++;
			}
			if (remaining > 0)
				setBackground(initialColor);
			else
				setBackground(Color.WHITE);
			setText(label());
		}
	}
}
