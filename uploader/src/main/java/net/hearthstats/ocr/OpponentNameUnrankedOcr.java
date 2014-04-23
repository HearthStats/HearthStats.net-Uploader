package net.hearthstats.ocr;

import net.hearthstats.analysis.HearthstoneAnalyser;

import java.awt.image.BufferedImage;

/**
 * Performs OCR on images that contain the opponent name, while playing in an unranked mode (ie casual or arena).
 *
 */
public class OpponentNameUnrankedOcr extends OpponentNameOcr {

    @Override
    protected BufferedImage crop(BufferedImage image, int iteration) {

        float ratio = HearthstoneAnalyser.getRatio(image);

        int x = (int) (6 * ratio);
        int y = (int) (34 * ratio);
        int width = (int) (150 * ratio);
        int height = (int) (19 * ratio);

        return image.getSubimage(x, y, width, height);
    }

}
