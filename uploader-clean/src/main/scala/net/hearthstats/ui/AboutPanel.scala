package net.hearthstats.ui

import java.awt.Color
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.util.HashMap
import java.util.Map
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.text.html.HTMLDocument
import net.hearthstats.config.Application
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.log.LogPane
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.text.StrSubstitutor
import scala.collection.JavaConversions._
import net.hearthstats.util.Translation

class AboutPanel(translation: Translation, uiLog: Log) extends JScrollPane {
  import translation.t

  private def contributors(): JEditorPane = {
    val localeStrings = new HashMap[String, String]()
    localeStrings.put("Author", t("Author"))
    localeStrings.put("version", t("Companion") + " v" + Application.version)
    localeStrings.put("utility_l1", t("about.utility_l1"))
    localeStrings.put("utility_l2", t("about.utility_l2"))
    localeStrings.put("utility_l3", t("about.utility_l3"))
    localeStrings.put("open_source_l1", t("about.open_source_l1"))
    localeStrings.put("open_source_l2", t("about.open_source_l2"))
    localeStrings.put("project_source", t("about.project_source"))
    localeStrings.put("releases_and_changelog", t("about.releases_and_changelog"))
    localeStrings.put("feedback_and_suggestions", t("about.feedback_and_suggestions"))
    localeStrings.put("support_project", t("about.support_project"))
    localeStrings.put("donate_image", classOf[AboutPanel].getResource("/images/donate.gif").toString)
    val contributorsText = new JEditorPane()
    contributorsText.setContentType("text/html")
    contributorsText.setEditable(false)
    contributorsText.setBackground(Color.WHITE)
    try {
      val cssReader = new InputStreamReader(classOf[LogPane].getResourceAsStream("/net/hearthstats/about.css"))
      contributorsText.getDocument.asInstanceOf[HTMLDocument].getStyleSheet.loadRules(cssReader, null)
    } catch {
      case e: IOException => uiLog.warn("Unable to format About tab", e)
    }
    try {
      val aboutReader = new InputStreamReader(classOf[LogPane].getResourceAsStream("/net/hearthstats/about.html"))
      val aboutText = StrSubstitutor.replace(IOUtils toString aboutReader, localeStrings)
      contributorsText.setText(aboutText)
    } catch {
      case e: IOException => uiLog.warn("Unable to display About tab", e)
    }
    contributorsText.addHyperlinkListener(HyperLinkHandler.getInstance)
    contributorsText
  }
}

