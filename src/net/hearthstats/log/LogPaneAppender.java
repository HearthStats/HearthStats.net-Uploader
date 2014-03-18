package net.hearthstats.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.hearthstats.Main;
import org.slf4j.Marker;

/**
 * Appender implementation that takes log messages from SLF4J/Logback, formats them and sends them to the LogPane for display.
 */
public class LogPaneAppender extends AppenderBase<ILoggingEvent> {

    private Marker lastMarker = Log.WELCOME;


    @Override
    protected void append(ILoggingEvent eventObject) {
        LogPane logPane = Main.getLogPane();
        if (logPane != null) {
            logPane.addLog(formatMessage(eventObject));
        }

    }


    private String formatMessage(ILoggingEvent eventObject) {
        StringBuilder sb = new StringBuilder();
        Marker marker = eventObject.getMarker();

        if (marker == Log.DIVIDER) {
            // Divider is a special case, there's no message to display: it's just a divider
            sb.append("<div class=\"break\">&nbsp;</div><hr noshade size=\"2\" color=\"#b9b9b9\">");
            lastMarker = marker;
        } else {
            // This is a normal log message
            String cssClass;
            if (eventObject.getLevel() == Level.ERROR) {
                cssClass = "error";
            } else if (eventObject.getLevel() == Level.WARN) {
                cssClass = "warn";
            } else if (marker == null) {
                cssClass = "log";
            } else {
                cssClass = marker.getName();
            }

            if (marker != lastMarker) {
                // The marker has changed, so add a line break to make the log clearer
                sb.append("<div class=\"break\">&nbsp;</div>");
                lastMarker = marker;
            }

            sb.append("<div class=\"");
            sb.append(cssClass);
            sb.append("\">");

            String message = eventObject.getMessage().replaceAll("\n", "<br>");
            sb.append(message);

            sb.append("</div>");
        }

        return sb.toString();
    }

}
