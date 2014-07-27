package net.hearthstats.ui.log;

import java.io.File;
import java.util.Iterator;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;

/**
 * Static helper class that allows log messages to be logged from anywhere in the application.
 * These messages will be displayed in the log pane as well as in the debug log file.
 */
public class Log {

    public static final Marker WELCOME = MarkerFactory.getMarker("welcome");
    public static final Marker HELP = MarkerFactory.getMarker("help");
    public static final Marker DIVIDER = MarkerFactory.getMarker("divider");
    public static final Marker MATCH = MarkerFactory.getMarker("match");

    private static Logger appLog = LoggerFactory.getLogger("net.hearthstats.app");


    /**
     * Logs a welcome message. Usually used first thing after startup.
     * @param logMessage The message to log.
     */
  public void welcome(String logMessage) {
      System.out.println("WELCOME: " + logMessage);
        appLog.info(WELCOME, logMessage);
    }

    /**
     * Logs help messages. Usually only used immediately after startup.
     * @param logMessage The message to log.
     */
  public void help(String logMessage) {
        appLog.info(HELP, logMessage);
    }

    /**
     * Logs a divider, useful for separating changes to state such as the end of a match.
     */
  public void divider() {
        appLog.info(DIVIDER, "------------------------------------------");
    }

    /**
     * Logs a match result. This message has more prominence than normal log messages.
     * @param logMessage The message to log.
     */
  public void matchResult(String logMessage) {
        appLog.info(MATCH, logMessage);
    }

    /**
     * Logs a general info message. Most log messages should use this.
     * @param logMessage The message to log.
     */
  public void info(String logMessage) {
        appLog.info(logMessage);
    }

  public void debug(String logMessage) {
		appLog.debug(logMessage);
	}

    /**
     * Logs a warning message. This should be used for problems that are temporary or unlikely to break the application.
     * @param logMessage The message to log.
     */
  public void warn(String logMessage) {
        appLog.warn(logMessage);
    }

    /**
     * Logs a warning message. This should be used for problems that are temporary or unlikely to break the application.
     * @param logMessage The message to log.
     * @param ex The exception that triggered this warning message.
     */
  public void warn(String logMessage, Throwable ex) {
        appLog.warn(logMessage, ex);
    }

    /**
     * Logs an error message. This should be used for problems that are serious and likely to break the application.
     * @param logMessage The message to log.
     */
  public void error(String logMessage) {
        appLog.error(logMessage);
    }

    /**
     * Logs an error message. This should be used for problems that are serious and likely to break the application.
     * @param logMessage The message to log.
     * @param ex The exception that triggered this error message.
     */
  public void error(String logMessage, Throwable ex) {
        appLog.error(logMessage, ex);
    }


    /**
     * Gets the location of the log file configured in the logback.xml configuration file.
     *
     * @return The location of the log file, including full path if available. Returns null if the standard file logger could not be found.
     */
  public String getLogFileLocation() {
        try {
            ILoggerFactory contextObject = LoggerFactory.getILoggerFactory();
            if (contextObject != null && contextObject instanceof LoggerContext) {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
                    for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();) {
                        Appender<ILoggingEvent> appender = index.next();
                        if (appender != null && "FILE".equals(appender.getName())) {
                            // This is our standard file logger
                            if (appender instanceof FileAppender) {
                                FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) appender;
                                if (fileAppender.getOutputStream() != null && fileAppender.getOutputStream() instanceof ResilientFileOutputStream) {
                                    // We have an output stream, so we can get the full filename including path
                                    File file = ((ResilientFileOutputStream) fileAppender.getOutputStream()).getFile();
                                    return file.getAbsolutePath();
                                } else {
                                    // We don't have an output stream, so just get the filename as configured in the logback configuration file
                                    return fileAppender.getFile();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exceptions when getting the log file location, they don't really matter
            // (and we may not have anywhere to log them!)
            System.out.println("Ignoring exception looking up log file: " + e.getMessage());
        }
        // The standard file logger was found
        return null;
    }

}
