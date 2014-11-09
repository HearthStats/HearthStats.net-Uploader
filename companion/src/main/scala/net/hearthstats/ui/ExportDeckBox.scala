package net.hearthstats.ui

import java.awt.{ Dimension, Font }
import scala.collection.JavaConversions.mutableMapAsJavaMap
import scala.swing.{ BoxPanel, Frame, Label, Orientation, ScrollPane, TextArea, TextField }
import scala.swing.event.ButtonClicked
import scala.util.control.NonFatal
import org.json.simple.JSONObject
import grizzled.slf4j.Logging
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import net.hearthstats.{ Main, ProgramHelper }
import net.hearthstats.companion.ScreenEvents
import net.hearthstats.core.{ Deck, HeroClass }
import net.hearthstats.game.{ CollectionDeckScreen, CollectionScreen, ScreenEvent }
import net.hearthstats.game.imageanalysis.DeckAnalyser
import net.hearthstats.hstatsapi.{ API, CardUtils, DeckUtils }
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.util.MigPanel
import net.hearthstats.util.Translation
import scala.swing.ComboBox
import net.hearthstats.game.HearthstoneLogMonitor
import net.hearthstats.companion.ScreenEvents

class ExportDeckBox(
  screenEvents: ScreenEvents,
  programHelper: ProgramHelper,
  uiLog: Log,
  cardUtils: CardUtils,
  deckUtils: DeckUtils,
  hsAPI: API,
  translation: Translation) extends Logging {
  import translation.t

  val minSizeWithoutDeck = new Dimension(450, 200)
  val minSizeWithDeck = new Dimension(450, 400)
  val maxSize = new Dimension(450, 800)

  var currentWindow: Option[ExportDeckBoxImpl] = None

  def open(): Option[ExportDeckBoxImpl] = try {
    currentWindow match {
      // Redisplay existing window, if there is one
      case Some(box) => Some(open(box))
      // Otherwise create a new window and display it
      case None => {
        currentWindow = Some(new ExportDeckBoxImpl())
        Some(open(currentWindow.get))
      }
    }
  } catch {
    case e: Exception =>
      uiLog.warn(s"Unable to open export popup due to error: ${e.getMessage}", e)
      None
  }

  private def open(box: ExportDeckBoxImpl): ExportDeckBoxImpl = {
    box.open()
    box.pack()
    box.peer.toFront()
    box
  }

  def closeCurrent() = currentWindow match {
    case Some(box) =>
      box.close()
      box.dispose()
      currentWindow = None
    case None =>
  }

  class ExportDeckBoxImpl() extends Frame with Logging {

    var hasDeck = false

    minimumSize = minSizeWithoutDeck
    preferredSize = minSizeWithDeck
    maximumSize = maxSize

    title = t("export.heading") + " - HearthStats Companion"

    // Array of hero classes
    val localizedClassOptions = Array.ofDim[String](HeroClass.values.length)
    localizedClassOptions(0) = ""
    for (i <- 1 until localizedClassOptions.length) localizedClassOptions(i) = t(HeroClass.stringWithId(i))

    val panel = new MigPanel(
      layoutConstraints = "hidemode 3",
      colConstraints = "12[]12[]8[grow,fill]12",
      rowConstraints = "12[]8[]8[]12[]8[grow]12[]12") {

      // Heading
      contents += new Label {
        icon = new ImageIcon(ImageIO.read(getClass.getResource("/images/icon_32px.png")))
      }
      contents += (new Label {
        text = t("export.heading")
        font = font.deriveFont(16f)
      }, "span 2, wrap")

      // Status
      val status = new Label {
        text = t("export.status.not_ready")
        font = font.deriveFont(Font.BOLD)
        override def text_=(s: String): Unit = super.text_=(t("export.label.status") + " " + s)
      }
      contents += (status, "skip, span 2, wrap")

      // Instructions
      val instructions = new Label {
        text = ""
        // Using HTML as a simple way to make the label wrap
        override def text_=(s: String): Unit = super.text_=("<html><body style='width:100%'>" + s + "</body></html>")
      }
      contents += (instructions, "skip, span 2, wrap")

      // Deck Name
      val nameLabel = new Label {
        text = t("export.label.deckname")
        visible = false
      }
      contents += (nameLabel, "span 2, top, right, hmin 26, pad 2 0 0 0")
      val nameField = new TextField {
        visible = false
      }
      contents += (nameField, "top, wrap")

      // Class
      val classLabel = new Label {
        text = t("export.label.class")
        visible = false
      }
      contents += (classLabel, "span 2, top, right, hmin 26, pad 2 0 0 0")
      val classComboBox = new ComboBox(localizedClassOptions) {
        visible = false
      }
      contents += (classComboBox, "top, wrap")

      // Cards
      val cardLabel = new Label {
        text = t("export.label.cards")
        visible = false
      }
      contents += (cardLabel, "span 2, top, right, hmin 26, pad 2 0 0 0")
      val cardTextArea = new TextArea
      val cardScrollPane = new ScrollPane {
        contents = cardTextArea
        border = nameField.border
        visible = false
      }
      contents += (cardScrollPane, "grow, hmin 80, gapleft 3, gapright 3, wrap")

      // Buttons
      val cancelButton = new swing.Button {
        text = t("button.cancel")
      }
      listenTo(cancelButton)
      defaultButton = cancelButton
      val exportButton = new swing.Button {
        text = t("button.export")
        enabled = false
      }
      listenTo(exportButton)
      contents += (new BoxPanel(Orientation.Horizontal) {
        contents += cancelButton
        contents += exportButton
      }, "skip, span 2, right")

      reactions += {
        case ButtonClicked(`cancelButton`) =>
          debug("Cancelling deck export")
          closeCurrent();
        case ButtonClicked(`exportButton`) =>
          debug("Exporting deck")
          exportDeck()
      }

      def disableDeck() = {
        nameLabel.visible = false
        nameField.visible = false
        classLabel.visible = false
        classComboBox.visible = false
        cardLabel.visible = false
        cardScrollPane.visible = false
        exportButton.enabled = false
        resize(minSizeWithoutDeck)
      }

      def enableDeck() = {
        nameLabel.visible = true
        nameField.visible = true
        classLabel.visible = true
        classComboBox.visible = true
        cardLabel.visible = true

        cardScrollPane.visible = true
        exportButton.enabled = true
        resize(minSizeWithDeck)
      }
    }
    contents = panel

    screenEvents.addReceive {
      case e: ScreenEvent => handleScreenEvent(e)
    }

    override def closeOperation {
      info("Closing deck export window")
      //      screenEventSubscription.unsubscribe()
      closeCurrent()
    }

    /**
     * Update status and instructions based on the screen currently being viewed in Hearthstone.
     * @param evt A ScreenEvent indicating the current screen in Hearthstone
     */
    def handleScreenEvent(evt: ScreenEvent): Unit = try {
      if (!hasDeck) evt.screen match {
        case CollectionDeckScreen =>
          captureDeck()
        case CollectionScreen =>
          setStatus(t("export.status.not_ready"), t("export.instructions.no_deck"))
        case _ =>
          setStatus(t("export.status.not_ready"), t("export.instructions.no_collection"))
      }
    } catch {
      case NonFatal(t) =>
        error(t.getMessage, t)
        uiLog.error(t.getMessage, t)
    }

    private def setStatus(s: String, i: String) = {
      panel.status.text = s
      panel.instructions.text = i
      pack()
    }

    private def resize(dimension: Dimension): Unit = {
      minimumSize = dimension
      preferredSize = dimension
      pack()
    }

    private def captureDeck(): Unit = {
      // Assume we have a deck until proved otherwise, this prevents events from resetting the status during the capture
      hasDeck = true

      setStatus(t("export.status.detecting"), t("export.instructions.detecting"))

      var deck: Option[Deck] = None

      var attempts = 0;
      while (deck == None && attempts < 4) {
        attempts += 1
        debug(s"Attempt $attempts at identifying deck")

        programHelper.bringWindowToForeground

        val robot = HsRobot(programHelper.getHSWindowBounds)
        robot.collectionScrollAway()
        val img1 = programHelper.getScreenCapture
        robot.collectionScrollTowards()
        val img2 = programHelper.getScreenCapture

        // Create a list of collectible cards only - non-collectible cards cannot be in constructed decks
        val cardList = cardUtils.cards.values.filter(c => c.collectible).toList

        val deckAnalyser = new DeckAnalyser(cardList, img1.getWidth, img1.getHeight)
        deck = deckAnalyser.identifyDeck(img1, img2)
      }

      peer.toFront()

      val deckString = deck match {
        case Some(d) =>
          // Successfully captured a deck
          setStatus(t("export.status.ready"), t("export.instructions.ready"))

          panel.nameField.text = d.name
          panel.classComboBox.selection.item = d.hero

          panel.enableDeck()
          d.deckString

        case None =>
          // Failed to capture a deck
          setStatus(t("export.status.error"), t("export.instructions.error"))
          uiLog.info("Could not export deck")
          hasDeck = false
          ""
      }

      panel.cardTextArea.text = deckString
    }

    private def exportDeck(): Unit = {
      // Attempt to convert the deck string back into card objects, which may fail if the string was edited badly
      val cards = deckUtils.parseDeckString(panel.cardTextArea.text.trim)
      val invalidCards = cards.filter(_.id == 0)

      if (invalidCards.length > 0) {
        // At least one card couldn't be recognised
        val details = invalidCards.map(_.name).mkString("\n")
        Main.showMessageDialog(this.peer, if (invalidCards.length == 1)
          s"Could not recognise this card:\n$details"
        else
          s"Could not recognise these cards:\n$details")
      } else {
        // All cards were recognised
        val deck = new Deck(
          name = panel.nameField.text.trim,
          cards = cards,
          hero = panel.classComboBox.selection.item)

        if (deck.isValid) {
          val jsonDeck = new JSONObject(collection.mutable.Map("deck" -> deck.toJsonObject))
          hsAPI.createDeck(jsonDeck) match {
            case true =>
              // Deck was loaded onto HearthStats.net successfully
              Main.showMessageDialog(peer, s"Deck ${deck.name} was exported to HearthStats.net successfully")
              closeCurrent()
            case false =>
              // An error occurred loading the deck onto HearthStats.net
              setStatus(t("export.status.error"), t("export.instructions.error"))
              peer.toFront()
          }
        } else {
          Main.showMessageDialog(this.peer, "Could not export because deck is invalid.\n" +
            s"Deck has ${deck.cardCount} cards, 30 are required.")
        }
      }

    }
  }
}
