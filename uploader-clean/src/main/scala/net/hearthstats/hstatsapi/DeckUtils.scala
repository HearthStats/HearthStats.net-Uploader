package net.hearthstats

import java.io.IOException
import scala.collection.JavaConversions.{ asScalaBuffer, bufferAsJavaList, seqAsJavaList }
import org.json.simple.JSONObject
import net.hearthstats.core.Deck
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.Log
import org.apache.commons.lang3.StringUtils
import net.hearthstats.core.HeroClasses
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.core.Card

class DeckUtils(api: API, uiLog: Log, cardUtils: CardUtils) {

  private var _decks: List[JSONObject] = _

  def updateDecks() {
    try {
      _decks = api.getDecks
    } catch {
      case e: IOException => uiLog.warn("Error occurred while loading deck list from HearthStats.net", e)
    }
  }

  def getDeckFromSlot(slotNum: java.lang.Integer): Option[Deck] = {
    getDecks
    for (
      i <- 0 until _decks.size if _decks.get(i).get("slot") != null &&
        _decks.get(i).get("slot").toString == slotNum.toString
    ) return Some(fromJson(_decks.get(i)))
    None
  }

  def getDecks: List[JSONObject] = {
    if (_decks == null) updateDecks()
    _decks
  }

  def getDeckLists: List[Deck] =
    for (deck <- getDecks)
      yield fromJson(deck)

  def getDeck(id: Int): Deck =
    getDeckLists.find(_.id == id) match {
      case Some(d) => d
      case None => throw new IllegalArgumentException("No deck found for id " + id)
    }

  def fromJson(json: JSONObject): Deck = {
    val id = Integer.parseInt(json.get("id").toString)
    val cardList: List[Card] =
      Option(json.get("cardstring")) match {
        case Some(cs) if StringUtils.isNotBlank(cs.toString) =>
          parseDeckString(cs.toString.trim)
        case _ => Nil
      }

    val klassId = json.get("klass_id")
    val heroString = if (klassId == null) "" else HeroClasses.all(klassId.toString.toInt)

    Deck(id = id,
      slug = json.get("slug").toString,
      name = json.get("name").toString,
      cards = cardList,
      hero = heroString,
      activeSlot = Option(json.get("slot")).map(_.toString.toInt))
  }

  def parseDeckString(ds: String): List[Card] = {
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
}
