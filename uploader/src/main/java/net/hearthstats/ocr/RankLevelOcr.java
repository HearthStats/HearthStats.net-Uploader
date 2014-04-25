package net.hearthstats.ocr;

import net.hearthstats.analysis.HearthstoneAnalyser;
import org.apache.commons.lang3.StringUtils;

import java.awt.image.BufferedImage;

public class RankLevelOcr extends OcrBase {


    public Integer processNumber(BufferedImage bufferedImage) throws OcrException {
        String result = process(bufferedImage);

        try {
            if (StringUtils.isEmpty(result)) {
                return null;
            } else {
                return Integer.valueOf(result);
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
        int x = (int) ((875 + retryOffset) * ratio + xOffset);
        int y = (int) (162 * ratio);
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
    protected String getFilename() {
        return "ranklevel";
    }
}
