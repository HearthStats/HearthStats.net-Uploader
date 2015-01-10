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
import net.hearthstats.core.Deck
import net.hearthstats.core.GameLog

@RunWith(classOf[JUnitRunner])
class APISpec extends FlatSpec with Matchers with MockitoSugar {
  val config: UserConfig = TestConfig
  val uiLog = mock[Log]
  val environment = TestEnvironment

  val api = wire[API]
  val cardUtils = wire[CardUtils]

  it should "return some cards" in {
    val cards = cardUtils.cards
    cards.size should be > 400
  }

  it should "create a casual match" in {
    val matc = HearthstoneMatch(GameMode.CASUAL,
      WARLOCK,
      DRUID,
      Some(false),
      Some(MatchOutcome.VICTORY),
      Some(Deck(id = 1, activeSlot = Some(1))),
      "unkownopp",
      None,
      1,
      1,
      "")
    api.createMatch(matc.withJsonLog(GameLog())).isSuccess shouldBe true
  }

  it should "create a ranked match" in {
    val matc = HearthstoneMatch(GameMode.RANKED,
      WARLOCK,
      DRUID,
      Some(true),
      Some(MatchOutcome.DEFEAT),
      Some(Deck(id = 1, activeSlot = Some(1))),
      "unkownopp",
      Some(Rank.RANK_11),
      1,
      1)
    api.createMatch(matc.withJsonLog(GameLog.sample)).isSuccess shouldBe true
  }

  it should "create an Arena run" in {
    val arena = ArenaRun("warlock")
    api.createArenaRun(arena).isSuccess shouldBe true
    api.getLastArenaRun should not be null
    val m = HearthstoneMatch(GameMode.ARENA,
      WARLOCK,
      DRUID,
      Some(false),
      Some(MatchOutcome.VICTORY),
      Some(Deck(id = 1, activeSlot = Some(1))),
      "unkownopp",
      Some(Rank.RANK_1),
      1,
      1,
      "")
    api.createMatch(m.withJsonLog(GameLog())).isSuccess shouldBe true
    api.endCurrentArenaRun.isSuccess shouldBe true
  }
}
