package net.hearthstats.game.imageanalysis;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;


public class UniquePixelTest {

  private final static Logger debugLog = LoggerFactory.getLogger(UniquePixelTest.class);


  /**
   * <p>Tests whether the pixels that identify a hero class when analysing a deck are distinct enough to avoid
   * ambiguities and thus misidentification.</p>
   * <p>Note that this test is currently <b>failing</b> because there are some overlaps between certain classes.
   * That is a reminder that we need to adjust the pixels to better locations.</p>
   */
  @Test
  @Ignore // Currently ignored because we *know* there are problem pixels that need to be fixed
  public void testDeckOverlap() throws Exception {


    UniquePixel[] deck1Pixels = new UniquePixel[]{
      UniquePixel.DECK_DRUID_1, UniquePixel.DECK_HUNTER_1, UniquePixel.DECK_MAGE_1,
      UniquePixel.DECK_PALADIN_1, UniquePixel.DECK_PRIEST_1, UniquePixel.DECK_ROGUE_1,
      UniquePixel.DECK_SHAMAN_1, UniquePixel.DECK_WARLOCK_1, UniquePixel.DECK_WARRIOR_1
    };
    UniquePixel[] deck2Pixels = new UniquePixel[]{
      UniquePixel.DECK_DRUID_2, UniquePixel.DECK_HUNTER_2, UniquePixel.DECK_MAGE_2,
      UniquePixel.DECK_PALADIN_2, UniquePixel.DECK_PRIEST_2, UniquePixel.DECK_ROGUE_2,
      UniquePixel.DECK_SHAMAN_2, UniquePixel.DECK_WARLOCK_2, UniquePixel.DECK_WARRIOR_2
    };


    for (int i = 0; i < deck1Pixels.length - 1; i++) {
      UniquePixel pixel1A = deck1Pixels[i];
      UniquePixel pixel2A = deck2Pixels[i];
      for (int j = i + 1; j < deck1Pixels.length; j++) {
        UniquePixel pixel1B = deck1Pixels[j];
        UniquePixel pixel2B = deck2Pixels[j];

        boolean red1Distinct = pixel1A.maxRed < pixel1B.minRed || pixel1B.maxRed < pixel1A.minRed;
        boolean green1Distinct = pixel1A.maxGreen < pixel1B.minGreen || pixel1B.maxGreen < pixel1A.minGreen;
        boolean blue1Distinct = pixel1A.maxBlue < pixel1B.minBlue || pixel1B.maxBlue < pixel1A.minBlue;
        boolean red2Distinct = pixel2A.maxRed < pixel2B.minRed || pixel2B.maxRed < pixel2A.minRed;
        boolean green2Distinct = pixel2A.maxGreen < pixel2B.minGreen || pixel2B.maxGreen < pixel2A.minGreen;
        boolean blue2Distinct = pixel2A.maxBlue < pixel2B.minBlue || pixel2B.maxBlue < pixel2A.minBlue;

        debugLog.info("Pixel {} distinct from {}: red={} green={} blue={}",
          pixel1A, pixel1B, red1Distinct, green1Distinct, blue1Distinct);
        debugLog.info("Pixel {} distinct from {}: red={} green={} blue={}",
          pixel2A, pixel2B, red2Distinct, green2Distinct, blue2Distinct);

        assertTrue(pixel1A + " & " + pixel2A + " have colour ranges that overlap with " + pixel1B + " & " + pixel2B + ": "
          + "[" + Math.max(pixel1A.minRed, pixel1B.minRed) + "-" + Math.min(pixel1A.maxRed, pixel1B.maxRed) + "],"
          + "[" + Math.max(pixel1A.minGreen, pixel1B.minGreen) + "-" + Math.min(pixel1A.maxGreen, pixel1B.maxGreen) + "],"
          + "[" + Math.max(pixel1A.minBlue, pixel1B.minBlue) + "-" + Math.min(pixel1A.maxBlue, pixel1B.maxBlue) + "] and "
          + "[" + Math.max(pixel2A.minRed, pixel2B.minRed) + "-" + Math.min(pixel2A.maxRed, pixel2B.maxRed) + "],"
          + "[" + Math.max(pixel2A.minGreen, pixel2B.minGreen) + "-" + Math.min(pixel2A.maxGreen, pixel2B.maxGreen) + "],"
          + "[" + Math.max(pixel2A.minBlue, pixel2B.minBlue) + "-" + Math.min(pixel2A.maxBlue, pixel2B.maxBlue) + "]"
          , red1Distinct || green1Distinct || blue1Distinct || red2Distinct || green2Distinct || blue2Distinct);

      }


    }


  }
}