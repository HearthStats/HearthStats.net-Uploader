package net.hearthstats.companion

import net.hearthstats.config.TestEnvironment
import org.mockito.Mockito._
import net.hearthstats.ProgramHelper
import net.hearthstats.Main
import com.softwaremill.macwire.MacwireMacros._
import javax.imageio.ImageIO
import net.hearthstats.game.imageanalysis.InGameAnalyser
import java.lang.Boolean

object MockedMain extends App {
  val env = TestEnvironment
  val programHelper = new MockProgramHelper
  val main = wire[Main]

  main.start()

  class MockProgramHelper extends ProgramHelper {
    def foundProgram = true

    def getScreenCapture = img("play_lobby")

    def getHSWindowBounds = null

    def img(fileName: String) = ImageIO.read(classOf[InGameAnalyser].getResourceAsStream(fileName + ".png"))
  }
}