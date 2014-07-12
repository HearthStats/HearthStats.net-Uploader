package net.hearthstats.updater.exception;

public class UpdaterException extends RuntimeException {

  public UpdaterException(String message) {
    super(message);
  }

  public UpdaterException(String message, Throwable cause) {
    super(message, cause);
  }
}
