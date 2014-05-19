package net.hearthstats.ocr;

/**
 * Exception thrown when OCR can't be performed on an image.
 */
public class OcrException extends Exception {

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }

}
