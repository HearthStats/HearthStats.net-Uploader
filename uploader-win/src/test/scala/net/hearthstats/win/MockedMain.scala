package net.hearthstats.win

import net.hearthstats.config.TestEnvironment
import org.mockito.Mockito._
import net.hearthstats.ProgramHelper
import net.hearthstats.Main
import com.softwaremill.macwire.MacwireMacros._
import javax.imageio.ImageIO
import net.hearthstats.game.imageanalysis.InGameAnalyser
import net.hearthstats.game.imageanalysis.InGameAnalyser

object MockedMain extends TesseractSetup with App {
  val environment = TestEnvironment
  val helper = new MockProgramHelper
  val main = wire[Main]

  setupTesseract()

  main.start()

  class MockProgramHelper extends ProgramHelper {
    def foundProgram = true

    def getScreenCapture = img("play_lobby")

    def getHSWindowBounds = null

    def img(fileName: String) = ImageIO.read(classOf[InGameAnalyser].getResourceAsStream(fileName + ".png"))
  }
}