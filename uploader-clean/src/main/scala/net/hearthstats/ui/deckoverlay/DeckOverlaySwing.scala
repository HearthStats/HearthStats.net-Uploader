package net.hearthstats.ui.deckoverlay

import java.awt.{BorderLayout, Dimension}
import java.awt.event.{MouseAdapter, MouseEvent}

import scala.swing.Swing.{ChangeListener, onEDT}

import javax.swing.{ImageIcon, JCheckBox, JFrame, JLabel, WindowConstants}
import javax.swing.Box.createVerticalBox
import net.hearthstats.config.{Environment, UserConfig}
import net.hearthstats.core.{Card, Deck}
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.Log

class DeckOverlaySwing(
  config: UserConfig,
  cardUtils: CardUtils,
  environment: Environment,
  uiLog: Log) extends JFrame with DeckOverlayPresenter {

  import config._

  var cardLabels: Map[String, ClickableLabel] = Map.empty

  def showDeck(deck: Deck) {
    val imagesReady = cardUtils.downloadImages(deck.cards)

    val richCards = for {
      c <- deck.cards
      card = cardUtils.withLocalFile(c)
      localName = gameCardsTranslation.opt(card.originalName)
      richCard = card.copy(localizedName = localName)
    } yield richCard

    val content = getContentPane
    content.setLayout(new BorderLayout)
    val box = createVerticalBox
    val imageLabel = new JLabel
    imageLabel.setPreferredSize(new Dimension(289, 398))
    cardLabels =
      (for {
        card <- richCards
        cardLabel = new ClickableLabel(card, imagesReady)
      } yield {
        box.add(cardLabel)
        cardLabel.addMouseListener(new MouseHandler(card, imageLabel))
        card.originalName -> cardLabel
      }).toMap

    val cardCheckBox = new JCheckBox("Display full card image")
    cardCheckBox.addChangeListener(ChangeListener(_ => imageLabel.setVisible(cardCheckBox.isSelected)))
    content.add(cardCheckBox, BorderLayout.NORTH)
    content.add(box, BorderLayout.CENTER)
    content.add(imageLabel, BorderLayout.EAST)
    imageLabel.setVisible(false)

    setAlwaysOnTop(true)
    setFocusableWindowState(true)
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

    setLocation(deckX, deckY)
    setSize(deckWidth, deckHeight)
    setVisible(true)

  }

  override def dispose(): Unit = {
    //    connection.unsubscribe()
    try {
      // Save the location of the window, if it is visible (it won't be visible if the selected deck is invalid)
      if (isVisible) {
        val p = getLocationOnScreen
        deckX.set(p.x)
        deckY.set(p.y)
        val rect = getSize()
        deckWidth.set(rect.getWidth.toInt)
        deckHeight.set(rect.getHeight.toInt)
      }
    } catch {
      case e: Exception =>
        uiLog.warn("Error occurred trying to save your settings, your deck overlay position may not be saved", e)
    }
    super.dispose()
  }

  //  val newEvents = cardEvents.publish
  //  val connection = newEvents.connect
  //  newEvents.subscribe {
  //    _ match {
  //      case CardEvent(card, DRAWN) => findLabel(card) map (_.decreaseRemaining())
  //      case CardEvent(card, REPLACED) => findLabel(card) map (_.increaseRemaining())
  //    }
  //  }

  private def findLabel(c: Card): Option[ClickableLabel] =
    if (c.collectible) {
      val l = cardLabels.get(c.name)
      if (l.isDefined) l
      else {
        uiLog.warn(s"card ${c.name} not found in deck")
        None
      }
    } else None

  def removeCard(card: Card): Unit = {
    findLabel(card) map (_.decreaseRemaining())
  }

  def addCard(card: Card): Unit = {
    findLabel(card) map (_.increaseRemaining())
  }

  case class MouseHandler(card: Card, imageLabel: JLabel) extends MouseAdapter {
    override def mouseEntered(e: MouseEvent) {
      val localFile = environment.imageCacheFile(card.fileName).getAbsolutePath
      onEDT(imageLabel.setIcon(new ImageIcon(localFile)))
    }
  }
}

trait DeckOverlayPresenter {
  /**
   * Initial deck.
   */
  def showDeck(deck: Deck)

  /**
   * When a card is drawn.
   */
  def removeCard(card: Card)

  /**
   * When a card is replaced (ie mulligan).
   */
  def addCard(card: Card)
}