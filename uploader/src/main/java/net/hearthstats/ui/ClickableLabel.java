package net.hearthstats.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.hearthstats.Card;

@SuppressWarnings("serial")
public class ClickableLabel extends JLabel {
	private int remaining;
	private Card card;
	private ImageIcon cardImage;
	private ImageIcon currentBack = cardBack;
	private static ImageIcon cardBack = new ImageIcon(
			ClickableLabel.class.getResource("/images/cardBack.png"));
	private static ImageIcon cardBack2 = new ImageIcon(
			ClickableLabel.class.getResource("/images/cardBack2.png"));
	private static ImageIcon cardBackL = new ImageIcon(
			ClickableLabel.class.getResource("/images/cardBackL.png"));

	public ClickableLabel(final Card card) {
		this.card = card;
		cardImage = new ImageIcon(card.localURL());
		setPreferredSize(new Dimension(214, 38));
		setMaximumSize(new Dimension(214, 38));
		setMinimumSize(new Dimension(214, 38));
		remaining = card.count();
		setText(String.format("   %s      %s", card.cost(), card.name()));
		setFont(Font.decode(Font.SANS_SERIF).deriveFont(Font.BOLD, 14));
		setForeground(Color.WHITE);
		updateRemaining();

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

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		float scale = 289F / 214;
		AffineTransform scaleTransform = AffineTransform.getScaleInstance(
				scale, scale);
		g2.transform(scaleTransform);
		if (remaining < 1)
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.1f));
		g2.drawImage(cardImage.getImage(), 0, -38, null);
		try {
			scaleTransform.invert();
		} catch (NoninvertibleTransformException e) {
			throw new RuntimeException(e);
		}
		g2.transform(scaleTransform);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		g2.drawImage(currentBack.getImage(), 0, 0, null);
		super.paintComponent(g2);
	}

	private void handleClick(int button) {
		if (button == MouseEvent.BUTTON1 && remaining > 0) {
			remaining--;
		}
		if (button != MouseEvent.BUTTON1 && remaining < card.count()) {
			remaining++;
		}
		updateRemaining();
	}

	private void updateRemaining() {
		if (remaining > 1)
			currentBack = cardBack2;
		else if (card.isLegendary())
			currentBack = cardBackL;
		else
			currentBack = cardBack;
		repaint();
	}
}