package net.hearthstats.analysis;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.hearthstats.ocr.RankLevelOcr;
import net.hearthstats.state.PixelLocation;
import net.hearthstats.state.Screen;
import net.hearthstats.util.Coordinate;
import net.hearthstats.util.MatchOutcome;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tests the ScreenAnalyser against a folder of screenshots (which you need to provide yourself).
 * It generates a set of HTML files showing each screenshot with information about what screen it matched for that
 * screenshot.
 *
 * @author gtch
 */
public class ScreenAnalyserTest {

    private final static Logger log = LoggerFactory.getLogger(ScreenAnalyserTest.class);

    private final static String IMAGE_PATH = "/tmp/hearthstats";
    private final static int PAGE_SIZE = 25;

    private int expectedWidth = 0;
    private int expectedHeight = 0;

    private Map<PixelLocation, Coordinate> pixelMap;


	// @Test
    public void htmlScreenMatchTest() throws Exception {

        log.debug("Starting check...");

        ScreenAnalyser analyser = new ScreenAnalyser();
        RankLevelOcr rankLevelOcr = new RankLevelOcr();

        File imageFolder = new File(IMAGE_PATH);
        File[] imageArray = imageFolder.listFiles();

        Assert.assertNotNull("No files found in " + IMAGE_PATH + ". Please make sure you've set the path to a folder that contains screenshots from Hearthstone", imageArray);

        List<File> images = new ArrayList<>(imageArray.length);
        for (File image : imageArray) {
            if (image.getName().endsWith(".png")) {
                images.add(image);
            }
        }

        Assert.assertFalse("No images found in " + IMAGE_PATH + ". Please make sure you've set the path to a folder that contains screenshots from Hearthstone", images.size() == 0);

        int page = 0;
        int pageCount = (images.size() / PAGE_SIZE) + 1;

        while (page < pageCount) {
            page++;

            String filename = IMAGE_PATH + "/match-test-" + page + ".html";
            try (BufferedWriter output = new BufferedWriter(new FileWriter(filename))) {

                writeHtmlHeader(output, page, pageCount);

                for (int i = (page - 1) * PAGE_SIZE; i < images.size() && i < page * PAGE_SIZE; i++) {
                    File image = images.get(i);

                    output.write("<tr>" +
                            "<td colspan=\"9\" class=\"filename\"><h2>");
                    output.write(image.getName());
                    output.write("</h2></td>" +
                            "</tr>" +
                            "<tr>" +
                            "<td rowspan=\"7\"><div><img src=\"");
                    output.write(image.getName());
                    output.write("\" alt=\"");
                    output.write(image.getName());
                    output.write("\" width=\"400\"></div></td>");

                    try {

                        BufferedImage bufferedImage = ImageIO.read(image);

                        EnumSet<Screen>[] matches = analyser.matchScreensForTesting(bufferedImage);
                        EnumSet<Screen> primaryMatches = matches[0];
                        EnumSet<Screen> secondaryMatches = matches[1];

                        log.debug("Image {} matches: {}", image.getName(), matches);

                        output.write("<td rowspan=\"3\" class=\"");
                        if (primaryMatches.size() == 0) {
                            output.write("matchzero");
                        } else if (primaryMatches.size() == 1) {
                            output.write("matchone");
                        } else {
                            output.write("matchmulti");
                        }
                        output.write("\">");

                        for (Screen match : primaryMatches) {
                            output.write(match.name());
                            output.write("<br>");
                        }

                        // Write screen-specific tests
                        output.write("</td><td rowspan=\"7\" class=\"extra\">");

                        // We don't need to keep the two match lists separate any more, so combine them for convenience
                        primaryMatches.addAll(secondaryMatches);

                        if (primaryMatches.contains(Screen.PLAY_LOBBY)) {
              writeScreenSpecificTest(output, "casual",
                  HearthstoneAnalyser.imageShowsCasualPlaySelected(bufferedImage));
              writeScreenSpecificTest(output, "ranked",
                  HearthstoneAnalyser.imageShowsRankedPlaySelected(bufferedImage));
              writeScreenSpecificTest(output, "deckSlot",
                  (Integer) HearthstoneAnalyser.imageIdentifyDeckSlot(bufferedImage).getOrElse(null));
                            writeScreenSpecificTest(output, "rankLevel", rankLevelOcr.processNumber(bufferedImage));
                        }
                        if (primaryMatches.contains(Screen.MATCH_VS) || primaryMatches.contains(Screen.MATCH_STARTINGHAND)) {
              writeScreenSpecificTest(output, "coin",
                  HearthstoneAnalyser.imageShowsCoin(bufferedImage));
              writeScreenSpecificTest(output, "opponentName",
                  HearthstoneAnalyser.imageShowsOpponentName(bufferedImage));
                        }
                        if (primaryMatches.contains(Screen.MATCH_VS)) {
              writeScreenSpecificTest(output, "yourClass",
                  HearthstoneAnalyser.imageIdentifyYourClass(bufferedImage));
              writeScreenSpecificTest(output, "opponentClass",
                  HearthstoneAnalyser.imageIdentifyOpponentClass(bufferedImage));
                        }
                        if (primaryMatches.contains(Screen.MATCH_ORGRIMMAR) || primaryMatches.contains(Screen.MATCH_PANDARIA)
                                || primaryMatches.contains(Screen.MATCH_STORMWIND) || primaryMatches.contains(Screen.MATCH_STRANGLETHORN)) {
              writeScreenSpecificTest(output, "yourTurn",
                  HearthstoneAnalyser.imageShowsYourTurn(bufferedImage));
              writeScreenSpecificTest(output, "opponentTurn",
                  HearthstoneAnalyser.imageShowsOpponentTurn(bufferedImage));
                        }
                        if (primaryMatches.contains(Screen.MATCH_ORGRIMMAR_END) || primaryMatches.contains(Screen.MATCH_PANDARIA_END)
                                || primaryMatches.contains(Screen.MATCH_STORMWIND_END) || primaryMatches.contains(Screen.MATCH_STRANGLETHORN_END)) {
              MatchOutcome matchOutcome = HearthstoneAnalyser
                  .imageShowsVictoryOrDefeat(bufferedImage);
                            writeScreenSpecificTest(output, "victory", matchOutcome == MatchOutcome.VICTORY);
                            writeScreenSpecificTest(output, "defeat", matchOutcome == MatchOutcome.DEFEAT);
                        }


                        // Write pixels
                        output.write("</td>");

                        if (expectedWidth != bufferedImage.getWidth() || expectedHeight != bufferedImage.getHeight()) {
                            pixelMap = analyser.calculatePixelPositions(bufferedImage.getWidth(), bufferedImage.getHeight());
                            expectedWidth = bufferedImage.getWidth();
                            expectedHeight = bufferedImage.getHeight();
                        }

                        writePixelValue(output, bufferedImage, PixelLocation.A);
                        writePixelValue(output, bufferedImage, PixelLocation.B);
                        writePixelValue(output, bufferedImage, PixelLocation.C);
                        output.write("</tr><tr>");
                        writePixelValue(output, bufferedImage, PixelLocation.D);
                        writePixelValue(output, bufferedImage, PixelLocation.E);
                        writePixelValue(output, bufferedImage, PixelLocation.F);
                        output.write("</tr><tr>");
                        writePixelValue(output, bufferedImage, PixelLocation.G);
                        writePixelValue(output, bufferedImage, PixelLocation.H);
                        writePixelValue(output, bufferedImage, PixelLocation.I);
                        output.write("</tr>");


                        // Write partial matches
                        output.write("<tr><td rowspan=\"4\" class=\"");
                        if (secondaryMatches.size() == 0) {
                            output.write("matchzero");
                        } else if (secondaryMatches.size() == 1) {
                            output.write("matchone");
                        } else {
                            output.write("matchmulti");
                        }
                        output.write("\">");

                        for (Screen match : secondaryMatches) {
                            output.write(match.name());
                            output.write("<br>");
                        }
                        output.write("</td>");


                        writePixelValue(output, bufferedImage, PixelLocation.J);
                        writePixelValue(output, bufferedImage, PixelLocation.K);
                        writePixelValue(output, bufferedImage, PixelLocation.L);
                        output.write("</tr><tr>");
                        writePixelValue(output, bufferedImage, PixelLocation.M);
                        writePixelValue(output, bufferedImage, PixelLocation.N);
                        writePixelValue(output, bufferedImage, PixelLocation.O);
                        output.write("</tr><tr>");
                        writePixelValue(output, bufferedImage, PixelLocation.P);
                        writePixelValue(output, bufferedImage, PixelLocation.Q);
                        writePixelValue(output, bufferedImage, PixelLocation.R);
                        output.write("</tr>");

                        // Blank row to make take up leftover space
                        output.write("<tr><td></td><td></td><td></td><td></td><td></td><td></td></tr>");

                        bufferedImage.flush();

                    } catch (IOException e) {
                        log.warn("Cannot handle image " + image.getName() + " due to exception", e);
                        output.write("<b>Exception</b></td></tr>");
                    }

                }

                writeHtmlFooter(output, page, pageCount);

            } catch (IOException e) {
                Assert.fail("IOException writing file " + filename);
                throw e;
            }

        }

    }


    private void writeScreenSpecificTest(BufferedWriter output, String label, boolean value) throws IOException {
        output.write("<li>");
        if (value) {
            output.write("<b>");
            output.write(label);
            output.write(" = true</b>");
        } else {
            output.write(label);
            output.write(" = false");
        }
        output.write("</li>");
    }


    private void writeScreenSpecificTest(BufferedWriter output, String label, Integer value) throws IOException {
        if (value == null) {
            writeScreenSpecificTest(output, label, (String) null);
        } else {
            writeScreenSpecificTest(output, label, String.valueOf(value));
        }
    }


    private void writeScreenSpecificTest(BufferedWriter output, String label, String value) throws IOException {
        output.write("<li>");
        if (value == null) {
            output.write(label);
            output.write(" = ");
        } else {
            output.write("<b>");
            output.write(label);
            output.write(" = ");
            output.write(value);
            output.write("</b>");
        }
        output.write("</li>");
    }



    private void writePixelValue(BufferedWriter output, BufferedImage image, PixelLocation pixelLocation) throws IOException {

        Coordinate coordinate = pixelMap.get(pixelLocation);
		int x = coordinate.x();
		int y = coordinate.y();

        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = (rgb & 0xFF);

        // Calculate brightness using YIQ formula
        int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

        output.write("<td class=\"");
        if (yiq < 128) {
            output.write("cd");
        } else {
            output.write("cl");
        }
        output.write("\" style=\"background-color:#");
        output.write(String.format("%08x", rgb).substring(2));
        output.write("\" title=\"x=");
        output.write(String.valueOf(x));
        output.write(", y=");
        output.write(String.valueOf(y));
        output.write("\">");
        output.write(pixelLocation.name());
        output.write("</td><td>");
        output.write(String.valueOf(red));
        output.write(", ");
        output.write(String.valueOf(green));
        output.write(", ");
        output.write(String.valueOf(blue));
        output.write("</td>");
    }


    private void writeHtmlHeader(BufferedWriter output, int page, int pageCount) throws IOException {
        output.write("<html>" +
                "<head>" +
                "<title>HeartStats.net Uploader - Match Test</title>" +
                "<style type=\"text/css\">" +
                    "html, body, p, div, th, td { font-family: Helvetica, Arial; } " +
                    ".nav { background-color: #f8f8f8; padding: 10px 20px; text-align: center; font-size: 123%; margin: 10px 0; } " +
                    ".matchzero { background-color: #f4f4f4; vertical-align: middle; } " +
                    ".matchone { background-color: #e8ffe8; vertical-align: middle; } " +
                    ".matchmulti { background-color: #fff8e8; vertical-align: middle; } " +
                    ".extra { vertical-align: top; } " +
                    "h2 { margin: 20px 0 0 0; font-size: 14pt; } " +
                    ".cd { color: #FFFFFF; text-align: center; font-weight: bold; } " +
                    ".cl { color: #000000; text-align: center; font-weight: bold; } " +
                    "tr, td, th { vertical-align: middle; padding: 5px 8px; } " +
                "</style></head>");
        output.write("<body><h1>HeartStats.net Uploader Match Test</h1><p>This test was executed at ");
        output.write(String.format("%1$tr, %1$te %1$tb %1$tY", new Date()));
        output.write(".</p>");
        writeHtmlPageNav(output, page, pageCount);
        output.write("<table>" +
                "<col width=\"400\">" +
                "<col width=\"250\">" +
                "<col width=\"250\">" +
                "<col width=\"35\">" +
                "<col width=\"120\">" +
                "<col width=\"35\">" +
                "<col width=\"120\">" +
                "<col width=\"35\">" +
                "<col width=\"120\">" +
                "<thead>" +
                "<tr>" +
                    "<th>Image</th>" +
                    "<th>Exact Match<br>Partial Match</th>" +
                    "<th>Extra Tests</th>" +
                    "<th colspan=\"6\">Pixels <i>(red, green, blue)</i></th>" +
                "</tr>" +
                "</thead><tbody>");
    }


    private void writeHtmlFooter(BufferedWriter output, int page, int pageCount) throws IOException {
        output.write("</tbody></table>");
        writeHtmlPageNav(output, page, pageCount);
        output.write("</body></html>");
    }


    private void writeHtmlPageNav(BufferedWriter output, int page, int pageCount) throws IOException {
        if (pageCount > 1) {
            output.write("<div class=\"nav\">");
            if (page > 1) {
                output.write("<a href=\"match-test-" + (page - 1) + ".html\">&lt; Prev Page</a> ");
            }
            for (int i = 1; i <= pageCount; i++) {
                output.write("<a href=\"match-test-" + i + ".html\">" + i + "</a> ");
            }
            if (page < pageCount) {
                output.write("<a href=\"match-test-" + (page + 1) + ".html\">Next Page &gt;</a> ");
            }
            output.write("</div>");
        }
    }




}
