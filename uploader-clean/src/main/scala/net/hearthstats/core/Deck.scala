package net.hearthstats.core

import org.json.simple.JSONObject
import org.apache.commons.lang3.StringUtils

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

  override def toString = s"$hero: $name"
}

