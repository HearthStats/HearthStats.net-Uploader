package net.hearthstats.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A simple help icon that can be clicked to browse to the given URL.
 */
public class HelpIcon extends JLabel {

  private static final long serialVersionUID = 50869548301146348L;
    private final ImageIcon normalIcon;
    private final ImageIcon hoverIcon;


    public HelpIcon(final String url, String tooltip) {
        BufferedImage normalImage = null;
        BufferedImage hoverImage = null;
        try {
            normalImage = ImageIO.read(getClass().getResource("/images/help-normal.png"));
            hoverImage = ImageIO.read(getClass().getResource("/images/help-hover.png"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read help icons", e);
        }
        normalIcon = new ImageIcon(normalImage);
        hoverIcon = new ImageIcon(hoverImage);

        normalIcon.setDescription("Help");
        hoverIcon.setDescription("Help");

        setToolTipText(tooltip);

        setIcon(normalIcon);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    Desktop d = Desktop.getDesktop();
                    try {
                        d.browse(new URI(url));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setIcon(hoverIcon);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                setIcon(normalIcon);
            }
        });
    }

}
