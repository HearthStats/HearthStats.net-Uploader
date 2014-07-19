package net.hearthstats.analysis;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import net.hearthstats.state.Screen;
import net.hearthstats.state.UniquePixel;
import net.hearthstats.util.Coordinate;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tests the IndividualPixelAnalyser against a folder of screenshots of match 'VS' screens
 * (which you need to provide yourself) to ensure that the hero/class detection is accurate.
 * It generates a set of HTML files showing each screenshot with information about which class it matched.
 *
 * @author gtch
 */
public class IndividualPixelAnalyserTest {

    private final static Logger log = LoggerFactory.getLogger(IndividualPixelAnalyserTest.class);


    private final static String IMAGE_PATH = "/tmp/hearthstats";
    private final static int PAGE_SIZE = 25;

	// @Test
    public void testYourClassDetection() throws Exception {

        ScreenAnalyser analyser = new ScreenAnalyser();
        IndividualPixelAnalyser individualPixelAnalyser = new IndividualPixelAnalyser();

        File imageFolder = new File(IMAGE_PATH);
        File[] imageArray = imageFolder.listFiles();

        Assert.assertNotNull("No files found in " + IMAGE_PATH + ". Please make sure you've set the path to a folder that contains screenshots from Hearthstone", imageArray);

        List<File> images = new ArrayList<>(imageArray.length);
        for (File image : imageArray) {
            if (image.getName().endsWith(".png")) {
                // Determine if this is a match end image
                BufferedImage bufferedImage = ImageIO.read(image);

                Screen screen = analyser.identifyScreen(bufferedImage, null);

                if (screen == Screen.MATCH_VS) {
                    // This is a match start screen, so it is suitable for testing with the IndividualPixelAnalyser
                    images.add(image);
                }

                bufferedImage.flush();
            }
        }

        Assert.assertFalse("No match end images found in " + IMAGE_PATH + ". Please make sure you've set the path to a folder that contains screenshots from Hearthstone", images.size() == 0);

        int page = 0;
        int pageCount = (images.size() / PAGE_SIZE) + 1;

        while (page < pageCount) {
            page++;

            String filename = IMAGE_PATH + "/individual-test-" + page + ".html";
            try (BufferedWriter output = new BufferedWriter(new FileWriter(filename))) {

                writeHtmlHeader(output, page, pageCount);

                List<Coordinate> coordinatesA = new ArrayList<>();
                List<Coordinate> coordinatesB = new ArrayList<>();
                List<Coordinate> coordinatesC = new ArrayList<>();

                for (int i = (page - 1) * PAGE_SIZE; i < images.size() && i < page * PAGE_SIZE; i++) {
                    File image = images.get(i);

                    try {

                        log.debug("***** Testing Image {} *****", image.getName());

                        BufferedImage bufferedImage = ImageIO.read(image);

                        boolean druidFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_DRUID_1, UniquePixel.YOUR_DRUID_2, UniquePixel.YOUR_DRUID_3});
                        boolean druidPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_DRUID_1, UniquePixel.YOUR_DRUID_2, UniquePixel.YOUR_DRUID_3});

                        boolean hunterFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_HUNTER_1, UniquePixel.YOUR_HUNTER_2, UniquePixel.YOUR_HUNTER_3});
                        boolean hunterPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_HUNTER_1, UniquePixel.YOUR_HUNTER_2, UniquePixel.YOUR_HUNTER_3});

                        boolean hunterGoldenFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_HUNTER_GOLDEN_1, UniquePixel.YOUR_HUNTER_GOLDEN_2, UniquePixel.YOUR_HUNTER_GOLDEN_3});
                        boolean hunterGoldenPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_HUNTER_GOLDEN_1, UniquePixel.YOUR_HUNTER_GOLDEN_2, UniquePixel.YOUR_HUNTER_GOLDEN_3});

                        boolean mageFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_MAGE_1, UniquePixel.YOUR_MAGE_2, UniquePixel.YOUR_MAGE_3});
                        boolean magePartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_MAGE_1, UniquePixel.YOUR_MAGE_2, UniquePixel.YOUR_MAGE_3});

                        boolean paladinFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_PALADIN_1, UniquePixel.YOUR_PALADIN_2, UniquePixel.YOUR_PALADIN_3});
                        boolean paladinPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_PALADIN_1, UniquePixel.YOUR_PALADIN_2, UniquePixel.YOUR_PALADIN_3});

                        boolean priestFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_PRIEST_1, UniquePixel.YOUR_PRIEST_2, UniquePixel.YOUR_PRIEST_3});
                        boolean priestPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_PRIEST_1, UniquePixel.YOUR_PRIEST_2, UniquePixel.YOUR_PRIEST_3});

                        boolean rogueFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_ROGUE_1, UniquePixel.YOUR_ROGUE_2, UniquePixel.YOUR_ROGUE_3});
                        boolean roguePartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_ROGUE_1, UniquePixel.YOUR_ROGUE_2, UniquePixel.YOUR_ROGUE_3});

                        boolean shamanFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_SHAMAN_1, UniquePixel.YOUR_SHAMAN_2, UniquePixel.YOUR_SHAMAN_3});
                        boolean shamanPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_SHAMAN_1, UniquePixel.YOUR_SHAMAN_2, UniquePixel.YOUR_SHAMAN_3});

                        boolean warlockFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_WARLOCK_1, UniquePixel.YOUR_WARLOCK_2, UniquePixel.YOUR_WARLOCK_3});
                        boolean warlockPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_WARLOCK_1, UniquePixel.YOUR_WARLOCK_2, UniquePixel.YOUR_WARLOCK_3});

                        boolean warriorFull = individualPixelAnalyser.testAllPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_WARRIOR_1, UniquePixel.YOUR_WARRIOR_2, UniquePixel.YOUR_WARRIOR_3});
                        boolean warriorPartial = individualPixelAnalyser.testAnyPixelsMatch(bufferedImage, new UniquePixel[] {
                                UniquePixel.YOUR_WARRIOR_1, UniquePixel.YOUR_WARRIOR_2, UniquePixel.YOUR_WARRIOR_3});

                        List<String> fullMatches = new ArrayList<>();
                        List<String> partialMatches = new ArrayList<>();

                        if (druidFull) fullMatches.add("Druid");
                        if (hunterFull) fullMatches.add("Hunter");
                        if (hunterGoldenFull) fullMatches.add("Golden Hunter");
                        if (mageFull) fullMatches.add("Mage");
                        if (paladinFull) fullMatches.add("Paladin");
                        if (priestFull) fullMatches.add("Priest");
                        if (rogueFull) fullMatches.add("Rogue");
                        if (shamanFull) fullMatches.add("Shaman");
                        if (warlockFull) fullMatches.add("Warlock");
                        if (warriorFull) fullMatches.add("Warrior");

                        if (druidPartial) partialMatches.add("Druid");
                        if (hunterPartial) partialMatches.add("Hunter");
                        if (hunterGoldenPartial) partialMatches.add("Golden Hunter");
                        if (magePartial) partialMatches.add("Mage");
                        if (paladinPartial) partialMatches.add("Paladin");
                        if (priestPartial) partialMatches.add("Priest");
                        if (roguePartial) partialMatches.add("Rogue");
                        if (shamanPartial) partialMatches.add("Shaman");
                        if (warlockPartial) partialMatches.add("Warlock");
                        if (warriorPartial) partialMatches.add("Warrior");

                        String primaryMatch = "";

                        Coordinate coordinateA;
                        Coordinate coordinateB;
                        Coordinate coordinateC;

                        UniquePixel pixelA = null;
                        UniquePixel pixelB = null;
                        UniquePixel pixelC = null;

                        if (druidFull) {
                            primaryMatch = "Druid";
                            pixelA = UniquePixel.YOUR_DRUID_1;
                            pixelB = UniquePixel.YOUR_DRUID_2;
                            pixelC = UniquePixel.YOUR_DRUID_3;
                        } else if (hunterFull) {
                            primaryMatch = "Hunter";
                            pixelA = UniquePixel.YOUR_HUNTER_1;
                            pixelB = UniquePixel.YOUR_HUNTER_2;
                            pixelC = UniquePixel.YOUR_HUNTER_3;
                        } else if (hunterGoldenFull) {
                            primaryMatch = "Golden Hunter";
                            pixelA = UniquePixel.YOUR_HUNTER_GOLDEN_1;
                            pixelB = UniquePixel.YOUR_HUNTER_GOLDEN_2;
                            pixelC = UniquePixel.YOUR_HUNTER_GOLDEN_3;
                        } else if (mageFull) {
                            primaryMatch = "Mage";
                            pixelA = UniquePixel.YOUR_MAGE_1;
                            pixelB = UniquePixel.YOUR_MAGE_2;
                            pixelC = UniquePixel.YOUR_MAGE_3;
                        } else if (paladinFull) {
                            primaryMatch = "Paladin";
                            pixelA = UniquePixel.YOUR_PALADIN_1;
                            pixelB = UniquePixel.YOUR_PALADIN_2;
                            pixelC = UniquePixel.YOUR_PALADIN_3;
                        } else if (priestFull) {
                            primaryMatch = "Priest";
                            pixelA = UniquePixel.YOUR_PRIEST_1;
                            pixelB = UniquePixel.YOUR_PRIEST_2;
                            pixelC = UniquePixel.YOUR_PRIEST_3;
                        } else if (rogueFull) {
                            primaryMatch = "Rogue";
                            pixelA = UniquePixel.YOUR_ROGUE_1;
                            pixelB = UniquePixel.YOUR_ROGUE_2;
                            pixelC = UniquePixel.YOUR_ROGUE_3;
                        } else if (shamanFull) {
                            primaryMatch = "Shaman";
                            pixelA = UniquePixel.YOUR_SHAMAN_1;
                            pixelB = UniquePixel.YOUR_SHAMAN_2;
                            pixelC = UniquePixel.YOUR_SHAMAN_3;
                        } else if (warlockFull) {
                            primaryMatch = "Warlock";
                            pixelA = UniquePixel.YOUR_WARLOCK_1;
                            pixelB = UniquePixel.YOUR_WARLOCK_2;
                            pixelC = UniquePixel.YOUR_WARLOCK_3;
                        } else if (warriorFull) {
                            primaryMatch = "Warrior";
                            pixelA = UniquePixel.YOUR_WARRIOR_1;
                            pixelB = UniquePixel.YOUR_WARRIOR_2;
                            pixelC = UniquePixel.YOUR_WARRIOR_3;
                        } else if (druidPartial) {
                            primaryMatch = "Druid (Partial)";
                            pixelA = UniquePixel.YOUR_DRUID_1;
                            pixelB = UniquePixel.YOUR_DRUID_2;
                            pixelC = UniquePixel.YOUR_DRUID_3;
                        } else if (hunterPartial) {
                            primaryMatch = "Hunter (Partial)";
                            pixelA = UniquePixel.YOUR_HUNTER_1;
                            pixelB = UniquePixel.YOUR_HUNTER_2;
                            pixelC = UniquePixel.YOUR_HUNTER_3;
                        } else if (hunterGoldenPartial) {
                            primaryMatch = "Golden Hunter (Partial)";
                            pixelA = UniquePixel.YOUR_HUNTER_GOLDEN_1;
                            pixelB = UniquePixel.YOUR_HUNTER_GOLDEN_2;
                            pixelC = UniquePixel.YOUR_HUNTER_GOLDEN_3;
                        } else if (magePartial) {
                            primaryMatch = "Mage (Partial)";
                            pixelA = UniquePixel.YOUR_MAGE_1;
                            pixelB = UniquePixel.YOUR_MAGE_2;
                            pixelC = UniquePixel.YOUR_MAGE_3;
                        } else if (paladinPartial) {
                            primaryMatch = "Paladin (Partial)";
                            pixelA = UniquePixel.YOUR_PALADIN_1;
                            pixelB = UniquePixel.YOUR_PALADIN_2;
                            pixelC = UniquePixel.YOUR_PALADIN_3;
                        } else if (priestPartial) {
                            primaryMatch = "Priest (Partial)";
                            pixelA = UniquePixel.YOUR_PRIEST_1;
                            pixelB = UniquePixel.YOUR_PRIEST_2;
                            pixelC = UniquePixel.YOUR_PRIEST_3;
                        } else if (roguePartial) {
                            primaryMatch = "Rogue (Partial)";
                            pixelA = UniquePixel.YOUR_ROGUE_1;
                            pixelB = UniquePixel.YOUR_ROGUE_2;
                            pixelC = UniquePixel.YOUR_ROGUE_3;
                        } else if (shamanPartial) {
                            primaryMatch = "Shaman (Partial)";
                            pixelA = UniquePixel.YOUR_SHAMAN_1;
                            pixelB = UniquePixel.YOUR_SHAMAN_2;
                            pixelC = UniquePixel.YOUR_SHAMAN_3;
                        } else if (warlockPartial) {
                            primaryMatch = "Warlock (Partial)";
                            pixelA = UniquePixel.YOUR_WARLOCK_1;
                            pixelB = UniquePixel.YOUR_WARLOCK_2;
                            pixelC = UniquePixel.YOUR_WARLOCK_3;
                        } else if (warriorPartial) {
                            primaryMatch = "Warrior (Partial)";
                            pixelA = UniquePixel.YOUR_WARRIOR_1;
                            pixelB = UniquePixel.YOUR_WARRIOR_2;
                            pixelC = UniquePixel.YOUR_WARRIOR_3;
                        }

                        if (pixelA == null) {
                            coordinatesA.add(null);
                            coordinatesB.add(null);
                            coordinatesC.add(null);
                            coordinateA = null;
                            coordinateB = null;
                            coordinateC = null;
                        } else {
                            coordinateA = addCoordinate(pixelA, individualPixelAnalyser, coordinatesA, bufferedImage);
                            coordinateB = addCoordinate(pixelB, individualPixelAnalyser, coordinatesB, bufferedImage);
                            coordinateC = addCoordinate(pixelC, individualPixelAnalyser, coordinatesC, bufferedImage);
                        }

                        output.write("<tr>" +
                                "<td colspan=\"3\" class=\"filename\"><h2>");
                        output.write(image.getName());
                        output.write("</h2></td>" +
                                "</tr>" +
                                "<tr>" +
                                "<td><div><img src=\"");
                        output.write(image.getName());
                        output.write("\" id=\"img_");
                        output.write(String.valueOf(i - ((page - 1) * PAGE_SIZE)));
                        output.write("\" alt=\"");
                        output.write(image.getName());
                        output.write("\" width=\"400\"></div></td>");

                        output.write("<td colspan=\"3\"><div><b>");
                        output.write(primaryMatch);
                        output.write(" A</b></div><div><canvas id=\"canvas_");
                        output.write(String.valueOf(i - ((page - 1) * PAGE_SIZE)));
                        output.write("a\" width=\"300\" height=\"300\"></div></td>");

                        output.write("<td colspan=\"3\"><div><b>");
                        output.write(primaryMatch);
                        output.write(" B</b></div><div><canvas id=\"canvas_");
                        output.write(String.valueOf(i - ((page - 1) * PAGE_SIZE)));
                        output.write("b\" width=\"300\" height=\"300\"></div></td>");

                        output.write("<td colspan=\"3\"><div><b>");
                        output.write(primaryMatch);
                        output.write(" C</b></div><div><canvas id=\"canvas_");
                        output.write(String.valueOf(i - ((page - 1) * PAGE_SIZE)));
                        output.write("c\" width=\"300\" height=\"300\"></div></td>");


                        output.write("<td class=\"");
                        if (fullMatches.size() == 0) {
                            output.write("matchzero");
                        } else if (fullMatches.size() == 1) {
                            output.write("matchone");
                        } else {
                            output.write("matchmulti");
                        }
                        output.write("\">");

                        if (fullMatches.size() > 0) {
                            output.write("<div style=\"margin-top:1em\"><b>Full Matches:</b></div>");
                            for (String fullMatch : fullMatches) {
                                output.write("<div>");
                                output.write(fullMatch);
                                output.write("</div>");
                            }
                            output.write("</b></div>");
                        }

                        if (partialMatches.size() > 0) {
                            output.write("<div style=\"margin-top:1em\"><b>Partial Matches:</b></div>");
                            for (String partialMatch : partialMatches) {
                                output.write("<div>");
                                output.write(partialMatch);
                                output.write("</div>");
                            }
                            output.write("</b></div>");
                        }


                        output.write("</td>" +
                                "</tr><tr><td></td>");

                        writePixelValue(output, bufferedImage, coordinateA, "A");
                        writePixelRange(output, pixelA);
                        writePixelValue(output, bufferedImage, coordinateB, "B");
                        writePixelRange(output, pixelB);
                        writePixelValue(output, bufferedImage, coordinateC, "C");
                        writePixelRange(output, pixelC);

                        output.write("<td></td>");

                        output.write("</tr>");


                    } catch (IOException e) {
                        log.warn("Cannot handle image " + image.getName() + " due to exception", e);
                        output.write("<b>Exception</b></td></tr>");
                    }


                }

                writeCanvasJavascript(output, coordinatesA, coordinatesB, coordinatesC);

                writeHtmlFooter(output, page, pageCount);

            } catch (IOException e) {
                Assert.fail("IOException writing file " + filename);
                throw e;
            }

        }



    }

    private Coordinate addCoordinate(UniquePixel uniquePixel, IndividualPixelAnalyser individualPixelAnalyser, List<Coordinate> coordinates, BufferedImage bufferedImage) {
		CoordinateCacheBase.UniquePixelIdentifier upi = new CoordinateCacheBase.UniquePixelIdentifier(
				uniquePixel.x(), uniquePixel.y(), bufferedImage.getWidth(),
				bufferedImage.getHeight());
        Coordinate coordinate = individualPixelAnalyser.getCachedCoordinate(upi);
        coordinates.add(coordinate);
        return coordinate;
    }


    private void writeHtmlHeader(BufferedWriter output, int page, int pageCount) throws IOException {
        output.write("<html>" +
                "<head>" +
                "<title>HeartStats Companion - Your Class Detection Test</title>" +
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
        output.write("<body><h1>HeartStats Companion Your Class Detection Test</h1><p>This test was executed at ");
        output.write(String.format("%1$tr, %1$te %1$tb %1$tY", new Date()));
        output.write(".</p>");
        writeHtmlPageNav(output, page, pageCount);
        output.write("<table>" +
                "<col width=\"400\">" +
                "<col width=\"34\">" +
                "<col width=\"110\">" +
                "<col width=\"156\">" +
                "<col width=\"34\">" +
                "<col width=\"110\">" +
                "<col width=\"156\">" +
                "<col width=\"34\">" +
                "<col width=\"110\">" +
                "<col width=\"156\">" +
                "<col width=\"200\">" +
                "<thead>" +
                "<tr>" +
                "<th>Image</th>" +
                "<th colspan=\"3\">Your Class Pixel A</th>" +
                "<th colspan=\"3\">Your Class Pixel B</th>" +
                "<th colspan=\"3\">Your Class Pixel C</th>" +
                "<th>Test Results</th>" +
                "</tr>" +
                "</thead><tbody>");
    }


    private void writeHtmlFooter(BufferedWriter output, int page, int pageCount) throws IOException {
        output.write("</tbody></table>");
        writeHtmlPageNav(output, page, pageCount);
        output.write("</body></html>");
    }


    /**
     * Writes an inline Javascript which zooms in on the reference pixel area and highlights the reference pixel location.
     */
    private void writeCanvasJavascript(BufferedWriter output, List<Coordinate> coordinatesA, List<Coordinate> coordinatesB, List<Coordinate> coordinatesC) throws IOException {
        output.write("<script type=\"text/javascript\">\n" +
                "function drawPixel(id, suffix, x, y) {\n" +
                "\n" +
                "    var canvas = document.getElementById(\"canvas_\" + id + suffix);\n" +
                "    var ctx = canvas.getContext(\"2d\");\n" +
                "    \n" +
                "    var image = new Image();\n" +
                "    image.onload = function()\n" +
                "    {\n" +
                "\t\tvar xoff = Math.floor(x / 200) * 200 - 50;\n" +
                "\t\tvar yoff = Math.floor(y / 200) * 200 - 50;\n" +
                "\t\tctx.drawImage(image, xoff, yoff, 300, 300, 0, 0, 300, 300);\n" +
                "\t\tctx.beginPath();\n" +
                "\t\tx = x - xoff;\n" +
                "\t\ty = y - yoff;\n" +
                "\t\tctx.strokeStyle = \"rgba(255,255,255,0.8)\";\n" +
                "\t\tctx.lineWidth = 4\n" +
                "\t\tctx.strokeRect(x-6, y-6, 12, 12);\n" +
                "\t\tctx.strokeStyle = \"rgba(0,0,0,0.8)\";\n" +
                "\t\tctx.lineWidth = 1\n" +
                "\t\tctx.strokeRect(x-4, y-4, 8, 8);\n" +
                "\t\tctx.strokeRect(x-8, y-8, 16, 16);\n" +
                "    }\n" +
                "    image.src = document.getElementById(\"img_\" + id).src;\t\n" +
                "}\n" +
                "\n" +
                "window.addEventListener(\"DOMContentLoaded\", function()\n" +
                "{\n");
        for (int i = 0; i < coordinatesA.size(); i++) {
            Coordinate coordinate = coordinatesA.get(i);
            if (coordinate != null) {
				output.write("drawPixel(\"" + i + "\", \"a\", "
						+ coordinate.x() + ", " + coordinate.y() + ");\n");
            }
        }
        for (int i = 0; i < coordinatesB.size(); i++) {
            Coordinate coordinate = coordinatesB.get(i);
            if (coordinate != null) {
				output.write("drawPixel(\"" + i + "\", \"b\", "
						+ coordinate.x() + ", " + coordinate.y() + ");\n");
            }
        }
        for (int i = 0; i < coordinatesC.size(); i++) {
            Coordinate coordinate = coordinatesC.get(i);
            if (coordinate != null) {
				output.write("drawPixel(\"" + i + "\", \"c\", "
						+ coordinate.x() + ", " + coordinate.y() + ");\n");
            }
        }
        output.write("});\n" +
                "</script>");


    }


    private void writePixelValue(BufferedWriter output, BufferedImage image, Coordinate coordinate, String name) throws IOException {


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
        output.write(Integer.toHexString(rgb).substring(2));
        output.write("\" title=\"x=");
        output.write(String.valueOf(x));
        output.write(", y=");
        output.write(String.valueOf(y));
        output.write("\">");
        output.write(name);
        output.write("</td><td>");
        output.write(String.valueOf(red));
        output.write(", ");
        output.write(String.valueOf(green));
        output.write(", ");
        output.write(String.valueOf(blue));
        output.write("</td>");
    }


    private void writePixelRange(BufferedWriter output, UniquePixel uniquePixel) throws IOException {

        output.write("<td><div><b>Min:</b> ");
        output.write(String.valueOf(uniquePixel.minRed));
        output.write(", ");
        output.write(String.valueOf(uniquePixel.minGreen));
        output.write(", ");
        output.write(String.valueOf(uniquePixel.minBlue));
        output.write("</div><div><b>Max:</b> ");
        output.write(String.valueOf(uniquePixel.maxRed));
        output.write(", ");
        output.write(String.valueOf(uniquePixel.maxGreen));
        output.write(", ");
        output.write(String.valueOf(uniquePixel.maxBlue));
        output.write("</div></td>");
    }



    private void writeHtmlPageNav(BufferedWriter output, int page, int pageCount) throws IOException {
        if (pageCount > 1) {
            output.write("<div class=\"nav\">");
            if (page > 1) {
                output.write("<a href=\"individual-test-" + (page - 1) + ".html\">&lt; Prev Page</a> ");
            }
            for (int i = 1; i <= pageCount; i++) {
                output.write("<a href=\"individual-test-" + i + ".html\">" + i + "</a> ");
            }
            if (page < pageCount) {
                output.write("<a href=\"individual-test-" + (page + 1) + ".html\">Next Page &gt;</a> ");
            }
            output.write("</div>");
        }
    }



}
