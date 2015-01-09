package net.hearthstats.hstatsapi

import java.io.IOException
import net.hearthstats.core.{ Card, Deck, HeroClass }
import net.hearthstats.ui.log.Log
import org.apache.commons.lang3.StringUtils
import scala.collection.JavaConversions.seqAsJavaList
import rapture.json._
import rapture.json.jsonBackends.jawn._
import scala.util.Success

class DeckUtils(api: API, uiLog: Log, cardUtils: CardUtils) {

  private var _decks: List[Deck] = Nil

  def updateDecks() {
    try {
      _decks = api.get("decks/show") match {
        case Success(d) => d.data.as[List[Json]].map(fromJson)
        case _ => Nil
      }
      if (_decks.isEmpty) {
        uiLog.warn("no deck were returned from Hearthstats.net. Either you have not created a deck or the site is down")
      }
    } catch {
      case e: IOException => uiLog.warn("Error occurred while loading deck list from HearthStats.net", e)
    }
  }

  def getDeckFromSlot(slotNum: Int): Option[Deck] =
    getDecks.find(_.activeSlot == Some(slotNum))

  def getDecks: List[Deck] = {
    if (_decks == null) updateDecks()
    _decks
  }

  def getDeck(id: Int): Deck =
    getDecks.find(_.id == id) match {
      case Some(d) => d
      case None => throw new IllegalArgumentException("No deck found for id " + id)
    }

  def fromJson(data: Json): Deck = {
    val json""" {
    	"id" : $id,
    	"cardstring" : $cardstring,
    	"klass_id" : $klassId,
    	"slug" : $slug,
    	"name" : $name,
    	"slot" : $slot
    } """ = data
    val cardList: List[Card] =
      cardstring.as[Option[String]] match {
        case Some(cs) if StringUtils.isNotBlank(cs.toString) =>
          parseCardString(cs.toString.trim)
        case _ => Nil
      }

    val heroClass = klassId.as[Option[Int]].map(HeroClass.stringWithId).map(HeroClass.byName)

    Deck(id = id.as[Int],
      slug = slug.as[String],
      name = name.as[String],
      cards = cardList,
      hero = heroClass.getOrElse(HeroClass.UNDETECTED),
      activeSlot = slot.as[Option[Int]])
  }

  def parseCardString(ds: String): List[Card] = {
    val cardData = cardUtils.cards
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
    val cardMap = cardUtils.cards.values.map(c => c.name -> c).toMap
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
