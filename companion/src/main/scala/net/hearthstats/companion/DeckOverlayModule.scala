package net.hearthstats.companion

import scala.collection.mutable.ListBuffer

import akka.actor.{ ActorRef, PoisonPill, actorRef2Scala }
import akka.actor.ActorDSL.{ Act, actor }
import grizzled.slf4j.Logging
import net.hearthstats.core.Deck
import net.hearthstats.game.{ HearthstoneLogMonitor, TurnCount }
import net.hearthstats.game.CardEvent
import net.hearthstats.game.CardEventType._
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.ui.deckoverlay.{ OpponentOverlaySwing, UserOverlaySwing, ClickableLabel }


class DeckOverlayModule(
  userPresenter: UserOverlaySwing,
  opponentPresenter: OpponentOverlaySwing,
  cardUtils: CardUtils,
  logMonitor: HearthstoneLogMonitor) extends Logging {

  val monitoringActors = ListBuffer.empty[ActorRef]
  var count = 0
  
  
  def show(deck: Deck): Unit = {    
    userPresenter.showDeck(deck)
  }

  def showOpponentDeck(opponentName: String): Unit = {
    opponentPresenter.showDeck(Deck(name = opponentName))
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
          if(cardCode != "GAME_005"){
            userPresenter.decreaseCardsLeft()  
            println(cardCode + " decreased ")}
        case CardEvent(cardCode, _, REPLACED, `playerId`) =>
          openingHand -= cardCode
        case TurnCount(2) =>
          for {
            cardCode <- openingHand
            card <- cardUtils.byCode(cardCode)
          } {
            userPresenter.decreaseCardCount(card)
          }
          become(handleOppCards orElse inGame)
        case _ =>
      }

      val inGame: Receive = {
        case CardEvent(cardCode, _, DISCARDED_FROM_DECK | PLAYED_FROM_DECK | DRAWN | DISCARDED, `playerId`) =>
          cardUtils.byCode(cardCode).map(userPresenter.decreaseCardCount)
          if(cardCode != "GAME_005")userPresenter.decreaseCardsLeft()
        case _ =>
      }

      val handleOppCards: Receive = {
        case CardEvent(cardCode, _, DISCARDED_FROM_DECK | PLAYED_FROM_DECK | PLAYED | REVEALED, p) if p != playerId =>
          for (c <- cardUtils.byCode(cardCode) if c.collectible) {
            opponentPresenter.addCard(c)
          }
        case CardEvent(cardCode, _, RETURNED, p) if p != playerId =>
          for (c <- cardUtils.byCode(cardCode) if c.collectible) {
            opponentPresenter.decreaseCardCount(c)
          }
      }

      become(handleOppCards orElse initial)
    })
  }

  def reset(): Unit = {
    userPresenter.reset()
  }
  
  def dispose()
  {
    userPresenter.dispose()
    opponentPresenter.dispose()
  }

}
