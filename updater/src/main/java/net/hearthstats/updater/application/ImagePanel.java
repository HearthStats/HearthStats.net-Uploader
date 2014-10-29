package net.hearthstats.updater.application;

import net.hearthstats.updater.exception.UpdaterException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;


/**
 * Customised JPanel that shows a HearthStats image behind the controls.
 */
public class ImagePanel extends JPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 6488610821169302008L;
  private final Image image;
  private final boolean hidpi;
  private final int x;
  private final int y;

    public ImagePanel(String resourceName, int x, int y, boolean hidpi) {
      this.hidpi = hidpi;
      this.x = x;
      this.y = y;
      try {
        image = ImageIO.read(getClass().getResource(resourceName));
      } catch (IOException ex) {
        throw new UpdaterException("Cannot read background image");
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (image != null) {
        if (hidpi) {
          g.drawImage(image, x, y, image.getWidth(null)/2, image.getHeight(null)/2, null);
        } else {
          g.drawImage(image, x, y, null);
        }
      }
    }

//    public Dimension getPreferredSize() {
//      return new Dimension(500, 350);
//    }
}
