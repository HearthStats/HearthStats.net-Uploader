package net.hearthstats.game.ocr;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import net.hearthstats.game.ScreenConfig;
import net.sourceforge.tess4j.TessAPI;

import org.apache.commons.lang3.StringUtils;

public class RankLevelOcr extends OcrBase {

  public Integer processNumber(BufferedImage bufferedImage) throws OcrException {
    String result = process(bufferedImage);

    try {
      if (StringUtils.isEmpty(result)) {
        return null;
      } else {
        int rank = Integer.valueOf(result);
        if (rank < 0 || rank > 25) {
          debugLog.debug("Rank {} is invalid, rank detection has failed");
          return null;
        } else {
          return rank;
        }
      }
    } catch (NumberFormatException e) {
      debugLog.debug("Ignoring NumberFormatException parsing " + result);
      return null;
    }
  }

  @Override
  protected BufferedImage crop(BufferedImage image, int iteration) {
    debugLog.debug("rank detection try #" + (iteration + 1));

    float ratio = ScreenConfig.getRatio(image);
    int xOffset = ScreenConfig.getXOffset(image, ratio);

    int retryOffset = (iteration - 1) % 3;
    int x = (int) ((1369 + retryOffset) * ratio + xOffset);
    int y = (int) (250 * ratio);
    int width = (int) (52 * ratio);
    int height = (int) (38 * ratio);

    return image.getSubimage(x, y, width, height);
  }

  @Override
  protected String parseString(String input, int iteration) {
    if (input == null) {
      return null;
    } else {
      // Change easily-mistaken letters into numbers
      String output = StringUtils.replaceChars(input, "lIiSsOo", "1115500");
      // Remove all other unknown letters
      output = output.replaceAll("[^\\d]", "");

      debugLog.debug("Parse of rank \"{}\" is \"{}\"", input, output);
      return output;
    }
  }

  @Override
  protected boolean tryProcessingAgain(String ocrResult, int iteration) {

    try {
      if (ocrResult != null && !ocrResult.isEmpty() && Integer.parseInt(ocrResult) > 0
          && Integer.parseInt(ocrResult) < 26) {
        // A valid rank number has been found
        return false;
      }
    } catch (NumberFormatException e) {
      debugLog.debug("Ignoring NumberFormatException parsing " + ocrResult);
    }

    if (iteration <= 5) {
      // Rank level needs multiple iterations to try slightly shifted each time,
      // so try five times
      return true;
    } else {
      // Hasn't matched after five tries, so give up
      return false;
    }
  }

  @Override
  protected BufferedImage filter(BufferedImage image, int iteration) throws OcrException {
    int width = image.getWidth();
    int height = image.getHeight();
    int bigWidth = width * 2;
    int bigHeight = height * 2;

    // Extract only the black & white parts of the image, removing any coloured
    // parts. This results in just the
    // rank number being left... all the background magically disappears.
    // Note that this change is being done directly on the source image for
    // efficiency, which is OK because the
    // source image is thrown out. However if the caller needs to reuse the
    // source image then the following code
    // should be changed to work on a copy of the image instead of the original.
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Get the individual components of this pixel
        int inputRgba = image.getRGB(x, y);
        int red = (inputRgba >> 16) & 0xFF;
        int green = (inputRgba >> 8) & 0xFF;
        int blue = (inputRgba >> 0) & 0xFF;

        // If the pixel is coloured, set it to white instead
        int invertedGatedLevel;
        if (Math.abs(red - green) > 3 || Math.abs(red - blue) > 3 || Math.abs(green - blue) > 3) {
          invertedGatedLevel = 255;
        } else {
          invertedGatedLevel = 255 - blue;
        }

        // Replace the pixel with the new value
        int outputRgba = ((255 & 0xFF) << 24) | ((invertedGatedLevel & 0xFF) << 16)
            | ((invertedGatedLevel & 0xFF) << 8) | ((invertedGatedLevel & 0xFF) << 0);
        image.setRGB(x, y, outputRgba);
      }
    }

    // blow it up for ocr
    BufferedImage newImage = new BufferedImage(bigWidth, bigHeight, BufferedImage.TYPE_INT_RGB);
    Graphics g = newImage.createGraphics();
    g.drawImage(image, 0, 0, bigWidth, bigHeight, null);
    g.dispose();

    return newImage;
  }

  @Override
  protected String getFilename() {
    return "ranklevel";
  }

  @Override
  protected int getTesseractPageSegMode(int iteration) {
    return TessAPI.TessPageSegMode.PSM_SINGLE_WORD;
  }
}
