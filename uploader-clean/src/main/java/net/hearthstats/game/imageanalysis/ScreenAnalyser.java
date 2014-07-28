package net.hearthstats.game.imageanalysis;

import net.hearthstats.game.Screen;
import net.hearthstats.game.ScreenGroup;
import net.hearthstats.util.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * An analyser that identifies Hearthstone screens from screenshots.
 * 
 * @author gtch
 */
public class ScreenAnalyser {

  private final static Logger log = LoggerFactory.getLogger(ScreenAnalyser.class);

  private int expectedWidth = 0;
  private int expectedHeight = 0;

  private Map<PixelLocation, Coordinate> pixelMap;

  /**
   * <p>
   * Identifies the screen in the given image. If possible it will perform an
   * 'exact' match, meaning that it all the primary pixels specified in
   * {@link Screen} were within range. An exact match is very accurate, however
   * sometimes an exact match is not possible due to effects (eg partial
   * effects) or obstructing objects (eg a card being dragged over certain
   * pixels) so a partial match is performed. If a partial match doesn't
   * identify a screen with high enough confidence, no screen is returned.
   * </p>
   * <p>
   * It is considered normal that some screens can't be identified. Only the
   * screens that indicate important events have been defined in Screen so far;
   * some Hearthstone screens can't be identified but this is silently ignored
   * by the HearthstoneAnalyser which expects some unknown screens.
   * </p>
   * 
   * @param image
   *          The image to identify a Hearthstone screen from.
   * @param previousScreen
   *          The last screen that was identified; optional, but specify this to
   *          narrow down the search, reduce the risk of false positives (eg
   *          jumping out of a game unexpectedly) and generally make the
   *          analysis faster.
   * @return The Screen that was identified, or null if no screen could be
   *         identified with reasonable confidence.
   */
  public Screen identifyScreen(BufferedImage image, Screen previousScreen) {

    log.debug("Identifying screen");

    if (expectedWidth != image.getWidth() || expectedHeight != image.getHeight()) {
      pixelMap = calculatePixelPositions(image.getWidth(), image.getHeight());
      expectedWidth = image.getWidth();
      expectedHeight = image.getHeight();
    }

    // If we have a previous screen, check only those screens which follow from
    // this one
    EnumSet<Screen> possibleScreens;
    if (previousScreen == null) {
      possibleScreens = EnumSet.allOf(Screen.class);
    } else {
      possibleScreens = previousScreen.nextScreens;
      if (possibleScreens.size() == 0) {
        throw new IllegalStateException("Unable to identify screen because previous screen "
            + previousScreen + " has no nextScreens parameter");
      }
    }

    Screen match = null;

    // Try to perform and exact match on the screen we were last on -- it's the
    // most likely one to match, of course!
    if (previousScreen != null) {
      if (checkForExactMatch(image, previousScreen)) {
        // This screen matches
        log.debug("Exact match on previous screen {}", previousScreen);
        match = previousScreen;
      }
    }

    // Try to find an exact match for the screens, based on the primary pixels
    // only
    if (match == null) {
      for (Screen screen : possibleScreens) {
        if (checkForExactMatch(image, screen)) {
          // This screen matches
          if (log.isDebugEnabled()) {
            if (match == null) {
              log.debug("Exact match on new screen {}", screen);
            } else {
              log.warn(
                  "More that one screen matched! Matched screen {}, but have already matched {}",
                  screen, match);
            }
          }

          match = screen;

          // If not running in debug mode, we can skip the rest of the loop for
          // efficiency
          if (!log.isDebugEnabled())
            break;
        }
      }
    }

    if (match == null) {
      // A check of the primary pixels did not find an exact match, so try for a
      // partial match
      log.debug("Did not find exact screen match, attempting partial match");

      Map<Screen, PartialResult> screenMatchesMap = new HashMap<>();
      int maxMatchedCount = 0;
      int maxUnmatchedCount = 0;
      Screen bestMatch = null;

      EnumSet<Screen> possibleScreensIncludingPrevious = EnumSet.copyOf(possibleScreens);
      if (previousScreen != null) {
        possibleScreensIncludingPrevious.add(previousScreen);
      }

      for (Screen screen : possibleScreensIncludingPrevious) {
        PartialResult partialResult = checkForPartialMatch(image, screen);

        if (partialResult.matchedCount >= maxMatchedCount) {
          maxMatchedCount = partialResult.matchedCount;
          bestMatch = screen;
        }
        if (partialResult.unmatchedCount > maxUnmatchedCount) {
          maxUnmatchedCount = partialResult.unmatchedCount;
        }

        log.debug("Test of screen {} matched={} unmatched={}", screen, partialResult.matchedCount,
            partialResult.unmatchedCount);
        screenMatchesMap.put(screen, partialResult);
      }

      // A partial match is defined as the screen that:
      // - has no more than two pixels unmatched
      // - has more matched pixels than any other screen
      // - has fewer unmatched pixels than any other screen
      assert (bestMatch != null);

      PartialResult bestMatchResult = screenMatchesMap.get(bestMatch);
      boolean acceptBestMatch = true;

      if (bestMatchResult.unmatchedCount > 2) {
        log.debug("Partial match failed because best match {} has {} unmatched pixels", bestMatch,
            bestMatchResult.unmatchedCount);
        acceptBestMatch = false;
      } else {
        // Check whether other screens are too close to the best-matched screen,
        // but ignore any screens considered to be equivalent (ie the playing
        // screen for each board is considered equivalent)
        ScreenGroup ignoreGroup;
        if (bestMatch.group == ScreenGroup.MATCH_PLAYING
            || bestMatch.group == ScreenGroup.MATCH_END) {
          ignoreGroup = bestMatch.group;
        } else {
          ignoreGroup = null;
        }
        for (Screen screen : possibleScreens) {
          if (screen != bestMatch && (ignoreGroup == null || screen.group != ignoreGroup)) {
            // This screen is not the best match, and it's not from the same
            // group (for those groups considered equivalent) so we need to
            // ensure it's not too close to the best match
            PartialResult currentResult = screenMatchesMap.get(screen);
            if (bestMatchResult.matchedCount <= currentResult.matchedCount) {
              log.debug(
                  "Partial match failed because best match {} has {} matched pixels whereas {} has {}",
                  bestMatch, bestMatchResult.matchedCount, screen, currentResult.matchedCount);
              acceptBestMatch = false;
              break;
            } else if (bestMatchResult.unmatchedCount >= currentResult.unmatchedCount) {
              log.debug(
                  "Partial match failed because best match {} has {} unmatched pixels whereas {} has {}",
                  bestMatch, bestMatchResult.unmatchedCount, screen, currentResult.unmatchedCount);
              acceptBestMatch = false;
              break;
            }
          }
        }
      }

      if (acceptBestMatch) {
        log.debug("Partial match on screen {}", bestMatch);
        match = bestMatch;
      }
    }

    return match;
  }

  /**
   * Calculates the relative positions of our standard pixel locations given the
   * specified screen width and height. Hearthstone can run in many different
   * screen sizes so all pixel locations need to be adjusted accordingly.
   * 
   * @param width
   *          the screen width to calculate positions for
   * @param height
   *          the screen height to calculate positions for
   */
  Map<PixelLocation, Coordinate> calculatePixelPositions(int width, int height) {

    log.debug("Recalculating pixel position for width {} height {}", width, height);

    Map<PixelLocation, Coordinate> result;

    if (width == PixelLocation.REFERENCE_SIZE.x() && height == PixelLocation.REFERENCE_SIZE.y()) {
      // The screen size is exactly what our reference pixels are based on, so
      // we can use their coordinates directly
      result = new HashMap<>();
      for (PixelLocation pixelLocation : PixelLocation.values()) {
        Coordinate coordinate = new Coordinate(pixelLocation.x(), pixelLocation.y());
        log.debug("Stored position of {} as {}", pixelLocation, coordinate);
        result.put(pixelLocation, coordinate);
      }

    } else {
      // The screen size is different to our reference pixels, so coordinates
      // need to be adjusted
      float ratioX = (float) width / (float) PixelLocation.REFERENCE_SIZE.x();
      float ratioY = (float) height / (float) PixelLocation.REFERENCE_SIZE.y();
      // ratioY is normally the correct ratio to use, but occasionally ratioX is
      // smaller (usually during screen resizing?)
      float ratio = Math.min(ratioX, ratioY);
      float screenRatio = (float) width / (float) height;

      int xOffset;
      if (screenRatio > 1.4) {
        xOffset = (int) (((float) width - (ratio * PixelLocation.REFERENCE_SIZE.x())) / 2);
      } else {
        xOffset = 0;
      }

      log.debug("ratio={} screenRatio={}, xOffset={}", ratio, screenRatio, xOffset);

      result = new HashMap<>();
      for (PixelLocation pixelLocation : PixelLocation.values()) {
        int x = (int) (pixelLocation.x() * ratio) + xOffset;
        int y = (int) (pixelLocation.y() * ratio);
        Coordinate coordinate = new Coordinate(x, y);
        log.debug("Calculated position of {} as {}", pixelLocation, coordinate);
        result.put(pixelLocation, coordinate);
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  EnumSet<Screen>[] matchScreensForTesting(BufferedImage image) {

    if (expectedWidth != image.getWidth() || expectedHeight != image.getHeight()) {
      pixelMap = calculatePixelPositions(image.getWidth(), image.getHeight());
      expectedWidth = image.getWidth();
      expectedHeight = image.getHeight();
    }

    EnumSet<Screen> primaryMatches = EnumSet.noneOf(Screen.class);
    EnumSet<Screen> secondaryMatches = EnumSet.noneOf(Screen.class);

    for (Screen screen : Screen.values()) {
      if (checkForExactMatch(image, screen)) {
        primaryMatches.add(screen);
        if (checkForMatchSecondary(image, screen)) {
          secondaryMatches.add(screen);
        }
      }
    }

    return new EnumSet[] { primaryMatches, secondaryMatches };
  }

  boolean checkForExactMatch(BufferedImage image, Screen screen) {

    // Skip screens that haven't yet been defined
    if (screen.primary.size() == 0) {
      return false;
    }

    for (Pixel pixel : screen.primary) {
      Coordinate coordinate = pixelMap.get(pixel.pixelLocation);
      int x = coordinate.x();
      int y = coordinate.y();

      int rgb = image.getRGB(x, y);
      int red = (rgb >> 16) & 0xFF;
      int green = (rgb >> 8) & 0xFF;
      int blue = (rgb & 0xFF);

      if (red < pixel.minRed || red > pixel.maxRed || green < pixel.minGreen
          || green > pixel.maxGreen || blue < pixel.minBlue || blue > pixel.maxBlue) {
        // This pixel is outside the expected range
        return false;
      }
    }

    // All pixels matched
    return true;
  }

  PartialResult checkForPartialMatch(BufferedImage image, Screen screen) {

    // Skip screens that haven't yet been defined
    if (screen.primary.size() == 0) {
      return new PartialResult(0, 0);
    }

    int matchedCount = 0;
    int unmatchedCount = 0;

    for (Pixel pixel : screen.primaryAndSecondary) {
      Coordinate coordinate = pixelMap.get(pixel.pixelLocation);
      int x = coordinate.x();
      int y = coordinate.y();

      int rgb = image.getRGB(x, y);
      int red = (rgb >> 16) & 0xFF;
      int green = (rgb >> 8) & 0xFF;
      int blue = (rgb & 0xFF);

      if (red < pixel.minRed || red > pixel.maxRed || green < pixel.minGreen
          || green > pixel.maxGreen || blue < pixel.minBlue || blue > pixel.maxBlue) {
        // This pixel is outside the expected range: it's not a match
        unmatchedCount++;
      } else {
        // This pixel is inside the expected range: it's a match
        matchedCount++;
      }
    }

    return new PartialResult(matchedCount, unmatchedCount);
  }

  boolean checkForMatchSecondary(BufferedImage image, Screen screen) {

    // Skip screens that haven't yet been defined
    if (screen.primary.size() == 0) {
      return false;
    }

    for (Pixel pixel : screen.secondary) {
      Coordinate coordinate = pixelMap.get(pixel.pixelLocation);
      int x = coordinate.x();
      int y = coordinate.y();

      int rgb = image.getRGB(x, y);
      int red = (rgb >> 16) & 0xFF;
      int green = (rgb >> 8) & 0xFF;
      int blue = (rgb & 0xFF);

      if (red < pixel.minRed || red > pixel.maxRed || green < pixel.minGreen
          || green > pixel.maxGreen || blue < pixel.minBlue || blue > pixel.maxBlue) {
        // This pixel is outside the expected range
        return false;
      }
    }

    // All pixels matched
    return true;
  }

  class PartialResult {
    final int matchedCount;
    final int unmatchedCount;

    PartialResult(int matchedCount, int unmatchedCount) {
      this.matchedCount = matchedCount;
      this.unmatchedCount = unmatchedCount;
    }
  }

}
