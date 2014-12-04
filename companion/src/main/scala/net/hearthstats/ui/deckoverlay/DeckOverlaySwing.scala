package net.hearthstats.ui.deckoverlay

import java.awt.event.{ MouseAdapter, MouseEvent }
import java.awt.{ BorderLayout, Dimension }
import javax.swing.Box.createVerticalBox
import javax.swing.{ ImageIcon, JCheckBox, JFrame, JLabel, WindowConstants }

import net.hearthstats.config.{ Environment, UserConfig }
import net.hearthstats.core.{ Card, Deck }
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.log.Log

import scala.swing.Swing.{ ChangeListener, onEDT }

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
    content.removeAll()
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

    content.add(box, BorderLayout.CENTER)
    content.add(imageLabel, BorderLayout.EAST)
    imageLabel.setVisible(false)

    setAlwaysOnTop(true)
    setFocusableWindowState(true)
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

    setLocation(deckX, deckY)
    setSize(deckWidth, deckHeight)
    repaint()
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

  private def findLabel(c: Card): Option[ClickableLabel] =
    if (c.collectible) {
      val l = cardLabels.get(c.name)
      if (l.isDefined) l
      else {
        uiLog.warn(s"card ${c.name} not found in deck")
        None
      }
    } else None

  def removeCard(card: Card): Unit =
    findLabel(card) map (_.decreaseRemaining())

  def addCard(card: Card): Unit =
    findLabel(card) map (_.increaseRemaining())

  def reset(): Unit =
    cardLabels.foreach { keyVal => keyVal._2.reset() }

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
  /**
   * Reset the overlay without changing the deck
   */
  def reset
}