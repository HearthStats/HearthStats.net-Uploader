package net.hearthstats.ocr;

import net.hearthstats.analysis.HearthstoneAnalyser;
import net.sourceforge.tess4j.TessAPI;

import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * Performs OCR on images that contain a deck name in the top-right corner of the screen.
 */
public class DeckNameOcr extends OcrBase {

  @Override
  protected BufferedImage crop(BufferedImage image, int iteration) {
    float ratio = HearthstoneAnalyser.getRatio(image);

    int x = (int) (1256 * ratio);
    int y = (int) (65 * ratio);
    int width = (int) (262 * ratio);
    int height = (int) (34 * ratio);

    return image.getSubimage(x, y, width, height);
  }


  @Override
  protected String parseString(String ocrResult, int iteration) {
    // Opponent names could be just about anything, so only trim spaces at the start and end
    return ocrResult == null ? "" : ocrResult.trim();
  }

  @Override
  protected boolean tryProcessingAgain(String ocrResult, int iteration) {
    // Only try processing the deck name once
    return false;
  }

  @Override
  protected String getFilename() {
    return "deckname";
  }

  @Override
  protected BufferedImage filter(BufferedImage image, int iteration) throws OcrException {
    int width = image.getWidth();
    int height = image.getHeight();
    int bigWidth = width * 2;
    int bigHeight = height * 2;

    // Extract only the black & white parts of the image, removing any coloured parts. This results in just the
    // rank number being left... all the background magically disappears.
    // Note that this change is being done directly on the source image for efficiency, which is OK because the
    // source image is thrown out. However if the caller needs to reuse the source image then the following code
    // should be changed to work on a copy of the image instead of the original.
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Get the individual components of this pixel
        int inputRgba = image.getRGB(x, y);
        int red = (inputRgba >> 16) & 0xFF;
        int green = (inputRgba >> 8) & 0xFF;
        int blue = (inputRgba >> 0) & 0xFF;

        int invertedGatedLevel;
        if (Math.abs(red - green) > 3 || Math.abs(red - blue) > 3 || Math.abs(green - blue) > 3) {
          // If the pixel is coloured, set it to white instead
          invertedGatedLevel = 255;
        } else if (red < 64) {
          // If the pixel is dark, it is background so set it to white instead
          invertedGatedLevel = 255;
        } else if (skipPixel(x, y, red)) {
          // This is a problematic pixel on a priest or mage background, so erase it
          invertedGatedLevel = 255;
        } else {
          invertedGatedLevel = 255 - blue;
        }

        // Replace the pixel with the new value
        int outputRgba = ((255 & 0xFF) << 24) |
          ((invertedGatedLevel & 0xFF) << 16) |
          ((invertedGatedLevel & 0xFF) << 8)  |
          ((invertedGatedLevel & 0xFF) << 0);
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

  private boolean skipPixel(int x, int y, int red) {
    if (x < 170 || x > 193) {
      return false;
    }
    // The following is a list of known bad pixels that incorrectly get interpreted as text
    return
      // Priest
         (x == 170 && y == 0 && red > 135 && red < 143)
      || (x == 172 && y == 2  && red > 135 && red < 143)
      || (x == 176 && y == 1  && red > 140 && red < 150)
      || (x == 176 && y == 2  && red > 144 && red < 154)
      || (x == 177 && y == 3  && red > 142 && red < 152)
      || (x == 177 && y == 5  && red > 140 && red < 150)
      || (x == 177 && y == 7  && red > 142 && red < 152)
      || (x == 178 && y == 7  && red > 138 && red < 148)
      || (x == 178 && y == 10 && red > 132 && red < 142)
      || (x == 179 && y == 7  && red > 136 && red < 146)
      || (x == 179 && y == 10 && red > 140 && red < 150)
      || (x == 180 && y == 5  && red > 146 && red < 156)
      || (x == 180 && y == 7  && red > 140 && red < 150)
      || (x == 180 && y == 9  && red > 141 && red < 151)
      || (x == 181 && y == 5  && red > 145 && red < 155)
      || (x == 181 && y == 6  && red > 146 && red < 156)
      || (x == 181 && y == 8  && red > 149 && red < 159)
      || (x == 185 && y == 7  && red > 143 && red < 153)
      || (x == 192 && y == 2  && red > 157 && red < 167)
      || (x == 193 && y == 20 && red > 144 && red < 154)
      // Mage
      || (x == 175 && y == 30 && red > 119 && red < 129)
      || (x == 176 && y == 29 && red > 150 && red < 160)
      || (x == 176 && y == 30 && red >  93 && red < 103)
      || (x == 177 && y == 28 && red > 138 && red < 148)
      || (x == 178 && y == 28 && red > 145 && red < 155)
      || (x == 179 && y == 28 && red > 145 && red < 155)
      || (x == 179 && y == 29 && red > 126 && red < 136)
      || (x == 180 && y == 28 && red > 145 && red < 155)
      || (x == 181 && y == 28 && red > 147 && red < 157)
      ;
  }


  @Override
  protected int getTesseractPageSegMode(int iteration) {
    return TessAPI.TessPageSegMode.PSM_SINGLE_LINE;
  }
}

