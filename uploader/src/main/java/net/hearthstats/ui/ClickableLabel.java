package net.hearthstats.ui;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.hearthstats.Card;

@SuppressWarnings("serial")
public class ClickableLabel extends JLabel {
	private int remaining;
	private Card card;
	private Color initialColor;

	public ClickableLabel(Card card) {
		this.card = card;
		remaining = card.count();
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
		return String.format("%s - %s - %s/%s", card.cost(), card.name(),
				remaining, card.count());
	}

	private void handleClick(int button) {
		if (button == MouseEvent.BUTTON1 && remaining > 0) {
			remaining--;
		}
		if (button != MouseEvent.BUTTON1 && remaining < card.count()) {
			remaining++;
		}
		if (remaining > 0)
			setBackground(initialColor);
		else
			setBackground(Color.WHITE);
		setText(label());
	}
}