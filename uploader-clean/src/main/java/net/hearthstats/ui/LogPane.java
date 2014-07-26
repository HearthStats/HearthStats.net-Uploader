package net.hearthstats.ui;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

/**
 * Extension of JEditorPane that includes a thread-safe way to add log messages.
 */
public class LogPane extends JEditorPane {

    private static final String STYLESHEET_LOCATION = "/net/hearthstats/log/applog.css";

    private HyperlinkListener _hyperLinkListener;
    private HTMLDocument _document;
    private Element _bodyElement;


    public LogPane() {
        setContentType("text/html");
        setEditable(false);
        setText("<html><body id=\"logBody\"></body></html>");

        _document = (HTMLDocument) getDocument();
        _bodyElement = _document.getElement("logBody");

        loadStylesheet(_document);

        _hyperLinkListener = HyperLinkHandler.getInstance();
        addHyperlinkListener(_hyperLinkListener);
    }


    private static void loadStylesheet(HTMLDocument doc) {
        // This try-with-resources block will automatically close the inputstream
        try (Reader cssReader = new InputStreamReader(LogPane.class.getResourceAsStream(STYLESHEET_LOCATION))) {
            doc.getStyleSheet().loadRules(cssReader, null);
        } catch (NullPointerException e) {
            throw new IllegalStateException("NullPointerException reading stylesheet " + STYLESHEET_LOCATION + " - does the stylesheet exist?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addLog(final String html) {
        // Use invokeLater to ensure that the updates occur on the Swing Event Dispatch Thread
        // and thus avoid thread interference and other multi-threaded problems.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    _document.insertBeforeEnd(_bodyElement, html);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                setCaretPosition(getDocument().getLength());
            }
        });
    }
}

