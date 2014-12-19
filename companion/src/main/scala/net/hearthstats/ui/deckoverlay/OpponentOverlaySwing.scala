package net.hearthstats.ui.deckoverlay

import com.softwaremill.macwire.Tagging.{ @@ => @@ }

import net.hearthstats.config.{ Environment, RectangleConfig }
import net.hearthstats.core.Card
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.log.Log
import net.hearthstats.util.Translation

class OpponentOverlaySwing(
  opponentConfig: RectangleConfig @@ OpponentDeckOverlayRectangle,
  cardsTranslation: Translation,
  cardUtils: CardUtils,
  environment: Environment,
  uiLog: Log)

  extends DeckOverlaySwing(
    opponentConfig,
    cardsTranslation,
    cardUtils,
    environment,
    uiLog) {

  /**
   * When a card is revealed by opponent.
   */
  def addCard(card: Card): Unit = {
    dispose()
    val c = deck.cards.find(_.id == card.id).getOrElse(card).addOne
    val cards = c :: deck.cards.filter(_.id != c.id)
    showDeck(deck.copy(cards = cards.sorted))
  }

}

trait OpponentDeckOverlayRectangle