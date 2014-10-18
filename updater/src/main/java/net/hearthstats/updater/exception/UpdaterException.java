package net.hearthstats.updater.exception;

public class UpdaterException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1567145458528495693L;

  public UpdaterException(String message) {
    super(message);
  }

  public UpdaterException(String message, Throwable cause) {
    super(message, cause);
  }
}
