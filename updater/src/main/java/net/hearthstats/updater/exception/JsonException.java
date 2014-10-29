package net.hearthstats.updater.exception;

public class JsonException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 64833336660948683L;

  public JsonException(String message) {
    super(message);
  }

  public JsonException(String message, Throwable cause) {
    super(message, cause);
  }
}
