package net.hearthstats.companion

import net.hearthstats.core.Deck
import net.hearthstats.game.CardEvent
import net.hearthstats.game.CardEventType.{ DRAWN, REPLACED }
import net.hearthstats.game.HearthstoneLogMonitor
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.deckoverlay.DeckOverlaySwing
import grizzled.slf4j.Logging
import net.hearthstats.game.GameOver
import akka.actor.PoisonPill
import akka.actor.ActorDSL._
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef

class DeckOverlayModule(
  presenter: DeckOverlaySwing,
  cardUtils: CardUtils,
  logMonitor: HearthstoneLogMonitor) extends Logging {

  val monitoringActors = ListBuffer.empty[ActorRef]

  def show(deck: Deck): Unit = {
    presenter.showDeck(deck)
  }

  def startMonitoringCards(playerId: Int): Unit = {
    info(s"monitoring cards for player $playerId")
    reset()
    monitoringActors.foreach(_ ! PoisonPill)
    monitoringActors.clear()
    implicit val actorSystem = logMonitor.system
    val monitoringActor = actor(new Act {
      become {
        case CardEvent(cardCode, _, DRAWN, `playerId`) =>
          cardUtils.byCode(cardCode).map(presenter.removeCard)
        case CardEvent(cardCode, _, REPLACED, `playerId`) =>
          cardUtils.byCode(cardCode).map(presenter.addCard)
        case _ =>
      }
    })
    monitoringActors.append(monitoringActor)
    logMonitor.addObserver(monitoringActor)

  }

  def reset(): Unit = {
    presenter.reset()
  }

}