package net.hearthstats;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class HyperLinkHandler {

	public static HyperlinkListener getInstance() {
		return new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
					if (Desktop.isDesktopSupported()) {
						// Create Desktop object
						Desktop d = Desktop.getDesktop();
						// Browse a URL, say google.com
						try {
							d.browse(new URI(e.getURL().toString()));
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (URISyntaxException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		};
	}

}
