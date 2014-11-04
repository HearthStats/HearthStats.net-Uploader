package net.hearthstats.game

import rx.lang.scala.Observable
import rx.lang.scala.subscriptions._
import rx.subjects.PublishSubject
import rx.lang.scala.JavaConversions._
import net.hearthstats.util.FileObserver
import java.io.File
import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.hstatsapi.API
import net.hearthstats.config.UserConfig
import net.hearthstats.config.Environment
import net.hearthstats.util.FileObserver
import net.hearthstats.ui.log.Log
import grizzled.slf4j.Logging
import net.hearthstats.hstatsapi.CardUtils
import CardEvents._

trait LogMonitorModule {
  val config: UserConfig
  val api: API
  val cardUtils: CardUtils
  val environment: Environment
  val uiLog: Log
  lazy val fileObserver = FileObserver(new File(environment.hearthstoneLogFile))

  val logParser=wire[LogParser]
  lazy val hsLogMonitor = wire[HearthstoneLogMonitor]
}

class HearthstoneLogMonitor(
  config: UserConfig,
  api: API,
  cardUtils: CardUtils,
  environment: Environment,
  uiLog: Log,
  logParser:LogParser,
  fileObserver: FileObserver) extends GameEventProducer with Logging {

  import config._

  val lines = fileObserver.observable.
    doOnNext(line => debug(s"found : [$line]")).
    doOnError(ex => uiLog.error("Error reading Hearthstone log: " + ex.getMessage, ex))
  val relevant = lines.filter(l => l != null && l.length > 0 && l.charAt(0) == '[')

  val gameEvents: Observable[GameEvent] = relevant.map(zoneEvent).filter(_.isDefined).map(_.get). // remove None values
    doOnNext(evt => debug(s"game event: $evt"))
  val cardEvents: Observable[CardEvent] = gameEvents.ofType(classOf[CardEvent])
  val heroEvents: Observable[HeroEvent] = gameEvents.ofType(classOf[HeroEvent])

  def stop(): Unit = {
    fileObserver.stop()
  }

  def zoneEvent(line: String): Option[GameEvent] = {
    val ge=logParser.analyseLine(line)
    ge.map(event => uiLog.info(event.toString))
    ge
  }

 
}