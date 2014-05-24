package net.hearthstats.ui;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.hearthstats.Card;
import net.hearthstats.Deck;
import net.hearthstats.log.Log;

public class ClickableDeckBox {

	private ClickableDeckBox() {
	}

	public static Box makeBox(Deck deck) {
		downloadImages(deck);
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

	private static void downloadImages(Deck deck) {
		ScheduledExecutorService scheduledExecutorService = Executors
				.newScheduledThreadPool(30);

		for (final Card card : deck.cards()) {
			scheduledExecutorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						ReadableByteChannel rbc = Channels.newChannel(new URL(
								card.url()).openStream());
						File file = card.localFile();
						if (file.length() < 30000) {
							// probably not downloaded correctly yet
							FileOutputStream fos = new FileOutputStream(file);
							fos.getChannel().transferFrom(rbc, 0,
									Long.MAX_VALUE);
							fos.close();
							rbc.close();
							Log.debug(card.name() + " saved to cache folder");
						} else {
							Log.debug(card.name()
									+ " already in cache, skipping");
						}
					} catch (Exception e) {
						Log.error(
								"Could not download image for " + card.name(),
								e);
					}
				}
			});
		}
		try {
			scheduledExecutorService.shutdown();
			scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS);
			Log.info("all images downloaded successfully");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
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
					imageLabel.setIcon(new ImageIcon(card.localURL()));
				}
			});
		}
	}

}