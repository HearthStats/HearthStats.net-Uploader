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
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.subjects.PublishSubject
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LogMonitorSpec extends FlatSpec with Matchers with MockitoSugar with Logging {
  val environment = TestEnvironment
  val uiLog = mock[Log]
  val fileObserver = mock[FileObserver]
  val fileLines = PublishSubject.create[String]
  when(fileObserver.observable).thenReturn(fileLines.asObservable)

  val logParser = wire[LogParser]
  val hsLogMonitor = wire[HearthstoneLogMonitor]

  "The LogMonitor" should "detect when you won the game" in {
    var receivedEvent: GameEvent = null
    hsLogMonitor.gameEvents.subscribe(evt => receivedEvent = evt)

    fileLines.onNext(gameWonLog)

    receivedEvent shouldBe HeroDestroyedEvent(opponent = true)
  }

  "The LogMonitor" should "detect when you lost the game" in {
    var receivedEvent: GameEvent = null
    hsLogMonitor.gameEvents.subscribe(evt => receivedEvent = evt)

    fileLines.onNext(gameLostLog)

    receivedEvent shouldBe HeroDestroyedEvent(opponent = false)
  }

  val gameLostLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from FRIENDLY PLAY (Hero) -> FRIENDLY GRAVEYARD"""
  val gameWonLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from OPPOSING PLAY (Hero) -> OPPOSING GRAVEYARD"""

}
