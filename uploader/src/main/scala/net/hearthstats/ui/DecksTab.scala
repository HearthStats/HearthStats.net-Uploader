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
import java.util.List
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
import DecksTab._
//remove if not needed
import scala.collection.JavaConversions._

object DecksTab {

  private val DECKS_URL = "http://hearthstats.net/decks"
}

class DecksTab extends JPanel {
  private var _deckSlot1Field: JComboBox[String] = new JComboBox()
  private var _deckSlot2Field: JComboBox[String] = new JComboBox()
  private var _deckSlot3Field: JComboBox[String] = new JComboBox()
  private var _deckSlot4Field: JComboBox[String] = new JComboBox()
  private var _deckSlot5Field: JComboBox[String] = new JComboBox()
  private var _deckSlot6Field: JComboBox[String] = new JComboBox()
  private var _deckSlot7Field: JComboBox[String] = new JComboBox()
  private var _deckSlot8Field: JComboBox[String] = new JComboBox()
  private var _deckSlot9Field: JComboBox[String] = new JComboBox()

  protected var _api: API = new API()

  setLayout(new MigLayout)
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("set_your_deck_slots")), "skip,span,wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("deck_slot.label_1")), "skip")
  add(new JLabel(t("deck_slot.label_2")), "")
  add(new JLabel(t("deck_slot.label_3")), "wrap")
  add(_deckSlot1Field, "skip")
  add(_deckSlot2Field, "")
  add(_deckSlot3Field, "wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("deck_slot.label_4")), "skip")
  add(new JLabel(t("deck_slot.label_5")), "")
  add(new JLabel(t("deck_slot.label_6")), "wrap")
  add(_deckSlot4Field, "skip")
  add(_deckSlot5Field, "")
  add(_deckSlot6Field, "wrap")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("deck_slot.label_7")), "skip")
  add(new JLabel(t("deck_slot.label_8")), "")
  add(new JLabel(t("deck_slot.label_9")), "wrap")
  add(_deckSlot7Field, "skip")
  add(_deckSlot8Field, "")
  add(_deckSlot9Field, "wrap")
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
    _applyDecksToSelector(_deckSlot1Field, 1)
    _applyDecksToSelector(_deckSlot2Field, 2)
    _applyDecksToSelector(_deckSlot3Field, 3)
    _applyDecksToSelector(_deckSlot4Field, 4)
    _applyDecksToSelector(_deckSlot5Field, 5)
    _applyDecksToSelector(_deckSlot6Field, 6)
    _applyDecksToSelector(_deckSlot7Field, 7)
    _applyDecksToSelector(_deckSlot8Field, 8)
    _applyDecksToSelector(_deckSlot9Field, 9)
  }

  private def _applyDecksToSelector(selector: JComboBox[String], slotNum: java.lang.Integer) {
    selector.setMaximumSize(new Dimension(200, selector.getSize().height))
    selector.removeAllItems()
    selector.addItem("- Select a deck -")
    val decks = DeckUtils.getDecks
    Collections.sort(decks, new Comparator[JSONObject]() {

      override def compare(o1: JSONObject, o2: JSONObject): Int = return name(o1).compareTo(name(o2))
    })
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
      _api.setDeckSlots(_getDeckSlotDeckId(_deckSlot1Field), _getDeckSlotDeckId(_deckSlot2Field), _getDeckSlotDeckId(_deckSlot3Field),
        _getDeckSlotDeckId(_deckSlot4Field), _getDeckSlotDeckId(_deckSlot5Field), _getDeckSlotDeckId(_deckSlot6Field),
        _getDeckSlotDeckId(_deckSlot7Field), _getDeckSlotDeckId(_deckSlot8Field), _getDeckSlotDeckId(_deckSlot9Field))
      Main.showMessageDialog(this, _api.getMessage)
      updateDecks()
    } catch {
      case e: Throwable => Main.showErrorDialog("Error saving deck slots", e)
    }
  }

  private def _getDeckSlotDeckId(selector: JComboBox[String]): java.lang.Integer = {
    var deckId: java.lang.Integer = null
    val deckStr = selector.getItemAt(selector.getSelectedIndex).asInstanceOf[String]
    val pattern = Pattern.compile("[^0-9]+([0-9]+)$")
    val matcher = pattern.matcher(deckStr)
    if (matcher.find()) {
      deckId = Integer.parseInt(matcher.group(1))
    }
    deckId
  }
}
