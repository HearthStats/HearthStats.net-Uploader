package net.hearthstats

import java.io.IOException
import java.util.List

import scala.collection.JavaConversions.{asScalaBuffer, bufferAsJavaList, seqAsJavaList}

import org.json.simple.JSONObject

import net.hearthstats.core.Deck
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.Log

class DeckUtils(api: API, uiLog: Log) {

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
    ) return Some(Deck.fromJson(_decks.get(i)))
    None
  }

  def getDecks: List[JSONObject] = {
    if (_decks == null) updateDecks()
    _decks
  }

  def getDeckLists: List[Deck] =
    for (deck <- getDecks)
      yield Deck.fromJson(deck)

  def getDeck(id: Int): Deck =
    getDeckLists.find(_.id == id) match {
      case Some(d) => d
      case None => throw new IllegalArgumentException("No deck found for id " + id)
    }
}
