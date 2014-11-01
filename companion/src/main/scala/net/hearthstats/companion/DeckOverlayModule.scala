package net.hearthstats.companion

import net.hearthstats.game.CardEvent
import net.hearthstats.game.CardEventType.{ DRAWN, REPLACED }
import net.hearthstats.game.HearthstoneLogMonitor
import net.hearthstats.ui.deckoverlay.DeckOverlaySwing
import rx.lang.scala.Subscription
import net.hearthstats.core.Deck

class DeckOverlayModule(presenter: DeckOverlaySwing, logMonitor: HearthstoneLogMonitor) {
  var lastSubscription: Option[Subscription] = None

  def show(deck: Deck): Unit = {
    lastSubscription.map(_.unsubscribe())

    presenter.showDeck(deck)

    lastSubscription = Some(logMonitor.cardEvents.subscribe {
      _ match {
        case CardEvent(card, DRAWN) => presenter.removeCard(card)
        case CardEvent(card, REPLACED) => presenter.addCard(card)
      }
    })

  }

  def reset(): Unit = {
    presenter.reset()
  }

}