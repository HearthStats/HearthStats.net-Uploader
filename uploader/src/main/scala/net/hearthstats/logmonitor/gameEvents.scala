package net.hearthstats.logmonitor

import net.hearthstats.logmonitor.CardEventType._
import net.hearthstats.Card

sealed trait GameEvent
sealed trait HeroEvent extends GameEvent

case class CardEvent(card: Card, eventType: CardEventType) extends GameEvent

object CardEvents {
  def CardDrawn(card: Card) = CardEvent(card, DRAWN)
  def CardReplaced(card: Card) = CardEvent(card, REPLACED)
}

case class HeroDestroyedEvent(opponent: Boolean) extends HeroEvent

