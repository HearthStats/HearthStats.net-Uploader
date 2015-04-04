package net.hearthstats.ui

import java.awt.BorderLayout._
import java.awt.BorderLayout
import java.io.IOException
import javax.swing.{ JButton, JComboBox, JLabel, JOptionPane, JPanel }
import net.hearthstats._
import net.hearthstats.core.{ Deck, HeroClass }
import net.hearthstats.hstatsapi.HearthStatsUrls._
import net.hearthstats.hstatsapi.{ API, DeckUtils }
import net.hearthstats.util.{ Browse, Translation }
import net.miginfocom.swing.MigLayout
import scala.swing.Swing._
import scala.util.Failure
import scala.util.Success

class DecksTab(
  translation: Translation,
  api: API,
  deckUtils: DeckUtils,
  exportDeckBox: ExportDeckBox,
  programHelper: ProgramHelper) extends JPanel {

  import translation.t

  val deckSlotComboBoxes = 1 to 9 map { new DeckSlotPanel(_) }

  setLayout(new MigLayout)

  add(new JLabel(" "), "wrap")
  add(new JLabel(t("set_your_deck_slots")), "skip, span 2")
  add(new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Decks-Tab", "Help on Decks tab").peer, "right,wrap")

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
  add(refreshButton, "wrap")

  add(new JLabel(" "), "wrap")
  add(new JLabel(" "), "wrap")
  val exportButton = new JButton(t("button.export_deck"))
  exportButton.addActionListener(ActionListener(_ => onEDT(exportDeckBox.open())))
  add(exportButton, "skip")

  add(new JLabel(" "), "wrap")
  val myDecksButton = new JButton(t("manage_decks_on_hsnet"))
  myDecksButton.addActionListener(ActionListener(_ => Browse(DECKS_URL)))
  add(myDecksButton, "skip,span")

  onEDT(updateDecks())

  def updateDecks() {
    deckUtils.updateDecks()
    for (d <- deckSlotComboBoxes) d.applyDecks()
  }

  private def saveDeckSlots() {
    val slots = deckSlotComboBoxes map (_.selectedDeckId)
    api.setDeckSlots(slots) match {
      case Success(m) =>
        Main.showMessageDialog(this, m)
        updateDecks()
      case Failure(e) => Main.showErrorDialog("Error saving deck slots", e)
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

  private def editDeck(deck: Option[Deck]): Unit = deck.map { d =>
    Browse(s"http://hearthstats.net/decks/${d.slug}/edit")
  }

  private def createDeck(in: Option[Deck]): Unit = {
    in match {
      case Some(d) => {
        if (!d.isValid) {
          JOptionPane.showConfirmDialog(this,
            s"""${d.name} is not valid (${d.cardCount} cards). Do you want to edit it first on Hearthstats.net ?""".stripMargin) match {
              case JOptionPane.YES_OPTION =>
                editDeck(in)
              case JOptionPane.NO_OPTION => doCreate(d)
              case _                     =>
            }
        } else doCreate(d)
      }
      case None => {
        JOptionPane.showMessageDialog(this,
          s"""There is no deck in this slot.
             |Choose a deck before pressing Construct""".stripMargin)
      }
    }

    def doCreate(d: Deck) = {
      programHelper.bringWindowToForeground
      HsRobot(programHelper.getHSWindowBounds).create(d)
    }
  }

  class DeckSlotPanel(slot: Int) extends JPanel {
    setLayout(new BorderLayout)
    add(new JLabel(t("deck_slot.label", slot)), NORTH)
    val comboBox = new JComboBox[Object]
    add(comboBox, CENTER)

    val removeBtn = new JButton("x") //TODO use an icon instead
    removeBtn.setToolTipText("Remove this deck and shift next ones")
    removeBtn.addActionListener(ActionListener(_ => removeSlot(slot)))
    add(removeBtn, EAST)
    
    val south=new JPanel(new BorderLayout)
    add(south,  SOUTH)

    val createBtn = new JButton("Construct")
    createBtn.setToolTipText("""<html><b>Automatically creates this deck in Hearthstone</b><br/>
                    (providing you have the required cards)<br/>
                    <br/>
                    <i>You need to be in the collection mode and select the hero yourself</i>""")
    createBtn.addActionListener(ActionListener(_ => createDeck(selectedDeck)))

    south.add(createBtn, WEST)

    val editBtn = new JButton("Edit")
    editBtn.setToolTipText("Edit this deck on Hearthstats.net (You will need to refresh this tab after completing the edit)")
    editBtn.addActionListener(ActionListener(_ => editDeck(selectedDeck)))

    south.add(editBtn, EAST)

    def selectedDeck: Option[Deck] =
      comboBox.getSelectedItem match {
        case deck: Deck => Some(deck)
        case _          => None
      }

    def selectedDeckId: Option[Int] =
      selectedDeck.map(_.id)

    def applyDecks(): Unit = {
      comboBox.removeAllItems()
      comboBox.addItem(t("deck_slot.empty"))
      val decks = deckUtils.getDecks.sortBy(d => (d.hero, d.name))
      for (deck <- decks) {
        comboBox.addItem(deck)
        if (deck.activeSlot == Some(slot)) comboBox.setSelectedItem(deck)
      }
    }
  }
}
