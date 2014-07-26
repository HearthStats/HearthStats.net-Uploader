package net.hearthstats.game.imageanalysis;

import java.awt.image.BufferedImage;

import net.hearthstats.util.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests whether an individual pixels, or set of pixels, matched the expected
 * colour value.
 * 
 * @author gtch
 */
public class IndividualPixelAnalyser extends CoordinateCacheBase {

  private final static Logger debugLog = LoggerFactory.getLogger(IndividualPixelAnalyser.class);

  boolean testAllPixelsMatch(BufferedImage image, UniquePixel[] uniquePixels) {
    for (UniquePixel uniquePixel : uniquePixels) {

      UniquePixelIdentifier upi = new UniquePixelIdentifier(uniquePixel.x(), uniquePixel.y(),
          image.getWidth(), image.getHeight());

      Coordinate coordinate = getCachedCoordinate(upi);

      int x = coordinate.x();
      int y = coordinate.y();

      int rgb = image.getRGB(x, y);
      int red = (rgb >> 16) & 0xFF;
      int green = (rgb >> 8) & 0xFF;
      int blue = (rgb & 0xFF);

      if (red < uniquePixel.minRed || red > uniquePixel.maxRed || green < uniquePixel.minGreen
          || green > uniquePixel.maxGreen || blue < uniquePixel.minBlue
          || blue > uniquePixel.maxBlue) {
        // This pixel is outside the expected range
        return false;
      }

    }

    // No pixel texts failed, so this is a match
    debugLog.debug("matched all pixels {}", (Object[]) uniquePixels);

    return true;
  }

  boolean testAnyPixelsMatch(BufferedImage image, UniquePixel[] uniquePixels) {
    for (UniquePixel uniquePixel : uniquePixels) {

      UniquePixelIdentifier upi = new UniquePixelIdentifier(uniquePixel.x(), uniquePixel.y(),
          image.getWidth(), image.getHeight());

      Coordinate coordinate = getCachedCoordinate(upi);

      int x = coordinate.x();
      int y = coordinate.y();

      int rgb = image.getRGB(x, y);
      int red = (rgb >> 16) & 0xFF;
      int green = (rgb >> 8) & 0xFF;
      int blue = (rgb & 0xFF);

      if (red >= uniquePixel.minRed && red <= uniquePixel.maxRed && green >= uniquePixel.minGreen
          && green <= uniquePixel.maxGreen && blue >= uniquePixel.minBlue
          && blue <= uniquePixel.maxBlue) {
        // This pixel is inside the expected range so it's an immediate match
        debugLog.debug("matched {} any from pixel {}", uniquePixel, uniquePixels);
        return true;
      }

    }

    // All pixel text failed, so this is not a match
    return false;
  }

}
