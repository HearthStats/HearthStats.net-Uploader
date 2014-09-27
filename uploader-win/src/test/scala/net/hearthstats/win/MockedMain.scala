package net.hearthstats.win

import net.hearthstats.config.TestEnvironment
import org.mockito.Mockito._
import net.hearthstats.ProgramHelper
import net.hearthstats.Main
import com.softwaremill.macwire.MacwireMacros._
import javax.imageio.ImageIO
import net.hearthstats.game.imageanalysis.InGameAnalyser
import net.hearthstats.game.imageanalysis.InGameAnalyser
import net.hearthstats.config.TestConfig
import grizzled.slf4j.Logging
import net.hearthstats.game.imageanalysis.HsClassAnalyser

object MockedMain extends TesseractSetup with App with Logging {
  val environment = TestEnvironment
  val config = TestConfig
  val helper = new MockProgramHelper(List(
    "play_lobby" -> 1,
    "finding" -> 10,
    "Druid_VS_Hunter" -> 1,
    "starting_hand_4_cards" -> 1,
    "orgrimmar_with_coin" -> 1,
    //    "victory_pandaria" -> 1))
    "defeat_stranglehorn" -> 1))
  val main = wire[Main]

  setupTesseract()

  main.start()

  class MockProgramHelper(var files: List[(String, Int)]) extends ProgramHelper {
    def foundProgram = true

    def getScreenCapture = {
      val (f, c) :: t = files
      files = if (c > 1 || t == Nil)
        (f, c - 1) :: t
      else t
      info(s"sending $f")
      img(f)
    }

    def getHSWindowBounds = null

  }

  def img(fileName: String) = ImageIO.read(classOf[InGameAnalyser].getResourceAsStream(fileName + ".png"))
}