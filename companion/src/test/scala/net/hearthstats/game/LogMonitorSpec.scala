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
import scala.concurrent.duration.DurationInt
import rx.lang.scala.Observable

@RunWith(classOf[JUnitRunner])
class LogMonitorSpec extends FlatSpec with Matchers with MockitoSugar with Logging {
  val environment = TestEnvironment
  val uiLog = mock[Log]
  val fileObserver = mock[FileObserver]
  val fileLines = PublishSubject.create[String]
  when(fileObserver.observable).thenReturn(fileLines.asObservable)

  val logParser = wire[LogParser]
  val hsLogMonitor = wire[HearthstoneLogMonitor]

  it should "detect when you won the game" in {
    var receivedEvent: GameEvent = null
    hsLogMonitor.gameEvents.subscribe(evt => receivedEvent = evt)

    fileLines.onNext(gameWonLog)

    receivedEvent shouldBe HeroDestroyedEvent(opponent = true)
  }

  it should "detect when you lost the game" in {
    var receivedEvent: GameEvent = null
    hsLogMonitor.gameEvents.subscribe(evt => receivedEvent = evt)

    fileLines.onNext(gameLostLog)

    receivedEvent shouldBe HeroDestroyedEvent(opponent = false)
  }

  it should "detect several games" in {
    var count = 0
    for (game <- hsLogMonitor.games) {
      count += 1
    }
    sendLogs("several_games_log.txt")
    waitFor(hsLogMonitor.games)
    count shouldBe 4 // 3 GameOver should be detected => 4 possible games
  }

  val gameLostLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from FRIENDLY PLAY (Hero) -> FRIENDLY GRAVEYARD"""
  val gameWonLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from OPPOSING PLAY (Hero) -> OPPOSING GRAVEYARD"""

  def sendLogs(file: String): Unit = {
    val lines = io.Source.fromInputStream(getClass.getResourceAsStream(file)).getLines
    for (l <- lines) {
      fileLines.onNext(l)
    }
    fileLines.onCompleted()
  }

  /**
   * Subscribes to obs and waits until obs has completed. Note that if you subscribe to
   *  obs yourself and also call waitFor(obs), all side-effects of subscribing to obs
   *  will happen twice.
   */
  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlocking.toIterable.last
  }
}
