package net.hearthstats

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import net.hearthstats.util.{ MatchOutcome, Rank }
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class APISpec extends FlatSpec with Matchers {
  API.key = "a9efa89e4a7a806d428bdda944d7b48f" // a specific test key

  "The API" should "return some cards" in {
    val cards = CardUtils.cards
    cards.size should be > 400
  }

  "The API" should "create an Arena run" in {
    val arena = new ArenaRun
    arena.setUserClass("warlock")
    API.createArenaRun(arena).isDefined shouldBe true
    API.getLastArenaRun should not be null
    API.createMatch(new HearthstoneMatch("Arena",
      "warlock",
      "druid",
      false,
      Some(MatchOutcome.VICTORY),
      1,
      "unkownopp",
      Rank.RANK_1,
      1,
      1,
      ""))
    API.endCurrentArenaRun.isDefined shouldBe true
  }
}
