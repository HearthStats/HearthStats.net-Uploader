package net.hearthstats.game

import java.io.File
import com.softwaremill.macwire.MacwireMacros.wire
import grizzled.slf4j.Logging
import net.hearthstats.config.{ Environment, UserConfig }
import net.hearthstats.hstatsapi.{ API, CardUtils }
import net.hearthstats.ui.log.Log
import net.hearthstats.util.FileObserver
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import net.hearthstats.util.ActorObservable
import akka.actor.ActorDSL._
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class HearthstoneLogMonitor(
  logParser: LogParser,
  val fileObserver: FileObserver) extends ActorObservable with Logging { observable =>

  fileObserver.addObserver(actor(new Act {
    val normalBehaviour: Receive =
      {
        case l: String if l != null && l.length > 0 && l.charAt(0) == '[' =>
          debug(s"found : [$l]")
          zoneEvent(l) match {
            case Some(StartupEvent) =>
              context.become(ignoringEvents, false)
              system.scheduler.scheduleOnce(2000.milliseconds, self, "resume")
              info(s"Ignoring all logs for 2 seconds")
            case Some(e) =>
              debug(s"game event: $e")
              observable.notify(e)
            case None =>
          }
      }

    val ignoringEvents: Receive = {
      case "resume" =>
        unbecome()
        info("Resume monitoring log file")
      case e =>
        debug(s"Ignoring event $e")
      // ignore all events for a short while to avoid processing everything from the log file when HS exits
      // the numbers might need a little more tweaking (check also the default delay in FileObserver)
    }
    become(normalBehaviour)
  }))

  def stop(): Unit =
    fileObserver.stop()

  def zoneEvent(line: String): Option[GameEvent] =
    logParser.analyseLine(line)

}