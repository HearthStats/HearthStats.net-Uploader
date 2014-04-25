package net.hearthstats.ocr;

import net.hearthstats.analysis.HearthstoneAnalyser;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

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
        float ratio = HearthstoneAnalyser.getRatio(image);
        int xOffset = HearthstoneAnalyser.getXOffset(image, ratio);

        int retryOffset = (iteration - 1) % 3;
        int x = (int) ((877 + retryOffset) * ratio + xOffset);
        int y = (int) (161 * ratio);
        int width = (int) (32 * ratio);
        int height = (int) (22 * ratio);

        return image.getSubimage(x, y, width, height);
    }


    @Override
    protected String parseString(String ocrResult, int iteration) {
        if (ocrResult != null) {
            // Change easily-mistaken letters into numbers
            ocrResult = StringUtils.replaceChars(ocrResult, "lIiSsOo", "1115500");
            // Remove all other unknown letters
            ocrResult = ocrResult.replaceAll("[^\\d]", "");
        }
        return ocrResult;
    }


    @Override
    protected boolean tryProcessingAgain(String ocrResult, int iteration) {

        try {
            if (ocrResult != null && !ocrResult.isEmpty() && Integer.parseInt(ocrResult) > 0 && Integer.parseInt(ocrResult) < 26) {
                // A valid rank number has been found
                return false;
            }
        } catch (NumberFormatException e) {
            debugLog.debug("Ignoring NumberFormatException parsing " + ocrResult);
        }

        if (iteration <= 5) {
            // Rank level needs multiple iterations to try slightly shifted each time, so try five times
            debugLog.debug("rank detection try #" + iteration);
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
        int bigWidth = width * 3;
        int bigHeight = height * 3;

        // Extract only the black & white parts of the image, removing any coloured parts. This results in just the
        // rank number being left... all the backgroun magically disappears.
        // Note that this change is being done directly on the source image for efficiency, which is OK because the
        // source image is thrown out. However if the caller needs to reuse the source image then the following code
        // should be changed to work on a copy of the image instead of the original.
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgba = image.getRGB(x, y);
                int red = (rgba >> 16) & 0xFF;
                int green = (rgba >> 8) & 0xFF;
                int blue = (rgba >> 0) & 0xFF;

                int invertedGatedLevel;
                if (Math.abs(red - green) > 3 || Math.abs(red - blue) > 3 || Math.abs(green - blue) > 3) {
                    invertedGatedLevel = 255;
                } else {
                    invertedGatedLevel = 255 - blue;
                }

                Color col = new Color(invertedGatedLevel, invertedGatedLevel, invertedGatedLevel);
                image.setRGB(x, y, col.getRGB());
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
}
