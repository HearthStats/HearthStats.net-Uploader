package net.hearthstats.ui.log

import java.io.{ IOException, InputStreamReader }
import javax.swing.{ JEditorPane, SwingUtilities }
import javax.swing.text.BadLocationException
import javax.swing.text.html.HTMLDocument
import net.hearthstats.ui.HyperLinkHandler
import scala.swing.Swing

/**
 * Extension of JEditorPane that includes a thread-safe way to add log messages.
 */
class LogPane extends JEditorPane {
  val STYLESHEET_LOCATION = "/net/hearthstats/log/applog.css"

  setContentType("text/html")
  setEditable(false)
  setText("<html><body id=\"logBody\"></body></html>")

  private val _document = getDocument.asInstanceOf[HTMLDocument]
  private val _hyperLinkListener = HyperLinkHandler.getInstance
  private val _bodyElement = _document.getElement("logBody")

  loadStylesheet(_document)
  addHyperlinkListener(_hyperLinkListener)

  def addLog(html: String) {
    Swing.onEDT {
      _document.insertBeforeEnd(_bodyElement, html)
      setCaretPosition(getDocument.getLength)
    }
  }

  private def loadStylesheet(doc: HTMLDocument) {
    val cssReader = new InputStreamReader(getClass.getResourceAsStream(STYLESHEET_LOCATION))
    doc.getStyleSheet.loadRules(cssReader, null)
  }
}
