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
  fileObserver: FileObserver) extends ActorObservable with Logging { observable =>

  fileObserver.addObserver(actor(new Act {
    become {
      case l: String if l != null && l.length > 0 && l.charAt(0) == '[' =>
        debug(s"found : [$l]")
        zoneEvent(l) match {
          case Some(e) =>
            debug(s"game event: $e")
            observable.notify(e)
          case None =>
        }
    }
  }))

  def start(): Unit =
    fileObserver.start()

  def stop(): Unit =
    fileObserver.stop()

  def zoneEvent(line: String): Option[GameEvent] =
    logParser.analyseLine(line)

}