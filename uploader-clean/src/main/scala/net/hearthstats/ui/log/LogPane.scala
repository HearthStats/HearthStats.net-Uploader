package net.hearthstats.ui.log

import java.io.{ IOException, InputStreamReader }

import javax.swing.{ JEditorPane, SwingUtilities }
import javax.swing.text.BadLocationException
import javax.swing.text.html.HTMLDocument
import net.hearthstats.ui.HyperLinkHandler

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
    SwingUtilities.invokeLater(new Runnable() {

      override def run() {
        try {
          _document.insertBeforeEnd(_bodyElement, html)
        } catch {
          case e: BadLocationException => e.printStackTrace()
          case e: IOException => e.printStackTrace()
        }
        setCaretPosition(getDocument.getLength)
      }
    })
  }

  private def loadStylesheet(doc: HTMLDocument) {
    val cssReader = new InputStreamReader(getClass.getResourceAsStream(STYLESHEET_LOCATION))
    try {
      doc.getStyleSheet.loadRules(cssReader, null)
    } catch {
      case e: NullPointerException =>
        throw new IllegalStateException("NullPointerException reading stylesheet " + STYLESHEET_LOCATION + " - does the stylesheet exist?")
      case e: IOException => e.printStackTrace()
    }
  }
}
