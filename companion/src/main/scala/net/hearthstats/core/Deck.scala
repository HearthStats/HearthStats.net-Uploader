package net.hearthstats.core

import org.json.simple.JSONObject

import scala.collection.JavaConversions._

case class Deck(
  id: Int = -1,
  name: String = "",
  slug: String = "",
  cards: List[Card] = List.empty,
  hero: String = "Shaman",
  activeSlot: Option[Int] = None) {

  def isValid =
    cards != null && 30 == cardCount

  def cardCount =
    cards.map(_.count).sum

  def klassId: Int = {
    HeroClass.values().find(_.name() == hero).map(_.ordinal()).getOrElse(0)
  }

  def cardString: String = {
    (for (c <- cards.sortBy(_.id)) yield s"${c.id}_${c.count}").mkString(",")
  }

  def deckString: String = {
    (for (c <- cards.sortBy(card => (card.cost, -card.typeId, card.name)))
    yield s"${c.count} ${c.originalName}").mkString("\n")
  }

  def toJsonObject: JSONObject = {
    return new JSONObject(collection.mutable.Map(
      "klass_id" -> klassId,
      "name" -> name,
      "notes" -> "",
      "cardstring" -> cardString
    ))
  }

  override def toString = s"$hero: $name"
}

