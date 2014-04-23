package net.hearthstats.ocr;

import junit.framework.Assert;
import net.hearthstats.Main;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Created by charlie on 23/04/2014.
 */
public class RankLevelOcrTest {

    private final static Logger debugLog = LoggerFactory.getLogger(RankLevelOcrTest.class);

    private static RankLevelOcr rankLevelOcr;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Ensure that Tesseract has its training data available
        Main.setupTesseract();

        // Set up a single OCR instance for all image tests
        rankLevelOcr = new RankLevelOcr();
    }

    @Test
    public void testParseString() throws Exception {
        Assert.assertEquals("17", rankLevelOcr.parseString("i7", 0));
        Assert.assertEquals("15", rankLevelOcr.parseString("is", 0));
        Assert.assertEquals("10", rankLevelOcr.parseString("I0", 0));
        Assert.assertEquals("10", rankLevelOcr.parseString("IO", 0));
        Assert.assertEquals("5", rankLevelOcr.parseString("S", 0));
    }

    @Test
    public void testPerformOcr() throws Exception {

        testRankLevel("ranklevel-16-a.png", "16");
        testRankLevel("ranklevel-17-a.png", "17");

    }

    private void testRankLevel(String filename, String expectedResult) throws Exception {
        URL res = OpponentNameOcrTest.class.getResource("/images/ranklevel/" + filename);
        BufferedImage bufferedImage = ImageIO.read(res);
        String rawResult = rankLevelOcr.performOcr(bufferedImage, 0);
        bufferedImage.flush();

        String parsedResult = rankLevelOcr.parseString(rawResult, 0);

        debugLog.debug("OCR for {} found raw \"{}\" parsed \"{}\"", filename, rawResult, parsedResult);

        org.junit.Assert.assertEquals("OCR of opponent name did not match expected name", expectedResult, parsedResult);
    }

}
