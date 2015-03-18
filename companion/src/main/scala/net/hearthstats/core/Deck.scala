package net.hearthstats.core

import rapture.json.jsonBackends.jawn._
import rapture.json._

case class Deck(
  id: Int = -1,
  name: String = "",
  slug: String = "",
  cards: List[Card] = List.empty,
  hero: HeroClass = HeroClass.UNDETECTED,
  activeSlot: Option[Int] = None) {

  //TODO : also check if all cards belong to the class
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

  def toJsonObject: Json =
    json"""{ 
      "klass_id": $klassId, 
      "name":$name , 
      "cardstring":$cardString  }"""

  override def toString = s"$hero: $name"
}

