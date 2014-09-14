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

object MockedMain extends TesseractSetup with App {
  val environment = TestEnvironment
  val config = TestConfig
  val helper = new MockProgramHelper(List(
    "play_lobby" -> 2,
    "finding" -> 5,
    "Druid_VS_Hunter" -> 2,
    "starting_hand_4_cards" -> 2,
    "orgrimmar_with_coin" -> 2))
  val main = wire[Main]

  setupTesseract()

  main.start()

  class MockProgramHelper(var files: List[(String, Int)]) extends ProgramHelper {
    def foundProgram = true

    def getScreenCapture = {
      files match {
        case (f, c) :: t =>
          if (c > 0 || t == Nil) {
            files = (f, c - 1) :: t
          } else {
            files = t
          }
          img(f)
      }
    }

    def getHSWindowBounds = null

    def img(fileName: String) = ImageIO.read(classOf[InGameAnalyser].getResourceAsStream(fileName + ".png"))
  }
}