package net.hearthstats.core

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.NoSuchElementException
import net.hearthstats.game._

case class GameLog(
  //Note : the head of the list is actually the last turn (order is reversed)
  turns: List[Turn] = Nil,
  firstPlayer: Option[Int] = None,
  secondPlayer: Option[Int] = None) {

  import GameLog._

  def addEvent(event: GameEvent): GameLog =
    event match {
      case MatchStart(_) => copy(turns = Turn() :: Nil)
      case FirstPlayer(_, id) =>
        copy(firstPlayer = Some(id))
      case PlayerName(_, id) if firstPlayer != Some(id) =>
        copy(secondPlayer = Some(id))
      case TurnPassedEvent =>
        turns match {
          case previous :: others =>
            val (drawn, turn) = previous.extractLastDraw
            copy(Turn(List(drawn)) :: turn :: others)
        }
      case e => turns match {
        case Nil => this
        case previous :: others => copy(turns = previous.addEvent(e, 0) :: others)
      }
    }

  def toJson: String = {
    val cardNames = (for {
      turn <- turns
      action <- turn.actions
      cardName <- action.card
    } yield action.cardId -> cardName).toMap

    val updatedWithNames = turns.map { turn =>
      turn.copy(actions = turn.actions.reverse.map { action =>
        if (action.card.isDefined) action
        else action.copy(card = cardNames.get(action.cardId))
      })
    }
    mapper.writeValueAsString(updatedWithNames.reverse)
  }
}

object GameLog {
  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
}

case class Action(time: Int, action: String, card: Option[String], cardId: Int, player: Int)

//actions are also in reverse order
case class Turn(actions: List[Action] = Nil) {
  def addEvent(ge: GameEvent, time: Int): Turn = ge match {
    case e @ CardEvent(cardCode, cardId, eventType, player) =>
      val name = if (cardCode == "") None else Some(e.cardName)
      copy(actions = Action(time, eventType.toString, name, cardId, player) :: actions)
    case _ => this
  }

  //the first card drawn is associated with the previous turn
  def extractLastDraw: (Action, Turn) = actions match {
    case previous :: others => (previous, copy(actions = others))
  }
}

