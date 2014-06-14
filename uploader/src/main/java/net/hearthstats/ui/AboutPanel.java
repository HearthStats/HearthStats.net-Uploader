package net.hearthstats.ui;

import static net.hearthstats.util.Translations.t;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLDocument;

import net.hearthstats.Config;
import net.hearthstats.HyperLinkHandler;
import net.hearthstats.log.Log;
import net.hearthstats.log.LogPane;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class AboutPanel extends JScrollPane {

  public AboutPanel() {
    super(contributors(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  }

  private static JEditorPane contributors() {
    Map<String, String> localeStrings = new HashMap<String, String>();
    localeStrings.put("Author", t("Author"));
    localeStrings.put("version", t("Uploader") + " v" + Config.getVersion());
    localeStrings.put("utility_l1", t("about.utility_l1"));
    localeStrings.put("utility_l2", t("about.utility_l2"));
    localeStrings.put("utility_l3", t("about.utility_l3"));
    localeStrings.put("open_source_l1", t("about.open_source_l1"));
    localeStrings.put("open_source_l2", t("about.open_source_l2"));
    localeStrings.put("project_source", t("about.project_source"));
    localeStrings.put("releases_and_changelog", t("about.releases_and_changelog"));
    localeStrings.put("feedback_and_suggestions", t("about.feedback_and_suggestions"));
    localeStrings.put("support_project", t("about.support_project"));
    localeStrings
        .put("donate_image", AboutPanel.class.getResource("/images/donate.gif").toString());

    JEditorPane contributorsText = new JEditorPane();
    contributorsText.setContentType("text/html");
    contributorsText.setEditable(false);
    contributorsText.setBackground(Color.WHITE);

    try (Reader cssReader = new InputStreamReader(
        LogPane.class.getResourceAsStream("/net/hearthstats/about.css"))) {
      ((HTMLDocument) contributorsText.getDocument()).getStyleSheet().loadRules(cssReader, null);
    } catch (IOException e) {
      // If we can't load the About css, log a warning but continue
      Log.warn("Unable to format About tab", e);
    }

    try (Reader aboutReader = new InputStreamReader(
        LogPane.class.getResourceAsStream("/net/hearthstats/about.html"))) {
      String aboutText = StrSubstitutor.replace(IOUtils.toString(aboutReader), localeStrings);
      contributorsText.setText(aboutText);
    } catch (IOException e) {
      // If we can't load the About text, log a warning but continue
      Log.warn("Unable to display About tab", e);
    }
    contributorsText.addHyperlinkListener(HyperLinkHandler.getInstance());
    return contributorsText;

  }
}
