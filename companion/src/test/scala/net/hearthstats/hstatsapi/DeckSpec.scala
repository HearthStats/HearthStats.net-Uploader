package net.hearthstats.hstatsapi

import org.scalatest._
import org.junit.runner.RunWith
import net.hearthstats.core.Deck
import org.scalatest.junit.JUnitRunner
import net.hearthstats.config.TestConfig
import net.hearthstats.config.UserConfig
import net.hearthstats.config.TestEnvironment
import net.hearthstats.ui.log.Log
import org.scalatest.mock.MockitoSugar
import com.softwaremill.macwire.MacwireMacros.wire

@RunWith(classOf[JUnitRunner])
class DeckSpec extends FlatSpec with Matchers with MockitoSugar {
  val config: UserConfig = TestConfig
  val uiLog = mock[Log]
  val environment = new TestEnvironment

  val api = wire[API]
  val cardUtils = wire[CardUtils]
  val deckUtils = wire[DeckUtils]

  "A Deck with 30 cards" should "be a valid deck" in {
    Deck(cards = deckUtils.parseCardString("86_2,297_2,175_2,135_2,212_2,272_2,253_2,281_1,160_2,87_1,78_2,24_2,257_2,349_1,361_1,53_2,218_2"))
      .isValid shouldBe true
    Deck(cards = deckUtils.parseCardString("4_1,18_2,30_2,40_2,45_2,55_1,58_2,92_2,127_2,211_2,241_1,258_1,289_2,303_1,308_2,362_2,374_2,378_1"))
      .isValid shouldBe true
  }

  "A Deck with less than 30 cards" should "not be a valid deck" in {
    Deck(cards = Nil).isValid shouldBe false
    Deck(cards = deckUtils.parseCardString("4_1,18_2")).isValid shouldBe false
  }
}