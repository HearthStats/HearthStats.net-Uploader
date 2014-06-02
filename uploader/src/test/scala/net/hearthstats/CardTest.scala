package net.hearthstats

import java.net.URL
import org.junit.Test
import scala.collection.GenIterable

class CardTest {
  //@Test too slow for regular tests (a few minutes) 
  def checkAllCollectible() {
    val cards = CardUtils.cards.values.par.filter(_.collectible)
    checkURLs(cards)
  }

  @Test def checkOldBugs() {
    val oldBugs = Seq(
      "Al'Akir the Windlord",
      "Power Word: Shield",
      "Shadow Word: Pain",
      "Shadow Word: Death",
      "Pint-Sized Summoner",
      "SI:7 Agent",
      "Savage Roar")
    val cards = CardUtils.cards.values.filter(c => oldBugs contains c.name)
    checkURLs(cards)
  }

  private def checkURLs(cards: GenIterable[Card]) = {
    for (c <- cards) {
      println(s"checking download URL for $c : ${c.url}")
      new URL(c.url).openStream // throws an exception if bad URL
    }
  }
}