/**
 * Copyright @ 2012 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sourceforge.tess4j;

import net.sourceforge.vietocr.ImageIOHelper;
import com.sun.jna.Pointer;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.*;
import javax.imageio.IIOImage;

/**
 * An object layer on top of
 * <code>TessAPI1</code>, provides character recognition support for common
 * image formats, and multi-page TIFF images beyond the uncompressed, binary
 * TIFF format supported by Tesseract OCR engine. The extended capabilities are
 * provided by the
 * <code>Java Advanced Imaging Image I/O Tools</code>. <br /><br /> Support for
 * PDF documents is available through
 * <code>Ghost4J</code>, a
 * <code>JNA</code> wrapper for
 * <code>GPL Ghostscript</code>, which should be installed and included in
 * system path. <br /><br /> Any program that uses the library will need to
 * ensure that the required libraries (the
 * <code>.jar</code> files for
 * <code>jna</code>,
 * <code>jai-imageio</code>, and
 * <code>ghost4j</code>) are in its compile and run-time
 * <code>classpath</code>.
 */
public class Tesseract1 extends TessAPI1 {

    private String language = "eng";
    private String datapath = "tessdata";
    private int psm = TessAPI1.TessPageSegMode.PSM_AUTO;
    private boolean hocr;
    private int pageNum;
    private int ocrEngineMode = TessAPI1.TessOcrEngineMode.OEM_DEFAULT;
    private Properties prop = new Properties();
    public final static String htmlBeginTag =
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\""
            + " \"http://www.w3.org/TR/html4/loose.dtd\">\n"
            + "<html>\n<head>\n<title></title>\n"
            + "<meta http-equiv=\"Content-Type\" content=\"text/html;"
            + "charset=utf-8\" />\n<meta name='ocr-system' content='tesseract'/>\n"
            + "</head>\n<body>\n";
    public final static String htmlEndTag = "</body>\n</html>\n";
    
    private TessBaseAPI handle;
    
    private final static Logger logger = Logger.getLogger(Tesseract1.class.getName());
    
    /**
     * Public constructor.
     */
    public Tesseract1() {
        System.setProperty("jna.encoding", "UTF8");
    }

    /**
     * @param datapath the tessdata path to set
     */
    public void setDatapath(String datapath) {
        this.datapath = datapath;
    }
    
    /**
     * Sets language for OCR.
     *
     * @param language the language code, which follows ISO 639-3 standard.
     */
    public void setLanguage(String language) {
        this.language = language;
    }
    
    /**
     * Sets OCR engine mode.
     *
     * @param ocrEngineMode the OcrEngineMode to set
     */
    public void setOcrEngineMode(int ocrEngineMode) {
        this.ocrEngineMode = ocrEngineMode;
    }

    /**
     * @param mode the page segmentation mode to set
     */
    public void setPageSegMode(int mode) {
        this.psm = mode;
    }

    /**
     * Enables hocr output.
     *
     * @param hocr to enable or disable hocr output
     */
    public void setHocr(boolean hocr) {
        this.hocr = hocr;
        prop.setProperty("tessedit_create_hocr", hocr ? "1" : "0");
    }

    /**
     * Set the value of Tesseract's internal parameter.
     *
     * @param key variable name, e.g.,
     * <code>tessedit_create_hocr</code>,
     * <code>tessedit_char_whitelist</code>, etc.
     * @param value value for corresponding variable, e.g., "1", "0",
     * "0123456789", etc.
     */
    public void setTessVariable(String key, String value) {
        prop.setProperty(key, value);
    }

    /**
     * Returns API handle.
     */
    protected TessBaseAPI getHandle() {
        return handle;
    }

    /**
     * Performs OCR operation.
     *
     * @param imageFile an image file
     * @return the recognized text
     * @throws TesseractException
     */
    public String doOCR(File imageFile) throws TesseractException {
        return doOCR(imageFile, null);
    }

    /**
     * Performs OCR operation.
     *
     * @param imageFile an image file
     * @param rect the bounding rectangle defines the region of the image to be
     * recognized. A rectangle of zero dimension or
     * <code>null</code> indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    public String doOCR(File imageFile, Rectangle rect) throws TesseractException {
        try {
            return doOCR(ImageIOHelper.getIIOImageList(imageFile), rect);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    /**
     * Performs OCR operation.
     *
     * @param bi a buffered image
     * @return the recognized text
     * @throws TesseractException
     */
    public String doOCR(BufferedImage bi) throws TesseractException {
        return doOCR(bi, null);
    }

    /**
     * Performs OCR operation.
     *
     * @param bi a buffered image
     * @param rect the bounding rectangle defines the region of the image to be
     * recognized. A rectangle of zero dimension or
     * <code>null</code> indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    public String doOCR(BufferedImage bi, Rectangle rect) throws TesseractException {
        try {
            return doOCR(ImageIOHelper.getIIOImageList(bi), rect);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    /**
     * Performs OCR operation.
     *
     * @param imageList a list of
     * <code>IIOImage</code> objects
     * @param rect the bounding rectangle defines the region of the image to be
     * recognized. A rectangle of zero dimension or
     * <code>null</code> indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    public String doOCR(List<IIOImage> imageList, Rectangle rect) throws TesseractException {
        init();
        setTessVariables();

        try {
            StringBuilder sb = new StringBuilder();

            for (IIOImage oimage : imageList) {
                pageNum++;
                try {
                    setImage(oimage.getRenderedImage(), rect);
                    sb.append(getOCRText());
                } catch (IOException ioe) {
                    // skip the problematic image
                    logger.log(Level.SEVERE, ioe.getMessage(), ioe);
                }
            }

            if (hocr) {
                sb.insert(0, htmlBeginTag).append(htmlEndTag);
            }

            return sb.toString();
        } finally {
            dispose();
        }
    }

    /**
     * Performs OCR operation. Use
     * <code>SetImage</code>, (optionally)
     * <code>SetRectangle</code>, and one or more of the
     * <code>Get*Text</code> functions.
     *
     * @param xsize width of image
     * @param ysize height of image
     * @param buf pixel data
     * @param rect the bounding rectangle defines the region of the image to be
     * recognized. A rectangle of zero dimension or
     * <code>null</code> indicates the whole image.
     * @param bpp bits per pixel, represents the bit depth of the image, with 1
     * for binary bitmap, 8 for gray, and 24 for color RGB.
     * @return the recognized text
     * @throws TesseractException
     */
    public String doOCR(int xsize, int ysize, ByteBuffer buf, Rectangle rect, int bpp) throws TesseractException {
        init();
        setTessVariables();

        try {
            setImage(xsize, ysize, buf, rect, bpp);
            return getOCRText();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new TesseractException(e);
        } finally {
            dispose();
        }
    }
    
    /**
     * Initializes Tesseract engine.
     */
    protected void init() {
        pageNum = 0;
        handle = TessBaseAPICreate();
        TessBaseAPIInit2(handle, datapath, language, ocrEngineMode);
        TessBaseAPISetPageSegMode(handle, psm);
    }
     
    /**
     * Sets Tesseract's internal parameters.
     */
    protected void setTessVariables() {
        Enumeration<?> em = prop.propertyNames();
        while (em.hasMoreElements()) {
            String key = (String) em.nextElement();
            TessBaseAPISetVariable(handle, key, prop.getProperty(key));
        }
    }
    
    /**
     * A wrapper for {@link #setImage(int, int, ByteBuffer, Rectangle, int)}.
     */
    protected void setImage(RenderedImage image, Rectangle rect) throws IOException {
        setImage(image.getWidth(), image.getHeight(), ImageIOHelper.getImageByteBuffer(image), rect, image.getColorModel().getPixelSize());
    }

    /**
     * Sets image to be processed.
     * 
     * @param xsize width of image
     * @param ysize height of image
     * @param buf pixel data
     * @param rect the bounding rectangle defines the region of the image to be
     * recognized. A rectangle of zero dimension or
     * <code>null</code> indicates the whole image.
     * @param bpp bits per pixel, represents the bit depth of the image, with 1
     * for binary bitmap, 8 for gray, and 24 for color RGB.
     */
    protected void setImage(int xsize, int ysize, ByteBuffer buf, Rectangle rect, int bpp) {
        int bytespp = bpp / 8;
        int bytespl = (int) Math.ceil(xsize * bpp / 8.0);
        TessBaseAPISetImage(handle, buf, xsize, ysize, bytespp, bytespl);

        if (rect != null && !rect.isEmpty()) {
            TessBaseAPISetRectangle(handle, rect.x, rect.y, rect.width, rect.height);
        }
    }

    /**
     * Gets recognized text.
     */
    protected String getOCRText() {
        Pointer utf8Text = hocr ? TessBaseAPIGetHOCRText(handle, pageNum - 1) : TessBaseAPIGetUTF8Text(handle);
        String str = utf8Text.getString(0);
        TessDeleteText(utf8Text);
        return str;
    }

    /**
     * Releases all of the native resources used by this instance.
     */
    protected void dispose() {
        TessBaseAPIDelete(handle);
    }
}
