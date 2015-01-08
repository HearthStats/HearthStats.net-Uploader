package net.hearthstats.ui.deckoverlay

import java.awt.{ BorderLayout, Dimension }

import javax.swing.{ JFrame, JLabel, WindowConstants }
import javax.swing.Box.createVerticalBox
import net.hearthstats.config.{ Environment, RectangleConfig }
import net.hearthstats.config.UserConfig.configToValue
import net.hearthstats.core.{ Card, Deck }
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.log.Log
import net.hearthstats.util.Translation

abstract class DeckOverlaySwing(
  rectangleConfig: RectangleConfig,
  cardsTranslation: Translation,
  cardUtils: CardUtils,
  environment: Environment,
  uiLog: Log) extends JFrame with DeckOverlayPresenter {

  var cardLabels: Map[String, ClickableLabel] = Map.empty
  var deck: Deck = Deck()

  def showDeck(deck: Deck) {
    this.deck = deck
    setTitle(deck.name)
    val imagesReady = cardUtils.downloadImages(deck.cards)

    val richCards = for {
      c <- deck.cards
      card = cardUtils.withLocalFile(c)
      localName = cardsTranslation.opt(card.originalName)
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
        card.originalName -> cardLabel
      }).toMap

    content.add(box, BorderLayout.CENTER)
    content.add(imageLabel, BorderLayout.EAST)
    imageLabel.setVisible(false)

    setAlwaysOnTop(true)
    setFocusableWindowState(true)
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

    setLocation(rectangleConfig.x, rectangleConfig.y)
    setSize(rectangleConfig.width, rectangleConfig.height)
    repaint()
    setVisible(true)
  }

  override def dispose(): Unit = {
    try {
      // Save the location of the window, if it is visible (it won't be visible if the selected deck is invalid)
      if (isVisible) {
        val p = getLocationOnScreen
        rectangleConfig.x.set(p.x)
        rectangleConfig.y.set(p.y)
        val rect = getSize
        rectangleConfig.width.set(rect.getWidth.toInt)
        rectangleConfig.height.set(rect.getHeight.toInt)
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

  def decreaseCardCount(card: Card): Unit =
    findLabel(card) map (_.decreaseRemaining())

  def increaseCardCount(card: Card): Unit =
    findLabel(card) map (_.increaseRemaining())

  def reset(): Unit =
    cardLabels.foreach { keyVal => keyVal._2.reset() }

}

trait DeckOverlayPresenter {
  /**
   * Initial deck.
   */
  def showDeck(deck: Deck)

  /**
   * When a card is drawn.
   */
  def decreaseCardCount(card: Card)

  /**
   * When a card is replaced (ie mulligan).
   */
  def increaseCardCount(card: Card)
  /**
   * Reset the overlay without changing the deck
   */
  def reset
}
