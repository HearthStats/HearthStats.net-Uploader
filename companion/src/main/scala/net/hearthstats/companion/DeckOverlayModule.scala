package net.hearthstats.companion

import net.hearthstats.core.Deck
import net.hearthstats.game.CardEvent
import net.hearthstats.game.CardEventType._
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
  var count = 0

  def show(deck: Deck): Unit = {
    presenter.showDeck(deck)
  }

  def clearAll() {
    reset()
    for (a <- monitoringActors) {
      a ! PoisonPill
      logMonitor.removeObserver(a)
    }
    monitoringActors.clear()
  }

  def startMonitoringCards(playerId: Int): Unit = {
    info(s"monitoring cards for player $playerId")
    clearAll()
    implicit val actorSystem = logMonitor.system
    count += 1
    val monitoringActor = actor(s"DeckOverlay$count")(new Act {
      become {
        case CardEvent(cardCode, _, evtType, `playerId`) if Seq(DISCARDED_FROM_DECK, DRAWN) contains evtType =>
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