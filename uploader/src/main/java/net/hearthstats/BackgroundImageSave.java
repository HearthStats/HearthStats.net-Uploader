package net.hearthstats;

import net.hearthstats.analysis.HearthstoneAnalyser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Saves images on a background thread so that detection isn't held up by a slow filesystem.
 *
 * @author gtch
 */
public class BackgroundImageSave {

    private final static Logger debugLog = LoggerFactory.getLogger(BackgroundImageSave.class);

    private static final ExecutorService backgroundThreadExecutor = Executors.newSingleThreadExecutor();


    /**
     * Saves a PNG image in the temp/extraction folder, doing so on a background thread so that detection isn't held up.
     *
     * @param image The image to save. You should not make any further changes to this image on the main thread after
     *              submitting it to this method, otherwise the save file will have unpredictable contents.
     * @param filename The name of the file to save. Do not include the extension (.png is added automatically)
     */
    public static void savePngImage(final BufferedImage image, final String filename) {
        debugLog.debug("savePngImage({}, {})", image == null ? null : image.getWidth(), filename);
        if (image == null) {
            debugLog.warn("Could not save image {} because image is null", filename);
            return;
        } else if (StringUtils.isBlank(filename)) {
            debugLog.warn("Could not save image because filename is blank");
            return;
        }

        backgroundThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    File outputfile = new File(OldConfig.getExtractionFolder() + "/" + filename + ".png");
                    ImageIO.write(image, "png", outputfile);
                    debugLog.debug("Successfully saved image " + outputfile.getAbsolutePath());
                } catch (Exception e) {
                    debugLog.warn("Error saving image " + filename, e);
                }

                if (image != null) {
                    image.flush();
                }
            }
        });

    }


    /**
     * <p>Saves a PNG image in the temp/extraction folder, cropped as specified. Ths image is saved on a background thread
     * so that detection isn't held up.</p>
     * <p>The code assumes that it has been given a full-size Hearthstone screenshot, and so the position of the crop
     * is adjusted to the relative position for the actual screen in the image. In other words, if the screenshot is
     * higher resolution or lower resolution than the reference 1600x1200 size then the crop is moved accordingly.</p>
     *
     * @param image The image to save. You should not make any further changes to this image on the main thread after
     *              submitting it to this method, otherwise the save file will have unpredictable contents.
     * @param filename The name of the file to save. Do not include the extension (.png is added automatically)
     * @param x the X coordinate of the upper-left corner of the crop, relative to a 1600-pixel wide screen
     * @param y the Y coordinate of the upper-left corner of the crop, relative to a 1200-pixel high screen
     * @param w the width of the specified rectangular region, relative to a 1600-pixel wide screen
     * @param h the height of the specified rectangular region, relative to a 1200-pixel high screen
     */
    public static void saveCroppedPngImage(final BufferedImage image, final String filename, int x, int y, int w, int h) {
        float ratio = HearthstoneAnalyser.getRatio(image);
        int xOffset = HearthstoneAnalyser.getXOffset(image, ratio);

        int relativeX = (int) (x * ratio + xOffset);
        int relativeY = (int) (y * ratio);
        int relativeWidth = (int) (w * ratio);
        int relativeHeight = (int) (h * ratio);

        debugLog.debug("Cropping image to x={} y={} width={} height={}", relativeX, relativeY, relativeWidth, relativeHeight);

        BufferedImage croppedImage = image.getSubimage(relativeX, relativeY, relativeWidth, relativeHeight);
        savePngImage(croppedImage, filename);
    }

}
