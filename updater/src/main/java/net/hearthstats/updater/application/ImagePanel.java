package net.hearthstats.updater.application;

import net.hearthstats.updater.exception.UpdaterException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;


/**
 * Customised JPanel that shows a HearthStats background image behind the controls.
 */
public class ImagePanel extends JPanel {
    Image image;

    public ImagePanel() {
      try {
        image = ImageIO.read(getClass().getResource("/net/hearthstats/updater/background.jpg"));
      } catch (IOException ex) {
        throw new UpdaterException("Cannot read background image");
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (image != null) {
        g.drawImage(image, 0, 0, null);
      }
    }

    public Dimension getPreferredSize() {
      return new Dimension(500, 350);
    }
}
