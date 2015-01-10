package net.hearthstats.game.imageanalysis

import javax.imageio.ImageIO

import com.softwaremill.macwire.MacwireMacros._
import grizzled.slf4j.Logging
import net.hearthstats.config.{ UserConfig, TestEnvironment }
import net.hearthstats.hstatsapi.{ CardUtils, API }
import net.hearthstats.ui.log.Log

/**
 * Extracts a deck from a test image of the collection deck screen.
 */
object DeckAnalyserMain extends App with Logging {

  val environment = new TestEnvironment
  val uiLog = wire[Log]
  val config = wire[UserConfig]
  val api = wire[API]
  val cardUtils: CardUtils = wire[CardUtils]

  val img = ImageIO.read(getClass.getResourceAsStream("/images/deckexport/deck-01.png"))

  val cardList = cardUtils.cards.values.filter(c => c.collectible).toList
  val deckAnalyser = new DeckAnalyser(cardList, img.getWidth(), img.getHeight())

  val deck = deckAnalyser.identifyDeck(img, img)

  debug(deck.get.toJsonObject)

}
