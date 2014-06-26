package net.hearthstats.ocr;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import net.hearthstats.Main;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // TODO: Determine an cross-platform way to supply extracted Tesseract training data for this setup
        //Main.setupTesseract();

        // Set up a single OCR instance for all image tests
        opponentNameOcr = new OpponentNameOcr() {
            @Override
            protected BufferedImage crop(BufferedImage image, int iteration) {
                throw new NotImplementedException("Test images are pre-cropped, no need to implement this crop method");
            }
        };
    }


	// @Test
    public void testPerformOcr() throws Exception {
        debugLog.info("Performing OCR test on opponent names");
        testOpponentName("ADIZZLE23");
        testOpponentName("ADustCube");
        testOpponentName("Atlas");
        testOpponentName("Baitt");
        testOpponentName("Besant");
        testOpponentName("Blackvein666");
        testOpponentName("Blood");
        testOpponentName("BORTHOX");
        testOpponentName("CABJ18");
        testOpponentName("Calisc");
        testOpponentName("Clarendon");
        testOpponentName("Cyberdyne");
        testOpponentName("Cygnus");
        testOpponentName("Dada");
        testOpponentName("danutsio");
        testOpponentName("Darn");
        testOpponentName("Djoosah");
        testOpponentName("dontellu");
        testOpponentName("Ehamar");
        testOpponentName("EternalVoid");
        testOpponentName("Fauzy");
        testOpponentName("Fiizz01");
        testOpponentName("ForTheHorde");
        testOpponentName("Freewilly");
        testOpponentName("FREGON989");
        testOpponentName("Fritcheous");
        testOpponentName("HaPPoSSai");
        testOpponentName("Highlander");
        testOpponentName("humdeabril");
        testOpponentName("Icekicker");
        testOpponentName("Kazbec");
        testOpponentName("KeL");
        testOpponentName("Kritterkilla");
        testOpponentName("Lionheart");
        testOpponentName("macroberts");
        testOpponentName("Manic");
        testOpponentName("Mka");
        testOpponentName("MKDjanes");
        testOpponentName("NeilPogi");
        testOpponentName("Numenor");
        testOpponentName("Oreo");
        testOpponentName("Pleasurepack");
        testOpponentName("r2d2");
        testOpponentName("Ransux");
        testOpponentName("redrebel");
        testOpponentName("Renflex");
        testOpponentName("Rofltauren");
        testOpponentName("shobot");
        testOpponentName("SilentPanda");
        testOpponentName("Sisko");
        testOpponentName("Skorn57");
        testOpponentName("sLimer");
        testOpponentName("spacecow");
        testOpponentName("StarK");
        testOpponentName("Tando");
        testOpponentName("Toughnut");
        testOpponentName("tubatim32");
        testOpponentName("UrFather");
        testOpponentName("Veon");
        testOpponentName("yigg");
        testOpponentName("YouInTheNuts");
        testOpponentName("zeus");
    }


    private void testOpponentName(String name) throws Exception {
        URL res = OpponentNameOcrTest.class.getResource("/images/opponentname/opponentname-" + name + ".png");
        BufferedImage bufferedImage = ImageIO.read(res);
        String result = opponentNameOcr.performOcr(bufferedImage, 0);
        bufferedImage.flush();

        debugLog.info("OCR for {} found \"{}\"", name, result);

        Assert.assertEquals("OCR of opponent name did not match expected name", name, result);
    }

}
