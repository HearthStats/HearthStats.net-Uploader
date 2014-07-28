package net.hearthstats.game

import CardEventType._
import net.hearthstats.core.Card

sealed trait GameEvent
sealed trait HeroEvent extends GameEvent

case class CardEvent(card: Card, eventType: CardEventType) extends GameEvent

object CardEvents {
  def CardDrawn(card: Card) = CardEvent(card, DRAWN)
  def CardReplaced(card: Card) = CardEvent(card, REPLACED)
}

case class HeroDestroyedEvent(opponent: Boolean) extends HeroEvent

object ArenaRunEnd extends GameEvent
object StartingHand extends GameEvent
object FirstTurn extends GameEvent