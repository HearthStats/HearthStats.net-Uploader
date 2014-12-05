package net.hearthstats.core

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.NoSuchElementException
import net.hearthstats.game._

case class GameLog(
  //Note : the head of the list is actually the last turn (order is reversed)
  turns: List[Turn] = List(Turn()),
  firstPlayer: Option[Int] = None,
  secondPlayer: Option[Int] = None,
  firstPlayerName: Option[String] = None,
  secondPlayerName: Option[String] = None) {

  import GameLog._

  def addEvent(event: GameEvent, timeMs: Int = 0): GameLog =
    event match {
      case FirstPlayer(name) =>
        copy(firstPlayerName = Some(name))
      case PlayerName(name, id) if firstPlayerName == Some(name) =>
        copy(firstPlayer = Some(id))
      case PlayerName(name, id) if firstPlayerName != Some(name) =>
        copy(secondPlayer = Some(id), secondPlayerName = Some(name))
      case TurnCount(t) if t > 1 =>
        turns match {
          case previous :: others =>
            val (drawn, turn) = previous.extractLastDraw
            copy(Turn(List(drawn)) :: turn :: others)
          case Nil => ???
        }
      case e =>
        turns match {
          case Nil => this
          case previous :: others => copy(turns = previous.addEvent(e, timeMs) :: others)
        }
    }

  def toJson: String = {
    val cardNames = (for {
      turn <- turns
      action <- turn.actions
      cardName <- action.card
    } yield action.cardId -> cardName).toMap
    //got the name of the card drawn by opponent and revealed later

    val updatedWithNames = turns.map { turn =>
      turn.copy(actions = turn.actions.reverse.map { action =>
        if (action.card.isDefined) action
        else action.copy(card = cardNames.get(action.cardId))
      })
    }
    mapper.writeValueAsString(copy(turns = updatedWithNames.reverse))
  }
}

object GameLog {
  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
}

case class Action(time: Int, action: String, card: Option[String], cardId: Int, player: Int)

object Action {
  val HERO_POWER = "HERO_POWER"
}

import Action._

//actions are also in reverse order
case class Turn(actions: List[Action] = Nil) {
  def addEvent(ge: GameEvent, time: Int): Turn = ge match {
    case e @ CardEvent(cardCode, cardId, eventType, player) =>
      val name = if (cardCode == "") None else Some(e.cardName)
      copy(actions = Action(time, eventType.toString, name, cardId, player) :: actions)
    case e @ HeroPowerEvent(cardCode, player) if e.isValid =>
      val name = Some(e.cardName)
      val powerUsed = Action(time, HERO_POWER, name, -1, player)
      // hero power appears twice in the logs
      if (actions.contains(powerUsed)) this
      else copy(actions = powerUsed :: actions)
    case _ => this
  }

  //the first card drawn is associated with the previous turn
  def extractLastDraw: (Action, Turn) = actions match {
    case previous :: others => (previous, copy(actions = others))
    case Nil => ???
  }
}

