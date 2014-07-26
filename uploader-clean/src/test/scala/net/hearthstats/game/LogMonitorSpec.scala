package net.hearthstats.game

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import net.hearthstats.config.{ TestConfig, TestEnvironment }
import net.hearthstats.core.{ ArenaRun, HearthstoneMatch, MatchOutcome, Rank }
import net.hearthstats.ui.Log
import net.hearthstats.config.UserConfig
import org.scalatest.mock.MockitoSugar
import net.hearthstats.hstatsapi.API
import net.hearthstats.hstatsapi.CardUtils
import org.scalatest.junit.JUnitRunner
import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.util.FileObserver
import scala.collection.mutable.ListBuffer
import org.mockito.Mockito._
import rx.subjects.PublishSubject
import rx.lang.scala.JavaConversions._
import grizzled.slf4j.Logging

@RunWith(classOf[JUnitRunner])
class LogMonitorSpec extends FlatSpec with Matchers with MockitoSugar with Logging {
  val logMonitorModule = new LogMonitorModule {
    lazy val config: UserConfig = TestConfig
    lazy val environment = TestEnvironment
    lazy val uiLog = mock[Log]

    lazy val api = wire[API]
    lazy val cardUtils = wire[CardUtils]
    override lazy val fileObserver = mock[FileObserver]

    val fileLines = PublishSubject.create[String]
    when(fileObserver.observable).thenReturn(fileLines.asObservable)
  }

  import logMonitorModule._

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
