package net.hearthstats.ocr;

import net.hearthstats.analysis.HearthstoneAnalyser;

import java.awt.image.BufferedImage;

/**
 * Performs OCR on images that contain the opponent name, while playing in ranked mode.
 * In ranked mode the opponent name is shifted to the right to make room for the rank.
 */
public class OpponentNameRankedOcr extends OpponentNameOcr {

    @Override
    protected BufferedImage crop(BufferedImage image, int iteration) {
        float ratio = HearthstoneAnalyser.getRatio(image);

        int x = (int) (118 * ratio);
        int y = (int) (53 * ratio);
        int width = (int) (236 * ratio);
        int height = (int) (30 * ratio);

        return image.getSubimage(x, y, width, height);
    }

}
