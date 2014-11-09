package net.hearthstats.companion

import net.hearthstats.game.CardEvent
import net.hearthstats.game.CardEventType.{ DRAWN, REPLACED }
import net.hearthstats.game.HearthstoneLogMonitor
import net.hearthstats.ui.deckoverlay.DeckOverlaySwing
import net.hearthstats.core.Deck
import net.hearthstats.hstatsapi.CardUtils
import akka.actor.ActorSystem
import akka.actor.ActorDSL._

class DeckOverlayModule(
  presenter: DeckOverlaySwing,
  cardUtils: CardUtils,
  logMonitor: HearthstoneLogMonitor) {

  implicit val system = ActorSystem("companion")

  def show(deck: Deck): Unit = {
    presenter.showDeck(deck)

    logMonitor.addObserver {
      actor(new Act {
        val found: Receive = {
          //TODO : filter properly on player event
          case CardEvent(card, _, DRAWN, _) =>
            cardUtils.byName(card).map(presenter.removeCard)
          case CardEvent(card, _, REPLACED, _) =>
            cardUtils.byName(card).map(presenter.addCard)
          case _ =>
        }
      })
    }
  }

  def reset(): Unit = {
    presenter.reset()
  }

}