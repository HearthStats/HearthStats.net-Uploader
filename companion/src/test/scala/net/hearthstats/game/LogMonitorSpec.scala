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
import org.scalatest.OneInstancePerTest
import rx.lang.scala.schedulers.NewThreadScheduler
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class LogMonitorSpec extends FlatSpec with Matchers with MockitoSugar with OneInstancePerTest with Logging {
  val environment = TestEnvironment
  val uiLog = mock[Log]
  val fileObserver = mock[FileObserver]
  val fileLines = PublishSubject.create[String]
  when(fileObserver.observable).thenReturn(fileLines.asObservable)

  val logParser = wire[LogParser]
  val hsLogMonitor: HearthstoneLogMonitor = wire[HearthstoneLogMonitor]

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

  //TODO: this is not reliable enough yet, sometimes a subscription is missed (probably depends on the load)

  ignore should "detect several games" in {
    val games = hsLogMonitor.games
    sendLogs("several_games_log.txt")
    games.toList.toBlocking.single.size shouldBe 4
  }

  ignore should "detect several turns in a game" in {
    val turns = for {
      game1 <- hsLogMonitor.games.tail.head
      turn <- hsLogMonitor.turns(game1)
    } yield turn
    //    debugGames()
    debugTurns(turns)
    sendLogs("several_games_log.txt")
    turns.toList.toBlocking.single.size shouldBe 35
  }

  def debugTurns(turns: Observable[Observable[GameEvent]]) = {
    for ((turn, t) <- turns.zipWithIndex) {
      println(s"turn#$t")
      for ((evt, e) <- turn.zipWithIndex) println(s"evt#$e : $evt")
    }
  }

  def debugGames(): Unit = {
    for ((game, i) <- hsLogMonitor.games.zipWithIndex) {
      println(s"game#$i")
      for ((turn, t) <- hsLogMonitor.turns(game).zipWithIndex) {
        println(s"turn#$t")
        for ((evt, e) <- turn.zipWithIndex) println(s"evt#$e : $evt")
      }
    }

  }

  val gameLostLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from FRIENDLY PLAY (Hero) -> FRIENDLY GRAVEYARD"""
  val gameWonLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from OPPOSING PLAY (Hero) -> OPPOSING GRAVEYARD"""

  import scala.concurrent.ExecutionContext.Implicits.global

  def sendLogs(file: String): Unit = Future {
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
