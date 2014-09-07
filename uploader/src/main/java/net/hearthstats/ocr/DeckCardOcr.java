package net.hearthstats.ocr;

import net.sourceforge.tess4j.TessAPI;

import java.awt.image.BufferedImage;


/**
 * OCR for reading card names in a deck.
 */
public class DeckCardOcr extends OcrBase {

  private int cardNo = 0;


  public void setCardNo(int cardNo) {
    this.cardNo = cardNo;
  }


  @Override
  protected BufferedImage crop(BufferedImage image, int iteration) {
    return image;
  }


  @Override
  protected String parseString(String ocrResult, int iteration) {
    // Ignore empty or very small strings, they're too small to be a real card
    if (ocrResult == null || ocrResult.length() <= 3) {
      return "";
    } else {
      return ocrResult.trim();
    }
  }

  @Override
  protected boolean tryProcessingAgain(String ocrResult, int iteration) {
    // Only try processing the card name once
    return false;
  }

  @Override
  protected BufferedImage filter(BufferedImage image, int iteration) throws OcrException {
    int width = image.getWidth();
    int height = image.getHeight();
    int bigWidth = width * 2;
    int bigHeight = height * 2;

    BufferedImage newImage = new BufferedImage(bigWidth, bigHeight, BufferedImage.TYPE_INT_RGB);
    newImage.createGraphics();

    // Extract only the black & white parts of the image, removing any coloured parts. This results in just the
    // card name being left... all of the background *should* magically disappear.

    // First pass: identifies which pixels are coloured, which are black & white
    short[][] levelArray = new short[width][height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Get the individual components of this pixel
        int inputRgba = image.getRGB(x, y);
        int red = (inputRgba >> 16) & 0xFF;
        int green = (inputRgba >> 8) & 0xFF;
        int blue = (inputRgba >> 0) & 0xFF;

        // If the pixel is coloured, exclude it
        if (Math.abs(red - green) > 1 || Math.abs(red - blue) > 1 || Math.abs(green - blue) > 1) {
          levelArray[x][y] = -1;
        } else {
          levelArray[x][y] = (short) red; // (255 - red);
        }
      }
    }

    // Second pass: draws a new image in black & white that excludes all the background
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {

        short level;
        // If any of the neighbouring pixels is excluded, then exclude this pixel too - this avoids orphaned pixels
        if (levelArray[x][y] == -1                        // this pixel is excluded
          || (x > 0 && levelArray[x-1][y] == -1)          // left pixel is excluded
          || (x + 1 < width && levelArray[x+1][y] == -1)  // right pixel is excluded
          || (y > 0 && levelArray[x][y-1] == -1)          // top pixel is excluded
          || (y + 1 < height && levelArray[x][y+1] == -1) // bottom pixel is excluded
          ) {
          level = 255;
        } else {
          level = (short) (255 - levelArray[x][y]);
        }

        int outputRgba = ((255 & 0xFF) << 24) |
          ((level & 0xFF) << 16) |
          ((level & 0xFF) << 8)  |
          ((level & 0xFF) << 0);

        int x2 = x << 1;
        int y2 = y << 1;
        newImage.setRGB(x2, y2, outputRgba);
        newImage.setRGB(x2 + 1, y2, outputRgba);
        newImage.setRGB(x2, y2 + 1, outputRgba);
        newImage.setRGB(x2 + 1, y2 + 1, outputRgba);
      }
    }


    return newImage;
  }


  @Override
  protected String getFilename() {
    return "cardImg-" + cardNo + "-ocr3";
  }

  @Override
  protected int getTesseractPageSegMode(int iteration) {
    return TessAPI.TessPageSegMode.PSM_SINGLE_LINE;
  }
}
