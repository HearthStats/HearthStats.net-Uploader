package net.hearthstats.ui

import net.hearthstats.util.Translations.t
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.IOException
import java.net.URI
import java.util.Collections
import java.util.Comparator
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import net.hearthstats.API
import net.hearthstats.Constants
import net.hearthstats.DeckUtils
import net.hearthstats.Main
import net.miginfocom.swing.MigLayout
import org.json.simple.JSONObject
import scala.collection.JavaConversions._
import Constants._

class DecksTab extends JPanel {
  val deckSlotComboBoxes = List.fill(9)(new JComboBox[String])

  setLayout(new MigLayout)
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("set_your_deck_slots")), "skip,span,wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("deck_slot.label_1")), "skip")
  add(new JLabel(t("deck_slot.label_2")), "")
  add(new JLabel(t("deck_slot.label_3")), "wrap")
  add(deckSlotComboBoxes(0), "skip")
  add(deckSlotComboBoxes(1), "")
  add(deckSlotComboBoxes(2), "wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("deck_slot.label_4")), "skip")
  add(new JLabel(t("deck_slot.label_5")), "")
  add(new JLabel(t("deck_slot.label_6")), "wrap")
  add(deckSlotComboBoxes(3), "skip")
  add(deckSlotComboBoxes(4), "")
  add(deckSlotComboBoxes(5), "wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("deck_slot.label_7")), "skip")
  add(new JLabel(t("deck_slot.label_8")), "")
  add(new JLabel(t("deck_slot.label_9")), "wrap")
  add(deckSlotComboBoxes(6), "skip")
  add(deckSlotComboBoxes(7), "")
  add(deckSlotComboBoxes(8), "wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(" "), "wrap")

  val saveButton = new JButton(t("button.save_deck_slots"))
  saveButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent) {
      _saveDeckSlots()
    }
  })
  add(saveButton, "skip")

  val refreshButton = new JButton(t("button.refresh"))
  refreshButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent) {
      try {
        updateDecks()
      } catch {
        case e1: IOException => Main.showErrorDialog("Error updating decks", e1)
      }
    }
  })
  add(refreshButton, "wrap,span")

  add(new JLabel(" "), "wrap")
  add(new JLabel(" "), "wrap")
  val myDecksButton = new JButton(t("manage_decks_on_hsnet"))
  myDecksButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent) {
      try {
        Desktop.getDesktop.browse(new URI(DECKS_URL))
      } catch {
        case e1: Throwable => Main.showErrorDialog("Error launching browser with URL" + DECKS_URL, e1)
      }
    }
  })

  add(myDecksButton, "skip,span")

  def updateDecks() {
    DeckUtils.updateDecks()
    for (i <- 1 to 9)
      _applyDecksToSelector(deckSlotComboBoxes(i - 1), i)
  }

  private def _applyDecksToSelector(selector: JComboBox[String], slotNum: Int) {
    selector.setMaximumSize(new Dimension(200, selector.getSize().height))
    selector.removeAllItems()
    selector.addItem("- Select a deck -")
    val decks = DeckUtils.getDecks.sortBy(name)
    for (i <- 0 until decks.size) {
      selector.addItem(name(decks.get(i)) + "                                       #" +
        decks.get(i).get("id"))
      if (decks.get(i).get("slot") != null &&
        decks.get(i).get("slot").toString == slotNum.toString) selector.setSelectedIndex(i + 1)
    }
  }

  private def name(o: JSONObject): String = {
    Constants.hsClassOptions(Integer.parseInt(o.get("klass_id").toString)) +
      " - " +
      o.get("name").toString.toLowerCase()
  }

  private def _saveDeckSlots() {
    try {
      val slots = deckSlotComboBoxes map _getDeckSlotDeckId
      API.setDeckSlots(slots)
      Main.showMessageDialog(this, API.message)
      updateDecks()
    } catch {
      case e: Exception => Main.showErrorDialog("Error saving deck slots", e)
    }
  }

  val pattern = "[^0-9]+([0-9]+)$".r
  private def _getDeckSlotDeckId(selector: JComboBox[String]): Int =
    selector.getItemAt(selector.getSelectedIndex).asInstanceOf[String] match {
      case pattern(deckId) => deckId.toInt
      case _ => -1
    }
}
