package net.hearthstats.hstatsapi

import org.junit.runner.RunWith
import org.scalatest.{ Finders, FlatSpec, Matchers }
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.config.{ TestConfig, TestEnvironment }
import net.hearthstats.core.{ ArenaRun, HearthstoneMatch, MatchOutcome, Rank }
import net.hearthstats.ui.log.Log
import org.scalatest.junit.JUnitRunner
import net.hearthstats.config.UserConfig
import org.scalatest.mock.MockitoSugar
import net.hearthstats.core.HeroClass._
import net.hearthstats.core.GameMode

@RunWith(classOf[JUnitRunner])
class APISpec extends FlatSpec with Matchers with MockitoSugar {
  val config: UserConfig = TestConfig
  val uiLog = mock[Log]
  val environment = TestEnvironment

  val api = wire[API]
  val cardUtils = wire[CardUtils]

  "The API" should "return some cards" in {
    val cards = cardUtils.cards
    cards.size should be > 400
  }

  //TODO :move exception handling out of API class so it can be tested here.

  "The API" should "create an Arena run" in {
    val arena = new ArenaRun
    arena.setUserClass("warlock")
    api.createArenaRun(arena).isDefined shouldBe true
    api.getLastArenaRun should not be null
    api.createMatch(new HearthstoneMatch(GameMode.ARENA,
      WARLOCK,
      DRUID,
      Some(false),
      Some(MatchOutcome.VICTORY),
      1,
      "unkownopp",
      Rank.RANK_1,
      1,
      1,
      "")).isDefined shouldBe true
    api.endCurrentArenaRun.isDefined shouldBe true
  }
}
