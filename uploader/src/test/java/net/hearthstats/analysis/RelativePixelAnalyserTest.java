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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tests the RelativePixelAnalyser against a folder of screenshots of victory and defeat screens
 * (which you need to provide yourself) to ensure that the victory and defeat detection is accurate.
 * It generates a set of HTML files showing each screenshot with information about whether it matched victory
 * or defeat for screenshot.
 *
 * @author gtch
 */
public class RelativePixelAnalyserTest {

    private final static Logger log = LoggerFactory.getLogger(RelativePixelAnalyserTest.class);

    private final static String IMAGE_PATH = "/tmp/hearthstats";
    private final static int PAGE_SIZE = 25;

    @Test
    public void testFindRelativePixel() throws Exception {

        ScreenAnalyser analyser = new ScreenAnalyser();
        RelativePixelAnalyser relativePixelAnalyser = new RelativePixelAnalyser();

        File imageFolder = new File(IMAGE_PATH);
        File[] imageArray = imageFolder.listFiles();

        Assert.assertNotNull("No files found in " + IMAGE_PATH + ". Please make sure you've set the path to a folder that contains screenshots from Hearthstone", imageArray);

        List<File> images = new ArrayList<>(imageArray.length);
        for (File image : imageArray) {
            if (image.getName().endsWith(".png")) {
                // Determine if this is a match end image
                BufferedImage bufferedImage = ImageIO.read(image);

                Screen screen = analyser.identifyScreen(bufferedImage, null);

                if (screen == Screen.MATCH_ORGRIMMAR_END || screen == Screen.MATCH_PANDARIA_END
                        || screen == Screen.MATCH_STORMWIND_END || screen == Screen.MATCH_STRANGLETHORN_END) {
                    // This is a match end screen, so it is suitable for testing with the RelativePixelAnalyser
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

            String filename = IMAGE_PATH + "/relative-test-" + page + ".html";
            try (BufferedWriter output = new BufferedWriter(new FileWriter(filename))) {

                writeHtmlHeader(output, page, pageCount);

                List<Coordinate> coordinates = new ArrayList<>();

                for (int i = (page - 1) * PAGE_SIZE; i < images.size() && i < page * PAGE_SIZE; i++) {
                    File image = images.get(i);

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

                    output.write("<td><canvas id=\"canvas_");
                    output.write(String.valueOf(i - ((page - 1) * PAGE_SIZE)));
                    output.write("\" width=\"300\" height=\"300\"></td>");

                    try {

                        log.debug("***** Testing Image {} *****", image.getName());

                        BufferedImage bufferedImage = ImageIO.read(image);

                        Coordinate coordinate = relativePixelAnalyser.findRelativePixel(bufferedImage, UniquePixel.VICTORY_DEFEAT_REFBOX_TL, UniquePixel.VICTORY_DEFEAT_REFBOX_BR, 8, 11);
                        coordinates.add(coordinate);

                        output.write("<td class=\"");
                        if (coordinate == null) {
                            output.write("matchzero");
                        } else {
                            output.write("matchone");
                        }
                        output.write("\">");
                        if (coordinate != null) {
                            output.write("<div>Reference Pixel = ");
							output.write(String.valueOf(coordinate.x()));
                            output.write(", ");
							output.write(String.valueOf(coordinate.y()));
                            output.write("</div>");

                            int victory1Matches = relativePixelAnalyser.countMatchingRelativePixels(bufferedImage, coordinate, new UniquePixel[] {
                                    UniquePixel.VICTORY_REL_1A, UniquePixel.VICTORY_REL_1B
                            });
                            int victory2Matches = relativePixelAnalyser.countMatchingRelativePixels(bufferedImage, coordinate, new UniquePixel[] {
                                    UniquePixel.VICTORY_REL_2A, UniquePixel.VICTORY_REL_2B, UniquePixel.VICTORY_REL_2C
                            });
                            int defeat1Matches = relativePixelAnalyser.countMatchingRelativePixels(bufferedImage, coordinate, new UniquePixel[] {
                                    UniquePixel.DEFEAT_REL_1A, UniquePixel.DEFEAT_REL_1B, UniquePixel.DEFEAT_REL_1C, UniquePixel.DEFEAT_REL_1D, UniquePixel.DEFEAT_REL_1E
                            });
                            int defeat2Matches = relativePixelAnalyser.countMatchingRelativePixels(bufferedImage, coordinate, new UniquePixel[] {
                                    UniquePixel.DEFEAT_REL_2A
                            });

                            output.write("<div>Count of V1 matches: ");
                            output.write(String.valueOf(victory1Matches));
                            output.write("</div>");
                            output.write("<div>Count of V2 matches: ");
                            output.write(String.valueOf(victory2Matches));
                            output.write("</div>");
                            output.write("<div>Count of D1 matches: ");
                            output.write(String.valueOf(defeat1Matches));
                            output.write("</div>");
                            output.write("<div>Count of D2 matches: ");
                            output.write(String.valueOf(defeat2Matches));
                            output.write("</div>");

                            if (victory1Matches > 0 && victory2Matches == 3 && defeat1Matches == 0  && defeat2Matches == 0) {
                                output.write("<div><b>MATCHED VICTORY</b></div>");
                            }
                            if (victory1Matches == 0 && victory2Matches == 0 && defeat1Matches > 0  && defeat2Matches == 1) {
                                output.write("<div><b>MATCHED DEFEAT</b></div>");
                            }

                        }

                        output.write("</td>");

                    } catch (IOException e) {
                        log.warn("Cannot handle image " + image.getName() + " due to exception", e);
                        output.write("<b>Exception</b></td></tr>");
                    }

                    output.write("</tr>");

                }

                writeCanvasJavascript(output, coordinates);

                writeHtmlFooter(output, page, pageCount);

            } catch (IOException e) {
                Assert.fail("IOException writing file " + filename);
                throw e;
            }

        }



    }



    private void writeHtmlHeader(BufferedWriter output, int page, int pageCount) throws IOException {
        output.write("<html>" +
                "<head>" +
                "<title>HeartStats.net Uploader - Relative Pixel Location Test</title>" +
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
        output.write("<body><h1>HeartStats.net Uploader Relative Pixel Location Test</h1><p>This test was executed at ");
        output.write(String.format("%1$tr, %1$te %1$tb %1$tY", new Date()));
        output.write(".</p>");
        writeHtmlPageNav(output, page, pageCount);
        output.write("<table>" +
                "<col width=\"400\">" +
                "<col width=\"300\">" +
                "<col width=\"250\">" +
                "<thead>" +
                "<tr>" +
                "<th>Image</th>" +
                "<th>Reference Pixel</th>" +
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
    private void writeCanvasJavascript(BufferedWriter output, List<Coordinate> coordinates) throws IOException {
        output.write("<script type=\"text/javascript\">\n" +
                "function drawPixel(id, x, y) {\n" +
                "\n" +
                "    var canvas = document.getElementById(\"canvas_\" + id);\n" +
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
        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate coordinate = coordinates.get(i);
            if (coordinate != null) {
				output.write("drawPixel(\"" + i + "\", " + coordinate.x()
						+ ", " + coordinate.y() + ");\n");
            }
        }
        output.write("});\n" +
                "</script>");


    }


    private void writeHtmlPageNav(BufferedWriter output, int page, int pageCount) throws IOException {
        if (pageCount > 1) {
            output.write("<div class=\"nav\">");
            if (page > 1) {
                output.write("<a href=\"relative-test-" + (page - 1) + ".html\">&lt; Prev Page</a> ");
            }
            for (int i = 1; i <= pageCount; i++) {
                output.write("<a href=\"relative-test-" + i + ".html\">" + i + "</a> ");
            }
            if (page < pageCount) {
                output.write("<a href=\"relative-test-" + (page + 1) + ".html\">Next Page &gt;</a> ");
            }
            output.write("</div>");
        }
    }


}
