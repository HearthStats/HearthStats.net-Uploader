package net.hearthstats.ui

import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.ProgramHelper
import net.hearthstats.config.{ TestEnvironment, UserConfig }
import net.hearthstats.hstatsapi.{ API, CardUtils, DeckUtils }
import net.hearthstats.ui.log.Log
import net.hearthstats.util.{ Translation, TranslationConfig }
import org.scalatest.mock.MockitoSugar
import net.hearthstats.companion.GameMonitor
import net.hearthstats.companion.ScreenEvents

/**
 * Simple test app that displays the Deck Export popup so that you can test the UI.
 */
object ExportDeckBoxMain extends App with MockitoSugar {

  val environment = new TestEnvironment

  val translationConfig = TranslationConfig("net.hearthstats.resources.Main", "en")
  val translation = wire[Translation]
  val uiLog = wire[Log]
  val config = wire[UserConfig]
  val api = wire[API]
  val cardUtils = wire[CardUtils]
  val deckUtils = wire[DeckUtils]

  val helper = mock[ProgramHelper]
  val companionEvents = mock[ScreenEvents]

  val exportDeckBox = wire[ExportDeckBox]

  exportDeckBox.open()

}
