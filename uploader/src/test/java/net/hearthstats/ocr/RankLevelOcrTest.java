package net.hearthstats.ocr;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import junit.framework.Assert;
import net.hearthstats.Main;

import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankLevelOcrTest {

    private final static Logger debugLog = LoggerFactory.getLogger(RankLevelOcrTest.class);

    private static RankLevelOcr rankLevelOcr;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Ensure that Tesseract has its training data available
        // TODO: Determine an cross-platform way to supply extracted Tesseract training data for this setup
        //Main.setupTesseract();

        // Set up a single OCR instance for all image tests
        rankLevelOcr = new RankLevelOcr();
    }

	// @Test
    public void testParseString() throws Exception {
        Assert.assertEquals("17", rankLevelOcr.parseString("i7", 0));
        Assert.assertEquals("15", rankLevelOcr.parseString("is", 0));
        Assert.assertEquals("10", rankLevelOcr.parseString("I0", 0));
        Assert.assertEquals("10", rankLevelOcr.parseString("IO", 0));
        Assert.assertEquals("5", rankLevelOcr.parseString("S", 0));
    }

	// @Test
    public void testPerformOcr() throws Exception {

        testRankLevel("ranklevel-17-a.png", "17");
        testRankLevel("ranklevel-18-a.png", "18");
        testRankLevel("ranklevel-18-b.png", "18");
        testRankLevel("ranklevel-19-a.png", "19");
        testRankLevel("ranklevel-19-b.png", "19");
        testRankLevel("ranklevel-20-a.png", "20");
        testRankLevel("ranklevel-21-a.png", "21");
        testRankLevel("ranklevel-22-a.png", "22");

    }

    private void testRankLevel(String filename, String expectedResult) throws Exception {
        URL res = OpponentNameOcrTest.class.getResource("/images/ranklevel/" + filename);
        BufferedImage bufferedImage = ImageIO.read(res);
        String rawResult = rankLevelOcr.performOcr(bufferedImage, 0);
        bufferedImage.flush();

        String parsedResult = rankLevelOcr.parseString(rawResult, 0);

        debugLog.debug("OCR for {} found raw \"{}\" parsed \"{}\"", filename, rawResult, parsedResult);

        org.junit.Assert.assertEquals("OCR of rank in " + filename + " did not match expected rank", expectedResult, parsedResult);
    }

}
