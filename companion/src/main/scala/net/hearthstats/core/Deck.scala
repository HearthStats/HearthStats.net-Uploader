package net.hearthstats.core

import org.json.simple.JSONObject

import scala.collection.JavaConversions._

case class Deck(
  id: Int = -1,
  name: String = "",
  slug: String = "",
  cards: List[Card] = List.empty,
  hero: HeroClass = HeroClass.UNDETECTED,
  activeSlot: Option[Int] = None) {

  lazy val isValid: Boolean =
    cards != null && 30 == cardCount

  lazy val cardCount: Int =
    cards.map(_.count).sum

  lazy val klassId: Int = hero.ordinal

  lazy val cardString: String =
    (for (c <- cards.sortBy(_.id))
      yield s"${c.id}_${c.count}")
      .mkString(",")

  def deckString: String =
    (for (c <- cards.sortBy(card => (card.cost, -card.typeId, card.name)))
      yield s"${c.count} ${c.originalName}").mkString("\n")

  def toJsonObject: JSONObject =
    new JSONObject(collection.mutable.Map(
      "klass_id" -> klassId,
      "name" -> name,
      "notes" -> "",
      "cardstring" -> cardString))

  override def toString = s"$hero: $name"
}

