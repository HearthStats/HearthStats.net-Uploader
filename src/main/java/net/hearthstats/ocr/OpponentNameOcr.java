package net.hearthstats.ocr;

import net.sourceforge.tess4j.TessAPI;

import java.awt.image.BufferedImage;

/**
 * Performs OCR on images that contain the opponent name.
 * Subclasses adjust the cropping depending on whether this is ranked or unranked mode.
 */
public abstract class OpponentNameOcr extends OcrBase {

    @Override
    protected String parseString(String ocrResult, int iteration) {
        // Opponent names could be just about anything, so only strip out spaces
        return ocrResult == null ? "" : ocrResult.replaceAll("\\s+","");
    }

    @Override
    protected boolean tryProcessingAgain(String ocrResult, int iteration) {
        // Only try processing the opponent name once
        return false;
    }

    @Override
    protected String getFilename() {
        return "opponentname";
    }

    @Override
    protected BufferedImage crop(BufferedImage image, int iteration) {
        return null;
    }

    @Override
    protected int getTesseractPageSegMode(int iteration) {
        return TessAPI.TessPageSegMode.PSM_SINGLE_WORD;
    }
}
