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
import net.hearthstats.game.TurnStart
import net.hearthstats.game.TurnCount

class DeckOverlayModule(
  presenter: DeckOverlaySwing,
  cardUtils: CardUtils,
  logMonitor: HearthstoneLogMonitor) extends Logging {

  val monitoringActors = ListBuffer.empty[ActorRef]
  var count = 0

  def show(deck: Deck): Unit = {
    presenter.showDeck(deck)
  }

  def clearAll(): Unit = {
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
    count += 1
    val monitoringActor = newActor(playerId)
    monitoringActors += monitoringActor
    logMonitor.addObserver(monitoringActor)

  }

  def newActor(playerId: Int): ActorRef = {
    count += 1
    implicit val actorSystem = logMonitor.system
    actor(s"DeckOverlay$count")(new Act {
      val openingHand = ListBuffer.empty[String]

      val initial: Receive = { // handle mulligan
        case CardEvent(cardCode, _, RECEIVED | DRAWN, `playerId`) =>
          openingHand += cardCode
        case CardEvent(cardCode, _, REPLACED, `playerId`) =>
          openingHand -= cardCode
        case TurnCount(2) =>
          for {
            cardCode <- openingHand
            card <- cardUtils.byCode(cardCode)
          } {
            presenter.removeCard(card)
          }
          become(inGame)
        case _ =>
      }

      val inGame: Receive = {
        case CardEvent(cardCode, _, DISCARDED_FROM_DECK | PLAYED_FROM_DECK | DRAWN, `playerId`) =>
          cardUtils.byCode(cardCode).map(presenter.removeCard)
        case _ =>
      }

      become(initial)
    })
  }

  def reset(): Unit = {
    presenter.reset()
  }

}