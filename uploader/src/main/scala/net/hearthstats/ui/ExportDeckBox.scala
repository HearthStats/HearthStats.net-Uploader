package net.hearthstats.ui

import java.awt.{Dimension, Font}
import java.util.{Observable, Observer}
import javax.imageio.ImageIO
import javax.swing._

import grizzled.slf4j.Logging
import net.hearthstats.Monitor
import net.hearthstats.analysis.{AnalyserEvent, HearthstoneAnalyser}
import net.hearthstats.log.Log
import net.hearthstats.state.Screen
import net.hearthstats.ui.util.MigPanel
import net.hearthstats.util.HsRobot
import net.hearthstats.util.Translations.t
import zulu.deckexport.extracter.ExtracterMain

import scala.swing._
import scala.swing.event.ButtonClicked

class ExportDeckBox(val monitor: Monitor) extends Frame with Observer with Logging {

  var hasDeck = false

  minimumSize = ExportDeckBox.minSizeWithoutDeck
  preferredSize = ExportDeckBox.minSizeWithoutDeck
  maximumSize = ExportDeckBox.maxSize

  title = t("export.heading") + " - HearthStats Companion"

  val panel = new MigPanel(
    layoutConstraints = "hidemode 2",
    colConstraints = "12[]12[]8[grow,fill]12",
    rowConstraints = "12[]8[]8[]12[]8[grow]12[]12"
  ) {

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
    contents += (nameField, "wrap")

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
        ExportDeckBox.close()
      case ButtonClicked(`exportButton`) =>
        debug("Exporting deck")
    }

    def disableDeck() = {
      nameLabel.visible = false
      nameField.visible = false
      cardLabel.visible = false
      cardScrollPane.visible = false
      exportButton.enabled = false
      resize(ExportDeckBox.minSizeWithoutDeck)
    }

    def enableDeck() = {
      nameLabel.visible = true
      nameField.visible = true
      cardLabel.visible = true
      cardScrollPane.visible = true
      exportButton.enabled = true
      resize(ExportDeckBox.minSizeWithDeck)
    }
  }
  contents = panel

  HearthstoneAnalyser.addObserver(this)

  // Set the initial status based on the current screen
  setScreen(HearthstoneAnalyser.screen)


  override def closeOperation {
    debug("Closing deck export window")
    HearthstoneAnalyser.deleteObserver(this)
  }


  // Observe the monitor class to determine which screen Hearthstone is on
  override def update(o: Observable, change: scala.Any): Unit = {
    change.asInstanceOf[AnalyserEvent] match {
      case AnalyserEvent.SCREEN =>
        setScreen(HearthstoneAnalyser.screen)
      case _ =>
    }
  }


  def setScreen(screen: Screen) = if (!hasDeck) screen match {
    case Screen.COLLECTION_DECK =>
      captureDeck()
    case Screen.COLLECTION | Screen.COLLECTION_ZOOM =>
      setStatus(t("export.status.not_ready"), t("export.instructions.no_deck"))
    case _ =>
      setStatus(t("export.status.not_ready"), t("export.instructions.no_collection"))
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

  private def captureDeck() = {
    // Assume we have a deck until proved otherwise, this prevents events from resetting the status during the capture
    hasDeck = true

    setStatus(t("export.status.detecting"), t("export.instructions.detecting"))

    val hsHelper = monitor._hsHelper
    hsHelper.bringWindowToForeground

    val robot = HsRobot(monitor._hsHelper.getHSWindowBounds)
    robot.collectionScrollAway()
    val img1 = hsHelper.getScreenCapture
    robot.collectionScrollTowards()
    val img2 = hsHelper.getScreenCapture

    val deck = ExtracterMain.exportDeck(img1, img2)

    peer.toFront()

    val deckString =
      if (deck == null) {
        // Failed to capture a deck
        setStatus(t("export.status.error"), t("export.instructions.error"))
        Log.info("Could not export deck")
        hasDeck = false
        ""
      } else {
        // Successfully captured a deck
        setStatus(t("export.status.ready"), t("export.instructions.ready"))
        panel.enableDeck()
        deck.toArray.mkString("\n")
      }

    panel.cardTextArea.text = deckString
  }

}


object ExportDeckBox {

  val minSizeWithoutDeck = new Dimension(450, 200)
  val minSizeWithDeck = new Dimension(450, 400)
  val maxSize = new Dimension(450, 800)

  var currentWindow: Option[ExportDeckBox] = None

  def open(monitor: Monitor): ExportDeckBox = currentWindow match {
    // Redisplay existing window, if there is one
    case Some(box) => open(box)
    // Otherwise create a new window and display it
    case None => {
      currentWindow = Some(new ExportDeckBox(monitor))
      open(currentWindow.get)
    }
  }

  private def open(box: ExportDeckBox): ExportDeckBox = {
    box.open()
    box.pack()
    box.peer.toFront()
    box
  }

  def close() = currentWindow match {
    case Some(box) =>
      box.close()
      box.dispose()
      currentWindow = None
    case None =>
  }

}