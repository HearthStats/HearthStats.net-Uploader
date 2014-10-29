package net.hearthstats.game.imageanalysis;

import net.hearthstats.util.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>A base class for pixel analysers that caches the coordinates of pixels for the current screen size to avoid
 * recalculating pixel coordinates for every iteration.</p>
 * <p>All classes that extend CoordinateCacheBase share the same cache for efficiency, even if there are multiple
 * instances of those subclasses.</p>
 *
 * @author gtch
 */
public class CoordinateCacheBase {

    private final static int MAXIMUM_CACHE_ITEMS = 1000;

    private final static Logger debugLog = LoggerFactory.getLogger(CoordinateCacheBase.class);

    private final static Map<UniquePixelIdentifier, Coordinate> coordinateCache = new HashMap<>();


    protected Coordinate getCachedCoordinate(UniquePixelIdentifier upi) {
        Coordinate coordinate = coordinateCache.get(upi);
        if (coordinate == null) {
            coordinate = calculatePixelCoordinate(upi);
            if (coordinateCache.size() > MAXIMUM_CACHE_ITEMS) {
                coordinateCache.clear();
            }
            coordinateCache.put(upi, coordinate);
        }
        return coordinate;
    }

    protected Coordinate calculatePixelCoordinate(UniquePixelIdentifier upi) {

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

    public static class UniquePixelIdentifier {
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
