package net.hearthstats

import java.net.URL
import org.junit.Test
import scala.collection.GenIterable
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import net.hearthstats.util.Rank
import net.hearthstats.util.MatchOutcome._

@RunWith(classOf[JUnitRunner])
class HearthstoneMatchSpec extends FlatSpec with Matchers {

  "A valid ranked match" should "be detected as valid" in {
    val m = new HearthstoneMatch
    m.mode = "Ranked"
    m.result = Some(VICTORY)
    m.userClass = "Warlock"
    m.opponentClass = "Druid"
    m.opponentName = "toto"
    m.rankLevel = Rank.fromInt(15)
    m.deckSlot = 3

    m.isDataComplete shouldBe true
  }

  "A match with no mode" should "be detected as invalid" in {
    val m = new HearthstoneMatch
    m.result = Some(VICTORY)
    m.userClass = "Warlock"
    m.opponentClass = "Druid"
    m.opponentName = "toto"
    m.rankLevel = Rank.fromInt(15)
    m.deckSlot = 3

    m.isDataComplete shouldBe false
  }
}