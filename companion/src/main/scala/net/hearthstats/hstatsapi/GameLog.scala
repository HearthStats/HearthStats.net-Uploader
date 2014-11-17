package net.hearthstats.core

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.NoSuchElementException
import net.hearthstats.game.GameEvent
import net.hearthstats.game.CardEvent

object GameLog {

  case class Action(time: Int, action: String, card: Option[String], cardId: Int, player: Int)

  case class Turn(player: Int, actions: List[Action]) {
    def addEvent(ge: GameEvent, time: Int): Turn = ge match {
      case e @ CardEvent(cardCode, cardId, eventType, player) =>
        val name = if (cardCode == "") None else Some(e.cardName)
        copy(actions = actions ::: List(Action(time, eventType.toString, name, cardId, player)))
      case _ => this
    }
  }

  def gameLogToString(gamelog: List[Turn]): String = {
    val cardNames = (for {
      turn <- gamelog
      action <- turn.actions
      cardName <- action.card
    } yield action.cardId -> cardName).toMap
    val updatedWithNames = gamelog.map { turn =>
      turn.copy(actions = turn.actions.map { action =>
        if (action.card.isDefined) action
        else action.copy(card = cardNames.get(action.cardId))
      })
    }

    mapper.writeValueAsString(updatedWithNames)
  }

  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

}