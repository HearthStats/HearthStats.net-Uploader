package net.hearthstats.ui.log

import java.io.File
import java.util.Iterator
import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.recovery.ResilientFileOutputStream
import scala.collection.JavaConversions._
import org.apache.commons.lang3.time.FastDateFormat
import org.slf4j.Marker
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import scala.collection.JavaConversions._
import org.apache.commons.lang3.time.FastDateFormat
import java.util.Date
import grizzled.slf4j.Logging

/**
 * Helper class that allows log messages to be logged from anywhere in
 * the application. These messages will be displayed in the log pane as well as
 * in the debug log file.
 */
class Log extends Logging {
  var logPane: LogPane = _
  private var lastMarker: LogType = WELCOME

  /**
   * Logs a welcome message. Usually used first thing after startup.
   *
   * @param logMessage
   *          The message to log.
   */
  def welcome(logMessage: String) {
    doLog(logMessage, WELCOME, INFO)
  }

  /**
   * Logs help messages. Usually only used immediately after startup.
   *
   * @param logMessage
   *          The message to log.
   */
  def help(logMessage: String) {
    doLog(logMessage, HELP, INFO)
  }

  /**
   * Logs a divider, useful for separating changes to state such as the end of a
   * match.
   */
  def divider() {
    doLog("------------------------------------------", DIVIDER, INFO)
  }

  /**
   * Logs a match result. This message has more prominence than normal log
   * messages.
   *
   * @param logMessage
   *          The message to log.
   */
  def matchResult(logMessage: String) {
    doLog(logMessage, MATCH, INFO)
  }

  /**
   * Logs a general info message. Most log messages should use this.
   *
   * @param logMessage
   *          The message to log.
   */
  def info(logMessage: String) {
    doLog(logMessage, STD, INFO)
  }

  def debug(logMessage: String) {
    doLog(logMessage, STD, DEBUG)
  }

  /**
   * Logs a warning message. This should be used for problems that are temporary
   * or unlikely to break the application.
   *
   * @param logMessage
   *          The message to log.
   * @param ex
   *          The exception that triggered this warning message.
   */
  def warn(logMessage: String, ex: Throwable = null) {
    doLog(logMessage, STD, WARN, Option(ex))
  }

  /**
   * Logs an error message. This should be used for problems that are serious
   * and likely to break the application.
   *
   * @param logMessage
   *          The message to log.
   * @param ex
   *          The exception that triggered this error message.
   */
  def error(logMessage: String, ex: Throwable = null) {
    doLog(logMessage, STD, ERROR, Option(ex))
  }

  private def doLog(msg: String, marker: LogType, level: LogLevel, exc: Option[Throwable] = None) {
    val message = formatMessage(msg, marker, level)
    if (logPane != null) {
      logPane.addLog(message)
    }
    val e = exc.getOrElse(null)
    level match {
      case DEBUG => super.debug(message, e)
      case INFO => super.info(message, e)
      case WARN => super.warn(message, e)
      case ERROR => super.error(message, e)
    }
  }

  /**
   * Gets the location of the log file configured in the logback.xml
   * configuration file.
   *
   * @return The location of the log file, including full path if available.
   *         Returns None if the standard file logger could not be found.
   */
  def getLogFileLocation: Option[String] = {
    def appenderFile(a: Appender[_]) = a match {
      case f: FileAppender[_] if "FILE" == f.getName =>
        f.getOutputStream match {
          case out: ResilientFileOutputStream =>
            Some(out.getFile.getAbsolutePath)
          case _ =>
            Some(f.getFile)
        }
      case _ => None
    }

    LoggerFactory.getILoggerFactory match {
      case context: LoggerContext =>
        for {
          logger <- context.getLoggerList
          appender <- logger.iteratorForAppenders
          file = appenderFile(appender)
          if (file.nonEmpty)
        } return file
        return None
      case _ => None
    }
  }

  private val timestampFormat: FastDateFormat = FastDateFormat.getInstance("HH:mm:ss")

  private def formatMessage(msg: String, marker: LogType, level: LogLevel): String = {
    val sb = new StringBuilder
    if (marker == DIVIDER) {
      sb.append("<div class=\"break\">&nbsp;</div><hr noshade size=\"2\" color=\"#b9b9b9\">")
      lastMarker = marker
    } else {
      val cssClass =
        if (level == ERROR) "error"
        else if (level == WARN) "warn"
        else if (marker == null) "log"
        else marker.toString
      if (marker != lastMarker) {
        sb.append("<div class=\"break\">&nbsp;</div>")
        lastMarker = marker
      }
      sb.append("<div class=\"")
      sb.append(cssClass)
      sb.append("\">")
      if (marker != WELCOME && marker != HELP) {
        sb.append("<span class=\"ts\">[")
        sb.append(timestampFormat.format(new Date))
        sb.append("]</span>&nbsp; ")
      }
      val message = msg.replaceAll("\n", "<br>")
      sb.append(message)
      sb.append("</div>")
    }
    sb.toString
  }
}

sealed trait LogType

case object WELCOME extends LogType
case object HELP extends LogType
case object DIVIDER extends LogType
case object MATCH extends LogType
case object STD extends LogType

sealed trait LogLevel

case object ERROR extends LogLevel
case object WARN extends LogLevel
case object INFO extends LogLevel
case object DEBUG extends LogLevel
