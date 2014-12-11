package net.hearthstats.game

import java.io.File

import akka.actor.ActorDSL.{ Act, actor }
import grizzled.slf4j.Logging
import net.hearthstats.util.{ ActorObservable, FileObserver }

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

  def start(file: File): Boolean =
    fileObserver.start(file)

  def stop(): Unit =
    fileObserver.stop()

  def zoneEvent(line: String): Option[GameEvent] =
    logParser.analyseLine(line)

}