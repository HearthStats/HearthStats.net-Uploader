package net.hearthstats.analysis

import javax.imageio.ImageIO

import grizzled.slf4j.Logging
import net.hearthstats.{API, EnvironmentTest}

/**
 * Extracts a deck from a test image of the collection deck screen.
 */
object DeckAnalyserMain extends App with Logging {

  val environment = EnvironmentTest
  API.setConfig(environment.config)

  val img = ImageIO.read(getClass.getResourceAsStream("/images/deckexport/deck-01.png"))

  val deckAnalyser = new DeckAnalyser(img.getWidth(), img.getHeight())

  val deck = deckAnalyser.identifyDeck(img, img)

  debug(deck.get.toJsonObject)


}
