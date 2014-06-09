package net.hearthstats.ui

import net.hearthstats.util.Translations.t
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.ActionEvent
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
import scala.concurrent._
import scala.swing.Swing._
import java.awt.BorderLayout
import net.hearthstats.Deck
import javax.swing.JDialog
import javax.swing.JOptionPane
import net.hearthstats.Config
import net.hearthstats.util.HsRobot
import net.hearthstats.util.Browse

class DecksTab extends JPanel {
  val deckSlotComboBoxes = 1 to 9 map { new DeckSlotPanel(_) }

  setLayout(new MigLayout)
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("set_your_deck_slots")), "skip,span,wrap")

  add(new JLabel(" "), "wrap")
  add(deckSlotComboBoxes(0), "skip")
  add(deckSlotComboBoxes(1), "")
  add(deckSlotComboBoxes(2), "wrap")

  add(new JLabel(" "), "wrap")
  add(deckSlotComboBoxes(3), "skip")
  add(deckSlotComboBoxes(4), "")
  add(deckSlotComboBoxes(5), "wrap")

  add(new JLabel(" "), "wrap")
  add(deckSlotComboBoxes(6), "skip")
  add(deckSlotComboBoxes(7), "")
  add(deckSlotComboBoxes(8), "wrap")

  add(new JLabel(" "), "wrap")
  add(new JLabel(" "), "wrap")

  val saveButton = new JButton(t("button.save_deck_slots"))
  saveButton.addActionListener(ActionListener(_ => onEDT(saveDeckSlots())))
  add(saveButton, "skip")

  val refreshButton = new JButton(t("button.refresh"))
  refreshButton.addActionListener(ActionListener(_ =>
    try {
      onEDT(updateDecks())
    } catch {
      case e1: IOException => Main.showErrorDialog("Error updating decks", e1)
    }))
  add(refreshButton, "wrap,span")

  add(new JLabel(" "), "wrap")
  add(new JLabel(" "), "wrap")
  val myDecksButton = new JButton(t("manage_decks_on_hsnet"))
  myDecksButton.addActionListener(ActionListener(_ => Browse(DECKS_URL)))

  add(myDecksButton, "skip,span")

  onEDT(updateDecks())

  def updateDecks() {
    DeckUtils.updateDecks()
    for (d <- deckSlotComboBoxes) d.applyDecks()
  }

  private def name(o: JSONObject): String = {
    Constants.hsClassOptions(Integer.parseInt(o.get("klass_id").toString)) +
      " - " +
      o.get("name").toString.toLowerCase
  }

  private def saveDeckSlots() {
    try {
      val slots = deckSlotComboBoxes map (_.selectedDeckId)
      API.setDeckSlots(slots)
      Main.showMessageDialog(this, API.message)
      updateDecks()
    } catch {
      case e: Exception => Main.showErrorDialog("Error saving deck slots", e)
    }
  }

  private def removeSlot(slot: Int): Unit = {
    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Are you sure ?", "Remove a deck", JOptionPane.YES_NO_OPTION)) {
      for (i <- slot to 8) {
        val nextIndex = deckSlotComboBoxes(i).comboBox.getSelectedIndex
        deckSlotComboBoxes(i - 1).comboBox.setSelectedIndex(nextIndex)
      }
      deckSlotComboBoxes(8).comboBox.setSelectedIndex(0)
    }
  }

  private def createDeck(d: Deck): Unit = {
    if (!d.isValid) {
      JOptionPane.showConfirmDialog(this, s"""${d.name} is not valid (${d.cardCount} cards).
          Do you want to edit it first on Heartstats.net ?""") match {
        case JOptionPane.YES_OPTION =>
          Browse(s"http://hearthstats.net/decks/${d.slug}/edit")
        case JOptionPane.NO_OPTION => doCreate()
        case _ =>
      }
    } else doCreate()

    def doCreate() = HsRobot(Config.programHelper.getHSWindowBounds).create(d)
  }

  class DeckSlotPanel(slot: Int) extends JPanel {
    setLayout(new BorderLayout)
    add(new JLabel(t("deck_slot.label", slot)), BorderLayout.NORTH)
    val comboBox = new JComboBox[Deck]
    add(comboBox, BorderLayout.CENTER)

    val removeBtn = new JButton("x") //TODO use an icon instead
    removeBtn.setToolTipText("Remove this deck and shift next ones")
    removeBtn.addActionListener(ActionListener(_ => removeSlot(slot)))
    add(removeBtn, BorderLayout.EAST)

    val createBtn = new JButton("Create")
    createBtn.setToolTipText("""<html><b>Automatically creates this deck in Hearthstone</b><br/>
						        (providing you have the required cards)<br/>
						        <br/>
						        <i>You need to be in the collection mode and select the hero yourself</i>""")
    createBtn.addActionListener(ActionListener(_ => createDeck(comboBox.getSelectedItem.asInstanceOf[Deck])))
    add(createBtn, BorderLayout.SOUTH)

    def selectedDeckId: Option[Int] =
      Option(comboBox.getSelectedItem.asInstanceOf[Deck]).map(_.id)

    def applyDecks(): Unit = {
      comboBox.removeAllItems()
      val decks = DeckUtils.getDeckLists.sortBy(d => (d.hero, d.name))
      for (deck <- decks) {
        comboBox.addItem(deck)
        if (deck.activeSlot == Some(slot)) comboBox.setSelectedItem(deck)
      }
    }
  }
}
