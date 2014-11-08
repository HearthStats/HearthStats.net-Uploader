package net.hearthstats.game

import java.io.File
import com.softwaremill.macwire.MacwireMacros.wire
import grizzled.slf4j.Logging
import net.hearthstats.config.{ Environment, UserConfig }
import net.hearthstats.hstatsapi.{ API, CardUtils }
import net.hearthstats.ui.log.Log
import net.hearthstats.util.FileObserver
import rx.lang.scala.JavaConversions.{ toJavaObservable, toScalaObservable }
import rx.lang.scala.Observable
import net.hearthstats.util.ObservableExtensions._

class HearthstoneLogMonitor(
  uiLog: Log,
  logParser: LogParser,
  fileObserver: FileObserver) extends GameEventProducer with Logging {

  val lines = fileObserver.observable.
    doOnNext(line => debug(s"found : [$line]")).
    doOnError(ex => uiLog.error("Error reading Hearthstone log: " + ex.getMessage, ex))
  val relevant = lines.filter(l => l != null && l.length > 0 && l.charAt(0) == '[')

  val gameEvents: Observable[GameEvent] = relevant.map(zoneEvent).filter(_.isDefined).map(_.get). // remove None values
    doOnNext(evt => debug(s"game event: $evt"))
  val cardEvents: Observable[CardEvent] = gameEvents.ofType(classOf[CardEvent])
  val heroEvents: Observable[HeroEvent] = gameEvents.ofType(classOf[HeroEvent])

  val gameEndEvents: Observable[GameOver] = gameEvents.ofType(classOf[GameOver])

  val games: Observable[Observable[GameEvent]] = gameEvents.span(_.isInstanceOf[GameOver])

  def turns(game: Observable[GameEvent]): Observable[Observable[GameEvent]] =
    game.span(_ == TurnPassedEvent)

  def stop(): Unit = {
    fileObserver.stop()
  }

  def zoneEvent(line: String): Option[GameEvent] = {
    val ge = logParser.analyseLine(line)
    ge.map(event => uiLog.info(event.toString))
    ge
  }

}