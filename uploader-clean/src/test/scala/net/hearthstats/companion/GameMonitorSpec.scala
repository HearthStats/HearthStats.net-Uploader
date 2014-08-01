package net.hearthstats.companion

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
import net.hearthstats.ProgramHelper
import org.mockito.Mockito._
import javax.imageio.ImageIO
import net.hearthstats.game.imageanalysis.AnalyserSpec
import net.hearthstats.core.GameMode._

@RunWith(classOf[JUnitRunner])
class GameMonitorSpec extends FlatSpec with Matchers with MockitoSugar {
  val config: UserConfig = TestConfig
  val state = new CompanionState
  val helper = mock[ProgramHelper]
  val imageToEvent=wire[ImageToEvent]

  val monitor = wire[GameMonitor]

  val rank8Lobby = readImage("play_lobby")

  "The monitor" should "detect mode ranked" in {
    when(helper.foundProgram).thenReturn(true)
    when(helper.getScreenCapture).thenReturn(rank8Lobby)
    Thread.sleep(config.pollingDelayMs.get)
    state.mode shouldBe Some(RANKED)
  }

  def readImage(name: String) =
    ImageIO.read(classOf[AnalyserSpec].getResourceAsStream(name + ".png"))
}