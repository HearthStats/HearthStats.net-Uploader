package net.hearthstats.game

import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.mock.MockitoSugar
import com.softwaremill.macwire.MacwireMacros.wire
import grizzled.slf4j.Logging
import net.hearthstats.config.TestEnvironment
import net.hearthstats.ui.log.Log
import net.hearthstats.util.FileObserver
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration.DurationInt
import org.scalatest.OneInstancePerTest
import scala.concurrent.Future
import java.io.File

@RunWith(classOf[JUnitRunner])
class LogMonitorSpec extends FlatSpec with Matchers with MockitoSugar with OneInstancePerTest with Logging {
  val environment = TestEnvironment
  val uiLog = mock[Log]
  val tempFile = File.createTempFile("unused", "unused")
  val fileObserver = wire[FileObserver]
  val logParser = wire[LogParser]
  val hsLogMonitor: HearthstoneLogMonitor = wire[HearthstoneLogMonitor]

  it should "detect when you won the game" in {
    var receivedEvent: GameEvent = null
    hsLogMonitor.start(tempFile)
    hsLogMonitor.addReceive { case evt: GameEvent => receivedEvent = evt }
    fileObserver.notify(gameWonLog)
    Thread.sleep(50)
    receivedEvent shouldBe HeroDestroyedEvent(opponent = true)
  }

  it should "detect when you lost the game" in {
    var receivedEvent: GameEvent = null
    hsLogMonitor.start(tempFile)
    hsLogMonitor.addReceive { case evt: GameEvent => receivedEvent = evt }
    fileObserver.notify(gameLostLog)
    Thread.sleep(50)
    receivedEvent shouldBe HeroDestroyedEvent(opponent = false)
  }

  val gameLostLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from FRIENDLY PLAY (Hero) -> FRIENDLY GRAVEYARD"""
  val gameWonLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from OPPOSING PLAY (Hero) -> OPPOSING GRAVEYARD"""

}
