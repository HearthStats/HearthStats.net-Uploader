package net.hearthstats

import java.text.ParseException

import org.apache.commons.lang3.StringUtils
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
    (0 until Constants.hsClassOptions.length).find(Constants.hsClassOptions(_) == hero).getOrElse(0)
  }
  
  def cardString: String = {
    (for (c <- cards.sortBy(_.id)) yield s"${c.id}_${c.count}").mkString(",")
  }

  def deckString: String = {
    (for (c <- cards.sortBy(card => (card.cost, card.name)))
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

object Deck {

  def fromJson(json: JSONObject): Deck = {
    val id = Integer.parseInt(json.get("id").toString)
    val cardList =
      Option(json.get("cardstring")) match {
        case Some(cs) if StringUtils.isNotBlank(cs.toString) =>
          parseCardString(cs.toString.trim)
        case _ => Nil
      }

    val klassId = json.get("klass_id")
    val heroString = if (klassId == null) "" else Constants.hsClassOptions(klassId.toString.toInt)

    Deck(id = id,
      slug = json.get("slug").toString,
      name = json.get("name").toString,
      cards = cardList,
      hero = heroString,
      activeSlot = Option(json.get("slot")).map(_.toString.toInt))
  }

  def parseCardString(ds: String): List[Card] = {
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

  def parseDeckString(ds: String): List[Card] = {
    val cardMap = CardUtils.cards.values.map(c => c.name -> c).toMap
    val cards = for {
      card <- ds.split("\n").toList
      count = try {
        Integer.parseInt(card.substring(0, 1))
      } catch {
        case e: NumberFormatException => 0
      }
      cd = cardMap.get(card.substring(2))
    } yield cd match {
        case Some(c) =>
          c.copy(count = count)
        case None =>
          // If the card is invalid, then return a made-up card. This might be better implemented as a different class
          // to indicate that the card couldn't be identified
          new Card(id = 0, originalName = card, collectible = false)
      }
    cards.sorted
  }
}

