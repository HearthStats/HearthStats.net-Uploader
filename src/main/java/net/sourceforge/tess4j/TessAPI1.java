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

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;

/**
 * A Java wrapper for
 * <code>Tesseract OCR 3.02 API</code> using
 * <code>JNA Direct Mapping</code>.
 */
public class TessAPI1 implements Library {

    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    /**
     * Native library name.
     */
    public static final String LIB_NAME = "libtesseract302";
    public static final String LIB_NAME_NON_WIN = "tesseract";

    static {
        Native.register(WINDOWS ? LIB_NAME : LIB_NAME_NON_WIN);
    }

    /**
     * When Tesseract/Cube is initialized we can choose to instantiate/load/run
     * only the Tesseract part, only the Cube part or both along with the
     * combiner. The preference of which engine to use is stored in
     * <code>tessedit_ocr_engine_mode</code>.<br />
     * <br />
     * ATTENTION: When modifying this enum, please make sure to make the
     * appropriate changes to all the enums mirroring it (e.g. OCREngine in
     * cityblock/workflow/detection/detection_storage.proto). Such enums will
     * mention the connection to OcrEngineMode in the comments.
     */
    public static interface TessOcrEngineMode {

        /** Run Tesseract only - fastest */
        public static final int OEM_TESSERACT_ONLY = (int) 0;
        /** Run Cube only - better accuracy, but slower */
        public static final int OEM_CUBE_ONLY = (int) 1;
        /** Run both and combine results - best accuracy */
        public static final int OEM_TESSERACT_CUBE_COMBINED = (int) 2;
        /** Specify this mode when calling init_*(),
         to indicate that any of the above modes
         should be automatically inferred from the
         variables in the language-specific config,
         command-line configs, or if not specified
         in any of the above should be set to the
         default OEM_TESSERACT_ONLY. */
        public static final int OEM_DEFAULT = (int) 3;
    };

    /**
     * Possible modes for page layout analysis. These *must* be kept in order of
     * decreasing amount of layout analysis to be done, except for <code>OSD_ONLY</code>, so
     * that the inequality test macros below work.
     */
    public static interface TessPageSegMode {

        /** Orientation and script detection only. */
        public static final int PSM_OSD_ONLY = (int) 0;
        /** Automatic page segmentation with orientation and script detection. (OSD) */
        public static final int PSM_AUTO_OSD = (int) 1;
        /** Automatic page segmentation, but no OSD, or OCR. */
        public static final int PSM_AUTO_ONLY = (int) 2;
        /** Fully automatic page segmentation, but no OSD. */
        public static final int PSM_AUTO = (int) 3;
        /** Assume a single column of text of variable sizes. */
        public static final int PSM_SINGLE_COLUMN = (int) 4;
        /** Assume a single uniform block of vertically aligned text. */
        public static final int PSM_SINGLE_BLOCK_VERT_TEXT = (int) 5;
        /** Assume a single uniform block of text. */
        public static final int PSM_SINGLE_BLOCK = (int) 6;
        /** Treat the image as a single text line. */
        public static final int PSM_SINGLE_LINE = (int) 7;
        /** Treat the image as a single word. */
        public static final int PSM_SINGLE_WORD = (int) 8;
        /** Treat the image as a single word in a circle. */
        public static final int PSM_CIRCLE_WORD = (int) 9;
        /** Treat the image as a single character.  */
        public static final int PSM_SINGLE_CHAR = (int) 10;
        /** Find as much text as possible in no particular order.  */
        public static final int PSM_SPARSE_TEXT = (int) 11;
        /** Sparse text with orientation and script detection.  */
        public static final int PSM_SPARSE_TEXT_OSD = (int) 12;
        /** Number of enum entries. */
        public static final int PSM_COUNT = (int) 13;
    };

    /**
     * Enum of the elements of the page hierarchy, used in <code>ResultIterator</code> to
     * provide functions that operate on each level without having to have 5x as
     * many functions.
     */
    public static interface TessPageIteratorLevel {
    
        /** Block of text/image/separator line. */
        public static final int RIL_BLOCK = (int) 0;
        /** Paragraph within a block. */
        public static final int RIL_PARA = (int) 1;
        /** Line within a paragraph. */
        public static final int RIL_TEXTLINE = (int) 2;
        /** Word within a textline. */
        public static final int RIL_WORD = (int) 3;
        /** Symbol/character within a word. */
        public static final int RIL_SYMBOL = (int) 4;
    };

    public static interface TessPolyBlockType {
    
        /** Type is not yet known. Keep as the first element. */
        public static final int PT_UNKNOWN = (int) 0;
        /** Text that lives inside a column. */
        public static final int PT_FLOWING_TEXT = (int) 1;
        /** Text that spans more than one column. */
        public static final int PT_HEADING_TEXT = (int) 2;
        /** Text that is in a cross-column pull-out region. */
        public static final int PT_PULLOUT_TEXT = (int) 3;
        /** Partition belonging to an equation region. */
        public static final int PT_EQUATION = (int) 4;
        /** Partition has inline equation. */
        public static final int PT_INLINE_EQUATION = (int) 5;
        /** Partition belonging to a table region. */
        public static final int PT_TABLE = (int) 6;
        /** Text-line runs vertically. */
        public static final int PT_VERTICAL_TEXT = (int) 7;
        /** Text that belongs to an image. */
        public static final int PT_CAPTION_TEXT = (int) 8;
        /** Image that lives inside a column. */
        public static final int PT_FLOWING_IMAGE = (int) 9;
        /** Image that spans more than one column. */
        public static final int PT_HEADING_IMAGE = (int) 10;
        /** Image that is in a cross-column pull-out region. */
        public static final int PT_PULLOUT_IMAGE = (int) 11;
        /** Horizontal Line. */
        public static final int PT_HORZ_LINE = (int) 12;
        /** Vertical Line. */
        public static final int PT_VERT_LINE = (int) 13;
        /** Lies outside of any column. */
        public static final int PT_NOISE = (int) 14;
        /** Number of enum entries. */
        public static final int PT_COUNT = (int) 15;
    };

    /**
     * <pre>
     *  +------------------+
     *  | 1 Aaaa Aaaa Aaaa |
     *  | Aaa aa aaa aa    |
     *  | aaaaaa A aa aaa. |
     *  |                2 |
     *  |   #######  c c C |
     *  |   #######  c c c |
     *  | < #######  c c c |
     *  | < #######  c   c |
     *  | < #######  .   c |
     *  | 3 #######      c |
     *  +------------------+
     * </pre>
     * Orientation Example:<br />
     * ====================<br />
     * Above is a
     * diagram of some (1) English and (2) Chinese text and a (3) photo
     * credit.<br />
     * <br />
     * Upright Latin characters are represented as A and a. '<' represents
     * a latin character rotated anti-clockwise 90 degrees. Upright
     * Chinese characters are represented C and c.<br />
     * <br />
     * NOTA BENE: enum values here should match goodoc.proto<br />
     * <br />
     * If you orient your head so that "up" aligns with Orientation, then
     * the characters will appear "right side up" and readable.<br />
     * <br />
     * In the example above, both the
     * English and Chinese paragraphs are oriented so their "up" is the top of
     * the page (page up). The photo credit is read with one's head turned
     * leftward ("up" is to page left).<br />
     * <br /> The values of this enum
     * match the convention of Tesseract's osdetect.h
     */
    public static interface TessOrientation {

        public static final int ORIENTATION_PAGE_UP = (int) 0;
        public static final int ORIENTATION_PAGE_RIGHT = (int) 1;
        public static final int ORIENTATION_PAGE_DOWN = (int) 2;
        public static final int ORIENTATION_PAGE_LEFT = (int) 3;
    };

    /**
     * The grapheme clusters within a line of text are laid out logically in
     * this direction, judged when looking at the text line rotated so that its
     * Orientation is "page up".<br />
     * <br />
     * For English text, the writing direction is left-to-right. For the Chinese
     * text in the above example, the writing direction is top-to-bottom.
     */
    public static interface TessWritingDirection {

        public static final int WRITING_DIRECTION_LEFT_TO_RIGHT = (int) 0;
        public static final int WRITING_DIRECTION_RIGHT_TO_LEFT = (int) 1;
        public static final int WRITING_DIRECTION_TOP_TO_BOTTOM = (int) 2;
    };

    /**
     * The text lines are read in the given sequence.<br /> <br /> In English,
     * the order is top-to-bottom. In Chinese, vertical text lines are read
     * right-to-left. Mongolian is written in vertical columns top to bottom
     * like Chinese, but the lines order left-to right.<br /> <br /> Note that
     * only some combinations make sense. For example,
     * <code>WRITING_DIRECTION_LEFT_TO_RIGHT</code> implies
     * <code>TEXTLINE_ORDER_TOP_TO_BOTTOM</code>.
     */
    public static interface TessTextlineOrder {

        public static final int TEXTLINE_ORDER_LEFT_TO_RIGHT = (int) 0;
        public static final int TEXTLINE_ORDER_RIGHT_TO_LEFT = (int) 1;
        public static final int TEXTLINE_ORDER_TOP_TO_BOTTOM = (int) 2;
    };
    public static final int TRUE = (int) 1;
    public static final int FALSE = (int) 0;

    /**
     * Returns the version identifier.
     */
    public static native String TessVersion();

    public static native void TessDeleteText(Pointer text);

    public static native void TessDeleteTextArray(PointerByReference arr);

    public static native void TessDeleteIntArray(IntBuffer arr);

    /**
     * Creates an instance of the base class for all Tesseract APIs.
     */
    public static native TessAPI1.TessBaseAPI TessBaseAPICreate();

    /**
     * Disposes the TesseractAPI instance.
     */
    public static native void TessBaseAPIDelete(TessAPI1.TessBaseAPI handle);

    /**
     * Set the name of the input file. Needed only for training and reading a
     * UNLV zone file.
     */
    public static native void TessBaseAPISetInputName(TessAPI1.TessBaseAPI handle, String name);

    /**
     * Set the name of the bonus output files. Needed only for debugging.
     */
    public static native void TessBaseAPISetOutputName(TessAPI1.TessBaseAPI handle, String name);

    /**
     * Set the value of an internal "parameter." Supply the name of the
     * parameter and the value as a string, just as you would in a config file.
     * Returns false if the name lookup failed. E.g.,
     * <code>SetVariable("tessedit_char_blacklist", "xyz");</code> to ignore x, y and z. Or
     * <code>SetVariable("classify_bln_numeric_mode", "1");</code> to set numeric-only mode.
     * <code>SetVariable</code> may be used before <code>Init</code>, but settings will revert to defaults
     * on <code>End()</code>.<br />
     * <br />
     * Note: Must be called after <code>Init()</code>. Only works for non-init variables
     * (init variables should be passed to <code>Init()</code>).
     */
    public static native int TessBaseAPISetVariable(TessAPI1.TessBaseAPI handle, String name, String value);
    
    /**
     * Returns true (1) if the parameter was found among Tesseract parameters.
     * Fills in value with the value of the parameter.
     */
    public static native int TessBaseAPIGetIntVariable(TessAPI1.TessBaseAPI handle, String name, IntBuffer value);

    public static native int TessBaseAPIGetBoolVariable(TessAPI1.TessBaseAPI handle, String name, IntBuffer value);

    public static native int TessBaseAPIGetDoubleVariable(TessAPI1.TessBaseAPI handle, String name, DoubleBuffer value);

    public static native String TessBaseAPIGetStringVariable(TessAPI1.TessBaseAPI handle, String name);

    /**
     * Print Tesseract parameters to the given file.<br />
     * <br />
     * Note: Must not be the first method called after instance create.
     */
    public static native void TessBaseAPIPrintVariablesToFile(TessAPI1.TessBaseAPI handle, String filename);

    /**
     * Instances are now mostly thread-safe and totally independent, but some
     * global parameters remain. Basically it is safe to use multiple
     * TessBaseAPIs in different threads in parallel, UNLESS: you use
     * <code>SetVariable</code> on some of the Params in classify and textord. If you do,
     * then the effect will be to change it for all your instances.<br />
     * <br />
     * Start tesseract. Returns zero on success and -1 on failure. NOTE that the
     * only members that may be called before Init are those listed above here
     * in the class definition.<br />
     * <br />
     * The <code>datapath</code> must be the name of the parent directory of tessdata and
     * must end in / . Any name after the last / will be stripped. The language
     * is (usually) an <code>ISO 639-3</code> string or <code>NULL</code> will default to eng. It is
     * entirely safe (and eventually will be efficient too) to call Init
     * multiple times on the same instance to change language, or just to reset
     * the classifier. The language may be a string of the form
     * [~]<lang>[+[~]<lang>]* indicating that multiple languages are to be
     * loaded. E.g., hin+eng will load Hindi and English. Languages may specify
     * internally that they want to be loaded with one or more other languages,
     * so the ~ sign is available to override that. E.g., if hin were set to
     * load eng by default, then hin+~eng would force loading only hin. The
     * number of loaded languages is limited only by memory, with the caveat
     * that loading additional languages will impact both speed and accuracy, as
     * there is more work to do to decide on the applicable language, and there
     * is more chance of hallucinating incorrect words. WARNING: On changing
     * languages, all Tesseract parameters are reset back to their default
     * values. (Which may vary between languages.) If you have a rare need to
     * set a Variable that controls initialization for a second call to <code>Init</code> you
     * should explicitly call <code>End()</code> and then use <code>SetVariable</code> before <code>Init</code>. This
     * is only a very rare use case, since there are very few uses that require
     * any parameters to be set before <code>Init</code>.<br />
     * <br />
     * If <code>set_only_non_debug_params</code> is true, only params that do not contain
     * "debug" in the name will be set.
     */
    public static native int TessBaseAPIInit1(TessAPI1.TessBaseAPI handle, String datapath, String language, int oem, PointerByReference configs, int configs_size);

    public static native int TessBaseAPIInit2(TessAPI1.TessBaseAPI handle, String datapath, String language, int oem);

    public static native int TessBaseAPIInit3(TessAPI1.TessBaseAPI handle, String datapath, String language);

    /**
     * Returns the languages string used in the last valid initialization. If
     * the last initialization specified "deu+hin" then that will be returned.
     * If hin loaded eng automatically as well, then that will not be included
     * in this list. To find the languages actually loaded, use
     * <code>GetLoadedLanguagesAsVector</code>. The returned string should NOT be deleted.
     */
    public static native String TessBaseAPIGetInitLanguagesAsString(TessAPI1.TessBaseAPI handle);

    /**
     * Returns the loaded languages in the vector of STRINGs. Includes all
     * languages loaded by the last <code>Init</code>, including those loaded as dependencies
     * of other loaded languages.
     */
    public static native PointerByReference TessBaseAPIGetLoadedLanguagesAsVector(TessAPI1.TessBaseAPI handle);

    /**
     * Returns the available languages in the vector of STRINGs.
     */
    public static native PointerByReference TessBaseAPIGetAvailableLanguagesAsVector(TessAPI1.TessBaseAPI handle);

    /**
     * Init only the lang model component of Tesseract. The only functions that
     * work after this init are <code>SetVariable</code> and <code>IsValidWord</code>. WARNING: temporary!
     * This function will be removed from here and placed in a separate API at
     * some future time.
     */
    public static native int TessBaseAPIInitLangMod(TessAPI1.TessBaseAPI handle, String datapath, String language);

    /**
     * Init only for page layout analysis. Use only for calls to <code>SetImage</code> and
     * <code>AnalysePage</code>. Calls that attempt recognition will generate an error.
     */
    public static native void TessBaseAPIInitForAnalysePage(TessAPI1.TessBaseAPI handle);

    /**
     * Read a "config" file containing a set of param, value pairs. Searches the
     * standard places: <code>tessdata/configs</code>, <code>tessdata/tessconfigs</code> and also accepts
     * a relative or absolute path name. Note: only non-init params will be set
     * (init params are set by <code>Init()</code>).
     */
    public static native void TessBaseAPIReadConfigFile(TessAPI1.TessBaseAPI handle, String filename, int init_only);

    /**
     * Set the current page segmentation mode. Defaults to PSM_SINGLE_BLOCK. The
     * mode is stored as an IntParam so it can also be modified by
     * <code>ReadConfigFile</code> or <code>SetVariable("tessedit_pageseg_mode", mode as string)</code>.
     */
    public static native void TessBaseAPISetPageSegMode(TessAPI1.TessBaseAPI handle, int mode);

    /**
     * Return the current page segmentation mode.
     */
    public static native int TessBaseAPIGetPageSegMode(TessAPI1.TessBaseAPI handle);

    /**
     * Recognize a rectangle from an image and return the result as a string.
     * May be called many times for a single <code>Init</code>. Currently has no error
     * checking. Greyscale of 8 and color of 24 or 32 bits per pixel may be
     * given. Palette color images will not work properly and must be converted
     * to 24 bit. Binary images of 1 bit per pixel may also be given but they
     * must be byte packed with the MSB of the first byte being the first pixel,
     * and a 1 represents WHITE. For binary images set bytes_per_pixel=0. The
     * recognized text is returned as a char* which is coded as UTF8 and must be
     * freed with the delete [] operator.<br />
     * <br />
     * Note that <code>TesseractRect</code> is the simplified convenience interface. For
     * advanced uses, use <code>SetImage</code>, (optionally) <code>SetRectangle</code>, <code>Recognize</code>, and
     * one or more of the <code>Get*Text</code> functions below.
     */
    public static native Pointer TessBaseAPIRect(TessAPI1.TessBaseAPI handle, ByteBuffer imagedata, int bytes_per_pixel, int bytes_per_line, int left, int top, int width, int height);

    /**
     * Call between pages or documents etc to free up memory and forget adaptive
     * data.
     */
    public static native void TessBaseAPIClearAdaptiveClassifier(TessAPI1.TessBaseAPI handle);

    /**
     * Provide an image for Tesseract to recognize. Format is as TesseractRect
     * above. Does not copy the image buffer, or take ownership. The source
     * image may be destroyed after Recognize is called, either explicitly or
     * implicitly via one of the <code>Get*Text</code> functions. <code>SetImage</code> clears all
     * recognition results, and sets the rectangle to the full image, so it may
     * be followed immediately by a <code>GetUTF8Text</code>, and it will automatically
     * perform recognition.
     */
    public static native void TessBaseAPISetImage(TessAPI1.TessBaseAPI handle, ByteBuffer imagedata, int width, int height, int bytes_per_pixel, int bytes_per_line);

    /**
     * Set the resolution of the source image in pixels per inch so font size
     * information can be calculated in results. Call this after SetImage().
     */
    public static native void TessBaseAPISetSourceResolution(TessAPI1.TessBaseAPI handle, int ppi);

    /**
     * Restrict recognition to a sub-rectangle of the image. Call after
     * <code>SetImage</code>. Each <code>SetRectangle</code> clears the recognition results so multiple
     * rectangles can be recognized with the same image.
     */
    public static native void TessBaseAPISetRectangle(TessAPI1.TessBaseAPI handle, int left, int top, int width, int height);

    /** Scale factor from original image. */
    public static native int TessBaseAPIGetThresholdedImageScaleFactor(TessAPI1.TessBaseAPI handle);
	
    /** Dump the internal binary image to a PGM file. */
    public static native void TessBaseAPIDumpPGM(TessAPI1.TessBaseAPI handle, String filename);
                
    /**
     * Runs page layout analysis in the mode set by SetPageSegMode. May
     * optionally be called prior to Recognize to get access to just the page
     * layout results. Returns an iterator to the results. Returns NULL on
     * error. The returned iterator must be deleted after use. WARNING! This
     * class points to data held within the TessBaseAPI class, and therefore can
     * only be used while the TessBaseAPI class still exists and has not been
     * subjected to a call of <code>Init</code>, <code>SetImage</code>, <code>Recognize</code>, <code>Clear</code>, <code>End</code>, DetectOS, or
     * anything else that changes the internal PAGE_RES.
     */
    public static native TessAPI1.TessPageIterator TessBaseAPIAnalyseLayout(TessAPI1.TessBaseAPI handle);

    /**
     * Recognize the image from SetAndThresholdImage, generating Tesseract
     * internal structures. Returns 0 on success. Optional. The <code>Get*Text</code>
     * functions below will call <code>Recognize</code> if needed. After Recognize, the
     * output is kept internally until the next <code>SetImage</code>.
     */
    public static native int TessBaseAPIRecognize(TessAPI1.TessBaseAPI handle, TessAPI1.ETEXT_DESC monitor);

    /**
     * Variant on Recognize used for testing chopper.
     */
    public static native int TessBaseAPIRecognizeForChopTest(TessAPI1.TessBaseAPI handle, TessAPI1.ETEXT_DESC monitor);

    /**
     * Get a reading-order iterator to the results of LayoutAnalysis and/or
     * Recognize. The returned iterator must be deleted after use. WARNING! This
     * class points to data held within the TessBaseAPI class, and therefore can
     * only be used while the TessBaseAPI class still exists and has not been
     * subjected to a call of <code>Init</code>, <code>SetImage</code>, <code>Recognize</code>, <code>Clear</code>, <code>End</code>, DetectOS, or
     * anything else that changes the internal PAGE_RES.
     */
    public static native TessAPI1.TessResultIterator TessBaseAPIGetIterator(TessAPI1.TessBaseAPI handle);
    
   /**
    * Get a mutable iterator to the results of LayoutAnalysis and/or Recognize.
    * The returned iterator must be deleted after use.
    * WARNING! This class points to data held within the TessBaseAPI class, and
    * therefore can only be used while the TessBaseAPI class still exists and
    * has not been subjected to a call of Init, SetImage, Recognize, Clear, End
    * DetectOS, or anything else that changes the internal PAGE_RES.
    */
    public static native TessAPI1.TessMutableIterator TessBaseAPIGetMutableIterator(TessAPI1.TessBaseAPI handle);    

    /**
     * Recognizes all the pages in the named file, as a multi-page tiff or list
     * of filenames, or single image, and gets the appropriate kind of text
     * according to parameters: <code>tessedit_create_boxfile</code>,
     * <code>tessedit_make_boxes_from_boxes</code>, <code>tessedit_write_unlv</code>,
     * <code>tessedit_create_hocr</code>. Calls ProcessPage on each page in the input file,
     * which may be a multi-page tiff, single-page other file format, or a plain
     * text list of images to read. If tessedit_page_number is non-negative,
     * processing begins at that page of a multi-page tiff file, or filelist.
     * The text is returned in text_out. Returns false on error. If non-zero
     * timeout_millisec terminates processing after the timeout on a single
     * page. If non-NULL and non-empty, and some page fails for some reason, the
     * page is reprocessed with the retry_config config file. Useful for
     * interactively debugging a bad page.
     */
    public static native Pointer TessBaseAPIProcessPages(TessAPI1.TessBaseAPI handle, String filename, String retry_config, int timeout_millisec);

    /**
     * The recognized text is returned as a char* which is coded as UTF-8 and
     * must be freed with the delete [] operator.
     */
    public static native Pointer TessBaseAPIGetUTF8Text(TessAPI1.TessBaseAPI handle);

    /**
     * Make a HTML-formatted string with hOCR markup from the internal data
     * structures. page_number is 0-based but will appear in the output as
     * 1-based.
     */
    public static native Pointer TessBaseAPIGetHOCRText(TessAPI1.TessBaseAPI handle, int page_number);

    /**
     * The recognized text is returned as a char* which is coded in the same
     * format as a box file used in training. Returned string must be freed with
     * the delete [] operator. Constructs coordinates in the original image -
     * not just the rectangle. page_number is a 0-based page index that will
     * appear in the box file.
     */
    public static native Pointer TessBaseAPIGetBoxText(TessAPI1.TessBaseAPI handle, int page_number);

    /**
     * The recognized text is returned as a char* which is coded as UNLV format
     * Latin-1 with specific reject and suspect codes and must be freed with the
     * delete [] operator.
     */
    public static native Pointer TessBaseAPIGetUNLVText(TessAPI1.TessBaseAPI handle);

    /**
     * Returns the (average) confidence value between 0 and 100.
     */
    public static native int TessBaseAPIMeanTextConf(TessAPI1.TessBaseAPI handle);

    /**
     * Returns all word confidences (between 0 and 100) in an array, terminated
     * by -1. The calling function must delete [] after use. The number of
     * confidences should correspond to the number of space-delimited words in
     * GetUTF8Text.
     */
    public static native IntByReference TessBaseAPIAllWordConfidences(TessAPI1.TessBaseAPI handle);

    /**
     * Applies the given word to the adaptive classifier if possible. The word
     * must be SPACE-DELIMITED UTF-8 - l i k e t h i s , so it can tell the
     * boundaries of the graphemes. Assumes that SetImage/SetRectangle have been
     * used to set the image to the given word. The mode arg should be
     * PSM_SINGLE_WORD or PSM_CIRCLE_WORD, as that will be used to control
     * layout analysis. The currently set PageSegMode is preserved. Returns
     * false if adaption was not possible for some reason.
     */
    public static native int TessBaseAPIAdaptToWordStr(TessAPI1.TessBaseAPI handle, int mode, String wordstr);

    /**
     * Free up recognition results and any stored image data, without actually
     * freeing any recognition data that would be time-consuming to reload.
     * Afterwards, you must call <code>SetImage</code> or <code>TesseractRect</code> before doing any
     * <code>Recognize</code> or <code>Get*</code> operation.
     */
    public static native void TessBaseAPIClear(TessAPI1.TessBaseAPI handle);

    /**
     * Close down tesseract and free up all memory. <code>End()</code> is equivalent to
     * destructing and reconstructing your TessBaseAPI. Once <code>End()</code> has been
     * used, none of the other API functions may be used other than <code>Init</code> and
     * anything declared above it in the class definition.
     */
    public static native void TessBaseAPIEnd(TessAPI1.TessBaseAPI handle);

    /**
     * Check whether a word is valid according to Tesseract's language model.
     *
     * @return 0 if the word is invalid, non-zero if valid. @warning temporary!
     * This function will be removed from here and placed in a separate API at
     * some future time.
     */
    public static native int TessBaseAPIIsValidWord(TessAPI1.TessBaseAPI handle, String word);

    public static native int TessBaseAPIGetTextDirection(TessAPI1.TessBaseAPI handle, IntBuffer out_offset, FloatBuffer out_slope);

    /**
     * This method returns the string form of the specified unichar.
     */
    public static native String TessBaseAPIGetUnichar(TessAPI1.TessBaseAPI handle, int unichar_id);
    
    /* Page iterator */
    public static native void TessPageIteratorDelete(TessAPI1.TessPageIterator handle);

    public static native TessAPI1.TessPageIterator TessPageIteratorCopy(TessAPI1.TessPageIterator handle);

    public static native void TessPageIteratorBegin(TessAPI1.TessPageIterator handle);

    public static native int TessPageIteratorNext(TessAPI1.TessPageIterator handle, int level);

    public static native int TessPageIteratorIsAtBeginningOf(TessAPI1.TessPageIterator handle, int level);

    public static native int TessPageIteratorIsAtFinalElement(TessAPI1.TessPageIterator handle, int level, int element);

    public static native int TessPageIteratorBoundingBox(TessAPI1.TessPageIterator handle, int level, IntBuffer left, IntBuffer top, IntBuffer right, IntBuffer bottom);

    public static native int TessPageIteratorBlockType(TessAPI1.TessPageIterator handle);

    public static native int TessPageIteratorBaseline(TessAPI1.TessPageIterator handle, int level, IntBuffer x1, IntBuffer y1, IntBuffer x2, IntBuffer y2);

    public static native void TessPageIteratorOrientation(TessAPI1.TessPageIterator handle, IntBuffer orientation, IntBuffer writing_direction, IntBuffer textline_order, FloatBuffer deskew_angle);

    /* Result iterator */
    public static native void TessResultIteratorDelete(TessAPI1.TessResultIterator handle);

    public static native TessAPI1.TessResultIterator TessResultIteratorCopy(TessAPI1.TessResultIterator handle);

    public static native TessAPI1.TessPageIterator TessResultIteratorGetPageIterator(TessAPI1.TessResultIterator handle);

    public static native TessAPI1.TessPageIterator TessResultIteratorGetPageIteratorConst(TessAPI1.TessResultIterator handle);

    public static native Pointer TessResultIteratorGetUTF8Text(TessAPI1.TessResultIterator handle, int level);

    public static native float TessResultIteratorConfidence(TessAPI1.TessResultIterator handle, int level);

    public static native String TessResultIteratorWordFontAttributes(TessAPI1.TessResultIterator handle, IntBuffer is_bold, IntBuffer is_italic, IntBuffer is_underlined, IntBuffer is_monospace, IntBuffer is_serif, IntBuffer is_smallcaps, IntBuffer pointsize, IntBuffer font_id);

    public static native int TessResultIteratorWordIsFromDictionary(TessAPI1.TessResultIterator handle);

    public static native int TessResultIteratorWordIsNumeric(TessAPI1.TessResultIterator handle);

    public static native int TessResultIteratorSymbolIsSuperscript(TessAPI1.TessResultIterator handle);

    public static native int TessResultIteratorSymbolIsSubscript(TessAPI1.TessResultIterator handle);

    public static native int TessResultIteratorSymbolIsDropcap(TessAPI1.TessResultIterator handle);

    public static class TessBaseAPI extends PointerType {

        public TessBaseAPI(Pointer address) {
            super(address);
        }

        public TessBaseAPI() {
            super();
        }
    };

    public static class ETEXT_DESC extends PointerType {

        public ETEXT_DESC(Pointer address) {
            super(address);
        }

        public ETEXT_DESC() {
            super();
        }
    };

    public static class TessPageIterator extends PointerType {

        public TessPageIterator(Pointer address) {
            super(address);
        }

        public TessPageIterator() {
            super();
        }
    };
    
    public static class TessMutableIterator extends PointerType {

        public TessMutableIterator(Pointer address) {
            super(address);
        }

        public TessMutableIterator() {
            super();
        }
    };

    public static class TessResultIterator extends PointerType {

        public TessResultIterator(Pointer address) {
            super(address);
        }

        public TessResultIterator() {
            super();
        }
    };
}
