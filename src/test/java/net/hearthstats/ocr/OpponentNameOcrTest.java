package net.hearthstats.ocr;

import net.hearthstats.Main;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Tests whether the opponent name OCR is working as expected by running a number of known names through the OCR
 * and checking if the OCR text is correct.
 */
public class OpponentNameOcrTest {

    private final static Logger debugLog = LoggerFactory.getLogger(OpponentNameOcrTest.class);

    private static OpponentNameOcr opponentNameOcr;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Ensure that Tesseract has its training data available
        Main.setupTesseract();

        // Set up a single OCR instance for all image tests
        opponentNameOcr = new OpponentNameOcr() {
            @Override
            protected BufferedImage crop(BufferedImage image, int iteration) {
                throw new NotImplementedException("Test images are pre-cropped, no need to implement this crop method");
            }
        };
    }


    @Test
    public void testPerformOcr() throws Exception {
        debugLog.info("Performing OCR test on opponent names");
        testOpponentName("opponentname-ADIZZLE23.jpg", "ADIZZLE23");
        testOpponentName("opponentname-BORTHOX.png", "BORTHOX");
        testOpponentName("opponentname-Baitt.jpg", "Baitt");
        testOpponentName("opponentname-Besant.jpg", "Besant");
        testOpponentName("opponentname-CABJ18.jpg", "CABJ18");
        testOpponentName("opponentname-Clarendon.png", "Clarendon");
        testOpponentName("opponentname-Dada.jpg", "Dada");
        testOpponentName("opponentname-danutsio.jpg", "danutsio");
        testOpponentName("opponentname-Darn.png", "Darn");
        testOpponentName("opponentname-Ehamar.png", "Ehamar");
        testOpponentName("opponentname-EternalVoid.png", "EternalVoid");
        testOpponentName("opponentname-FREGON989.png", "FREGON989");
        testOpponentName("opponentname-Fauzy.png", "Fauzy");
        testOpponentName("opponentname-Fiizz01.jpg", "Fiizz01");
        testOpponentName("opponentname-ForTheHorde.jpg", "ForTheHorde");
        testOpponentName("opponentname-Freewilly.png", "Freewilly");
        testOpponentName("opponentname-Fritcheous.png", "Fritcheous");
        testOpponentName("opponentname-Highlander.png", "Highlander");
        testOpponentName("opponentname-humdeabril.jpg", "humdeabril");
        testOpponentName("opponentname-Icekicker.jpg", "Icekicker");
        testOpponentName("opponentname-Kazbec.jpg", "Kazbec");
        testOpponentName("opponentname-Kritterkilla.jpg", "Kritterkilla");
        testOpponentName("opponentname-Manic.png", "Manic");
        testOpponentName("opponentname-Mka.jpg", "Mka");
        testOpponentName("opponentname-NeilPogi.png", "NeilPogi");
        testOpponentName("opponentname-Numenor.jpg", "Numenor");
        testOpponentName("opponentname-redrebel.jpg", "redrebel");
        testOpponentName("opponentname-Renflex.png", "Renflex");
        testOpponentName("opponentname-Skorn57.jpg", "Skorn57");
        testOpponentName("opponentname-sLimer.jpg", "sLimer");
        testOpponentName("opponentname-StarK.jpg", "StarK");
        testOpponentName("opponentname-Veon.jpg", "Veon");
        testOpponentName("opponentname-YouInTheNuts.png", "YouInTheNuts");
        testOpponentName("opponentname-zeus.png", "zeus");
    }


    private void testOpponentName(String filename, String expectedResult) throws Exception {
        URL res = OpponentNameOcrTest.class.getResource("/images/opponentname/" + filename);
        BufferedImage bufferedImage = ImageIO.read(res);
        String result = opponentNameOcr.performOcr(bufferedImage, 0);
        bufferedImage.flush();

        debugLog.debug("OCR for {} found \"{}\"", filename, result);

        Assert.assertEquals("OCR of opponent name did not match expected name", expectedResult, result);
    }

}
