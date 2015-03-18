package net.hearthstats.hstatsapi

import org.scalatest._
import org.junit.runner.RunWith
import com.softwaremill.macwire.MacwireMacros._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ Finders, FlatSpec, Matchers }
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.config.TestEnvironment
import net.hearthstats.config.UserConfig
import net.hearthstats.config.TestConfig
import net.hearthstats.ui.log.Log
import java.net.URL
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class CardSpec extends FlatSpec with Matchers with MockitoSugar {
  val config: UserConfig = TestConfig
  val uiLog = mock[Log]
  val environment = new TestEnvironment

  val api = wire[API]
  val cardUtils = wire[CardUtils]

  "A Card" should "have a valid URL" in {

    val oldBugs = Seq(
      "Al'Akir the Windlord",
      "Power Word: Shield",
      "Shadow Word: Pain",
      "Shadow Word: Death",
      "Pint-Sized Summoner",
      "SI:7 Agent",
      "Savage Roar")
    val cards = cardUtils.cards.values.filter(c => oldBugs contains c.originalName)
    for (c <- cards)
      // throws an exception if bad URL
      new URL(c.url).openStream should not be null
  }
}