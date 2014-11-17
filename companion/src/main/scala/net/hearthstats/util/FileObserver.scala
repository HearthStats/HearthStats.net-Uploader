package net.hearthstats.util

import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import grizzled.slf4j.Logging
import akka.actor.ActorRef
import java.nio.charset.Charset

case class FileObserver(file: File) extends ActorObservable with Logging { self =>
  import FileObserver._

  val DEFAULT_DELAY_MS = 50
  var stopped = true

  private var tailer: Option[Tailer] = None

  def start(): Unit = {
    tailer = Some(Tailer.create(file, new SubjectAdapter, DEFAULT_DELAY_MS, true, true))
    info(s"observing file $file")
    stopped = false
  }

  def stop() = {
    tailer.map(_.stop())
    stopped = true
    info(s"stopped observing file $file")
  }

  private class SubjectAdapter extends TailerListenerAdapter with Logging {

    override def handle(line: String) =
      if (!stopped) {
        val l = hackToUtf8(line)
        debug(s"$file: $l")
        self.notify(l)
      } else warn("should be stopped")

    // Tailer does not handle UTF-8, see https://issues.apache.org/jira/browse/IO-354
    private def hackToUtf8(line: String): String = {
      val len = line.length
      val bytes = (0 until len).map(line.charAt(_).toByte)
      new String(bytes.toArray, "UTF8")
    }

    override def handle(ex: Exception) =
      error(ex.getMessage, ex)

    override def fileNotFound() =
      handle(new FileNotFoundException(file.getAbsolutePath))

    override def fileRotated() =
      info(s"$file was rotated")
  }
}
