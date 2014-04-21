package net.hearthstats.analysis;

import net.hearthstats.state.PixelLocation;
import net.hearthstats.state.UniquePixel;
import net.hearthstats.util.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests whether an individual pixels, or set of pixels, matched the expected colour value.
 *
 * @author gtch
 */
public class IndividualPixelAnalyser {

    private final static Logger debugLog = LoggerFactory.getLogger(IndividualPixelAnalyser.class);

    private Map<UniquePixelIdentifier, Coordinate> coordinateCache;


    public IndividualPixelAnalyser() {
        this.coordinateCache = new HashMap<>();
    }


    boolean testAllPixelsMatch(BufferedImage image, UniquePixel[] uniquePixels) {
        for (UniquePixel uniquePixel : uniquePixels) {

            UniquePixelIdentifier upi = new UniquePixelIdentifier(uniquePixel.x, uniquePixel.y, image.getWidth(), image.getHeight());

            Coordinate coordinate = coordinateCache.get(upi);
            if (coordinate == null) {
                coordinate = calculatePixelCoordinate(upi);
                coordinateCache.put(upi, coordinate);
            }

            int x = coordinate.x;
            int y = coordinate.y;

            int rgb = image.getRGB(x, y);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = (rgb & 0xFF);

            if (red < uniquePixel.minRed || red > uniquePixel.maxRed
                    || green < uniquePixel.minGreen || green > uniquePixel.maxGreen
                    || blue < uniquePixel.minBlue || blue > uniquePixel.maxBlue) {
                // This pixel is outside the expected range
                return false;
            }

        }

        // No pixel texts failed, so this is a match
        debugLog.debug("matched all pixels {}", uniquePixels);

        return true;
    }


    boolean testAnyPixelsMatch(BufferedImage image, UniquePixel[] uniquePixels) {
        for (UniquePixel uniquePixel : uniquePixels) {

            UniquePixelIdentifier upi = new UniquePixelIdentifier(uniquePixel.x, uniquePixel.y, image.getWidth(), image.getHeight());

            Coordinate coordinate = coordinateCache.get(upi);
            if (coordinate == null) {
                debugLog.debug("coordinateCache miss");
                coordinate = calculatePixelCoordinate(upi);
                coordinateCache.put(upi, coordinate);

            } else {
                debugLog.debug("coordinateCache hit");
            }

            int x = coordinate.x;
            int y = coordinate.y;

            int rgb = image.getRGB(x, y);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = (rgb & 0xFF);

            if (red >= uniquePixel.minRed && red <= uniquePixel.maxRed
                    && green >= uniquePixel.minGreen && green <= uniquePixel.maxGreen
                    && blue >= uniquePixel.minBlue && blue <= uniquePixel.maxBlue) {
                // This pixel is inside the expected range so it's an immediate match
                debugLog.debug("matched {} any from pixel {}", uniquePixel, uniquePixels);
                return true;
            }

        }

        // All pixel text failed, so this is not a match
        return false;
    }



    private Coordinate calculatePixelCoordinate(UniquePixelIdentifier upi) {

        if (debugLog.isDebugEnabled()) {
            debugLog.debug("Calculating pixel position {},{} for width {} height {}",
                    new Object[] { upi.x, upi.y, upi.width, upi.height });
        }

        Coordinate result;

        if (upi.width == PixelLocation.REFERENCE_SIZE.x && upi.height == PixelLocation.REFERENCE_SIZE.y) {
            // The screen size is exactly what our reference pixels are based on, so we can use their coordinates directly
            result = new Coordinate(upi.x, upi.y);
            debugLog.debug("Stored position as {}", result);

        } else {
            // The screen size is different to our reference pixels, so coordinates need to be adjusted
            float ratio = (float) upi.height / (float) PixelLocation.REFERENCE_SIZE.y;
            float screenRatio = (float) upi.width / (float) upi.height;

            int xOffset;
            if (screenRatio > 1.4) {
                xOffset = (int) (((float) upi.width - (ratio * PixelLocation.REFERENCE_SIZE.x)) / 2);
            } else {
                xOffset = 0;
            }

            int x = (int) (upi.x * ratio) + xOffset;
            int y = (int) (upi.y * ratio);
            result = new Coordinate(x, y);
            debugLog.debug("Calculated position as {}", result);
        }

        return result;
    }


    static class UniquePixelIdentifier {
        final int x;
        final int y;
        final int width;
        final int height;

        UniquePixelIdentifier(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UniquePixelIdentifier that = (UniquePixelIdentifier) o;

            if (height != that.height) return false;
            if (width != that.width) return false;
            if (x != that.x) return false;
            if (y != that.y) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + width;
            result = 31 * result + height;
            return result;
        }

    }

}
