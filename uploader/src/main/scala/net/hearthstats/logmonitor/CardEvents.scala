package net.hearthstats.logmonitor

import net.hearthstats.logmonitor.CardEventType._
import net.hearthstats.Card

object CardEvents {
  def CardDrawn(card: Card) = CardEvent(card, DRAWN)
  def CardReplaced(card: Card) = CardEvent(card, REPLACED)
}

case class CardEvent(card: Card, eventType: CardEventType)

