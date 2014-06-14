package net.hearthstats

import java.net.URL
import org.junit.Test
import scala.collection.GenIterable
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CardSpec extends FlatSpec with Matchers {

  "A Card" should "have a valid URL" in {
    val oldBugs = Seq(
      "Al'Akir the Windlord",
      "Power Word: Shield",
      "Shadow Word: Pain",
      "Shadow Word: Death",
      "Pint-Sized Summoner",
      "SI:7 Agent",
      "Savage Roar")
    val cards = CardUtils.cards.values.filter(c => oldBugs contains c.name)
    for (c <- cards)
      // throws an exception if bad URL
      new URL(c.url).openStream should not be null
  }
}