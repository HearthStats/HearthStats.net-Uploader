package net.hearthstats.hstatsapi

import org.mockito.Mockito
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.config.TestConfig
import net.hearthstats.config.TestEnvironment
import net.hearthstats.config.UserConfig
import net.hearthstats.ui.log.Log
import net.hearthstats.core.Deck
import net.hearthstats.core.CardData
import math._

object DeckMain extends App {
  val uiLog = Mockito.mock(classOf[Log])
  val config = new UserConfig
  val environment = new TestEnvironment {
    override val config = new UserConfig
  }

  val api = wire[API]
  val cardUtils = wire[CardUtils]
  val deckUtils: DeckUtils = wire[DeckUtils]

  val decks = for {
    deck <- deckUtils.getDecks if deck.isValid
    cards = deck.cards
    totalCost = score(deck) //cards.filter(_.).map(c => c.count * score(c.cost)).sum
  } yield deck -> totalCost

  decks.sortBy(_._2).foreach(println)

  def score(d: Deck) = {
    val minions =
      for (minion <- d.cards if minion.typeId == 1)
        yield minion
    minions.map(m => min(m.cost, 8) * m.count).sum / minions.map(_.count).sum.toFloat
  }

  def score(cost: Int) =
    if (cost < 3) 1
    else if (cost < 6) 2
    else 3
}