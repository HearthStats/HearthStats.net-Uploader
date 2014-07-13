package net.hearthstats

import org.json.simple.JSONObject
import Deck._
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

object Deck {

  def fromJson(json: JSONObject): Deck = {
    val id = Integer.parseInt(json.get("id").toString)
    val cardList =
      Option(json.get("cardstring")) match {
        case Some(cs) if StringUtils.isNotBlank(cs.toString) =>
          parseDeckString(cs.toString.trim)
        case _ => Nil
      }

    Deck(id = id,
      slug = json.get("slug").toString,
      name = json.get("name").toString,
      cards = cardList,
      hero = Constants.hsClassOptions(json.get("klass_id").toString.toInt),
      activeSlot = Option(json.get("slot")).map(_.toString.toInt))
  }

  def parseDeckString(ds: String): List[Card] = {
    val cardData = CardUtils.cards
    val cards = for {
      card <- ds.split(",").toList
      u = card.indexOf('_')
      count = card.substring(u + 1)
      id = Integer.parseInt(card.substring(0, u))
      cd = cardData(id)
    } yield cd.copy(count = Integer.parseInt(count))
    cards.sorted
  }
}

