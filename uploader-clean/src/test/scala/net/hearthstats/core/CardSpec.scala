package net.hearthstats.core

import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.hstatsapi.CardUtils
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.Log
import net.hearthstats.config.TestConfig
import net.hearthstats.config.TestEnvironment
import net.hearthstats.hstatsapi.API
import java.net.URL
import net.hearthstats.config.UserConfig
import net.hearthstats.config.TestConfig

@RunWith(classOf[JUnitRunner])
class CardSpec extends FlatSpec with Matchers {

  //  "A Card" should "have a valid URL" in {
  //    lazy val userConfig: UserConfig = new TestConfig
  //    lazy val api = wire[API]
  //    lazy val uiLog = wire[Log]
  //    lazy val environment = TestEnvironment
  //    lazy val cardUtils = new CardUtils(api, uiLog, environment)
  //
  //    val oldBugs = Seq(
  //      "Al'Akir the Windlord",
  //      "Power Word: Shield",
  //      "Shadow Word: Pain",
  //      "Shadow Word: Death",
  //      "Pint-Sized Summoner",
  //      "SI:7 Agent",
  //      "Savage Roar")
  //    val cards = cardUtils.cards.values.filter(c => oldBugs contains c.originalName)
  //    for (c <- cards)
  //      // throws an exception if bad URL
  //      new URL(c.url).openStream should not be null
  //  }
}