package net.hearthstats.core

import rapture.json.jsonBackends.jawn._
import rapture.json._
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
      case TurnCount(c) if c > 1 =>
        copy(turns = Turn() :: turns)
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
    val inOrder = copy(turns = updatedWithNames.reverse)
    val j = Json(inOrder)
    j.toString
  }
}

case class Action(
  time: Int,
  action: String,
  card: Option[String],
  cardId: Int,
  player: Int,
  target: Option[Int] = None)

object Action {
  val HERO_POWER = "HERO_POWER"
  val TARGET = "TARGET"
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
    case e @ TargetEvent(cardCode, cardId, player, target) =>
      val name = if (cardCode == "") None else Some(e.cardName)
      copy(actions = Action(time, TARGET, name, cardId, player, Some(target)) :: actions)
    case _ => this
  }
}

