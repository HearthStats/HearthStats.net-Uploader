package net.hearthstats.analysis;

import java.awt.image.BufferedImage;

import net.hearthstats.state.PixelLocation;
import net.hearthstats.state.UniquePixel;
import net.hearthstats.util.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Tests whether a set of pixels relative to a reference point match expected
 * colour values. Unlike the
 * {@link net.hearthstats.analysis.IndividualPixelAnalyser} which expects pixels
 * to be in a particular spot, the RelativePixelAnalyser can handle objects in
 * unknown position by finding a reference point across a broad section of the
 * screen and then calculating all the other pixels relative to that reference
 * point.
 * </p>
 * <p>
 * This makes the RelativePixelAnalyser most suitable for moving objects like
 * the victory and defect popups
 * </p>
 * 
 * @author gtch
 */
public class RelativePixelAnalyser extends CoordinateCacheBase {

  private final static Logger debugLog = LoggerFactory.getLogger(RelativePixelAnalyser.class);

  private float cachedRatio = 0;
  private float lastImageHeight = 0;

  Coordinate findRelativePixel(BufferedImage image, UniquePixel boundingBoxTopLeft,
      UniquePixel boundingBoxBottomRight, int xSamples, int ySamples) {

    UniquePixelIdentifier upiTopLeft = new UniquePixelIdentifier(boundingBoxTopLeft.x(),
        boundingBoxTopLeft.y(), image.getWidth(), image.getHeight());
    UniquePixelIdentifier upiBottomRight = new UniquePixelIdentifier(boundingBoxBottomRight.x(),
        boundingBoxBottomRight.y(), image.getWidth(), image.getHeight());

    Coordinate coordinateTopLeft = getCachedCoordinate(upiTopLeft);
    Coordinate coordinateBottomRight = getCachedCoordinate(upiBottomRight);

    float xStepSize = (float) (coordinateBottomRight.x() - coordinateTopLeft.x())
        / (float) (xSamples - 1);
    float yStepSize = (float) (coordinateBottomRight.y() - coordinateTopLeft.y())
        / (float) (ySamples - 1);

    debugLog.debug("relative pixel bounding box: topLeft={},{} bottomRight={},{} stepSize={},{}",
        coordinateTopLeft.x(), coordinateTopLeft.y(), coordinateBottomRight.x(),
        coordinateBottomRight.y(), xStepSize, yStepSize);

    for (int yCount = 0; yCount < ySamples; yCount++) {
      int y = coordinateTopLeft.y() + (int) (yCount * xStepSize);
      for (int xCount = 0; xCount < xSamples; xCount++) {
        int x = coordinateTopLeft.x() + (int) (xCount * xStepSize);

        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = (rgb & 0xFF);

        // The two bounding box pixels might have different colour ranges, so
        // test both: if either one matches than this pixel is considered to be
        // a match

        if (red >= boundingBoxTopLeft.minRed && red <= boundingBoxTopLeft.maxRed
            && green >= boundingBoxTopLeft.minGreen && green <= boundingBoxTopLeft.maxGreen
            && blue >= boundingBoxTopLeft.minBlue && blue <= boundingBoxTopLeft.maxBlue) {
          // This pixel is inside the expected range so it's an immediate match
          debugLog.debug("a matched reference pixel at {},{}", x, y);
          return new Coordinate(x, y);
        }

        if (red >= boundingBoxBottomRight.minRed && red <= boundingBoxBottomRight.maxRed
            && green >= boundingBoxBottomRight.minGreen && green <= boundingBoxBottomRight.maxGreen
            && blue >= boundingBoxBottomRight.minBlue && blue <= boundingBoxBottomRight.maxBlue) {
          // This pixel is inside the expected range so it's an immediate match
          debugLog.debug("b matched reference pixel at {},{}", x, y);
          return new Coordinate(x, y);
        }

      }
    }

    return null;

  }

  int countMatchingRelativePixels(BufferedImage image, Coordinate referencePixel,
      UniquePixel[] relativePixels) {

    int matches = 0;

    float ratio;
    if (lastImageHeight == image.getHeight()) {
      // Use the stored ratio
      ratio = cachedRatio;
    } else {
      // Calculate the ratio and store it for next time
      lastImageHeight = image.getHeight();
      ratio = (float) lastImageHeight / (float) PixelLocation.REFERENCE_SIZE.y();
      cachedRatio = ratio;
    }

    for (UniquePixel relativePixel : relativePixels) {
      int x = referencePixel.x() + (int) (relativePixel.x() * ratio);
      int y = referencePixel.y() + (int) (relativePixel.y() * ratio);

      int rgb = image.getRGB(x, y);
      int red = (rgb >> 16) & 0xFF;
      int green = (rgb >> 8) & 0xFF;
      int blue = (rgb & 0xFF);

      if (red >= relativePixel.minRed && red <= relativePixel.maxRed
          && green >= relativePixel.minGreen && green <= relativePixel.maxGreen
          && blue >= relativePixel.minBlue && blue <= relativePixel.maxBlue) {
        // This pixel is inside the expected range so it's an immediate match
        debugLog.debug("relative pixel at {}, {} matched {}", x, y, relativePixel);
        matches++;
      } else {
        debugLog.debug("relative pixel at {}, {} did not match {}", x, y, relativePixel);
      }

    }

    return matches;

  }

}
