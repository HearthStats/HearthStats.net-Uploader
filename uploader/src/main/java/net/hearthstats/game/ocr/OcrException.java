package net.hearthstats.game.ocr;

/**
 * Exception thrown when OCR can't be performed on an image.
 */
public class OcrException extends Exception {

    /**
   * 
   */
  private static final long serialVersionUID = 2712507173589879357L;

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }

}
