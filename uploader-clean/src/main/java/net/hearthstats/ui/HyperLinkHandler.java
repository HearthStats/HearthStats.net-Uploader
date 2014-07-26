package net.hearthstats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class HyperLinkHandler {

    private final static Logger debugLog = LoggerFactory.getLogger(HyperLinkHandler.class);

	public static HyperlinkListener getInstance() {
		return new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
					if (Desktop.isDesktopSupported()) {
						Desktop d = Desktop.getDesktop();
						try {
							d.browse(new URI(e.getURL().toString()));
						} catch (IOException e1) {
                            debugLog.warn("IOException accessing URL " + e.getURL(), e1);
							e1.printStackTrace();
						} catch (URISyntaxException e1) {
                            debugLog.warn("URISyntaxException accessing URL " + e.getURL(), e1);
							e1.printStackTrace();
						}
					}
				}
			}
		};
	}

    /**
     * Gets a JLabel formatted to look like a URL, which can be clicked to display the URL in a browser.
     * @param url The URL to display - must be URL only, no other text
     * @return A JLabe for the given URL
     */
    public static JLabel getUrlLabel(String url) {
        JLabel label = new JLabel(url);
        label.setForeground(Color.BLUE);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new OpenHyperlinkAdapter(url));
        return label;
    }


    private static class OpenHyperlinkAdapter extends MouseAdapter {

        final String url;

        private OpenHyperlinkAdapter(String url) {
            this.url = url;
        }


        @Override
        public void mouseClicked(MouseEvent e) {
            if (Desktop.isDesktopSupported()) {
                Desktop d = Desktop.getDesktop();
                try {
                    d.browse(new URI(url.toString()));
                } catch (IOException e1) {
                    debugLog.warn("IOException accessing URL " + url, e1);
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    debugLog.warn("URISyntaxException accessing URL " + url, e1);
                    e1.printStackTrace();
                }
            }
        }
    }

}
