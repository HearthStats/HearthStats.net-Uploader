package net.hearthstats.util

import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import grizzled.slf4j.Logging
import akka.actor.ActorRef

case class FileObserver(file: File) extends ActorObservable with Logging { self =>
  import FileObserver._

  val DEFAULT_DELAY_MS = 50
  var stopped = false

  private var tailer: Option[Tailer] = None

  def start(): Unit = {
    tailer = Some(Tailer.create(file, new SubjectAdapter, DEFAULT_DELAY_MS, true, true))
    info(s"observing file $file")
  }

  def stop() = {
    tailer.map(_.stop())
    stopped = true
    info(s"stopped observing file $file")
  }

  private class SubjectAdapter extends TailerListenerAdapter with Logging {

    override def handle(line: String) =
      if (!stopped) {
        debug(s"$file: $line")
        self.notify(line)
      } else warn("should be stopped")

    override def handle(ex: Exception) =
      error(ex.getMessage, ex)

    override def fileNotFound() =
      handle(new FileNotFoundException(file.getAbsolutePath))

    override def fileRotated() =
      info(s"$file was rotated")
  }
}
