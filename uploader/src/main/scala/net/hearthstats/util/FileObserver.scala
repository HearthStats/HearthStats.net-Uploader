package net.hearthstats.util

import java.io.File
import java.io.FileNotFoundException
import net.hearthstats.logmonitor.Tailer
import net.hearthstats.logmonitor.TailerListener
import net.hearthstats.logmonitor.TailerListenerAdapter
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.Observable
import rx.subjects.PublishSubject
import grizzled.slf4j.Logging
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

case class FileObserver(file: File) {
  import FileObserver._

  val DEFAULT_DELAY_MS = 500
  var stopped = false

  private val subject = PublishSubject.create[String]
  private val tailer = Tailer.create(file, StandardCharsets.UTF_8, new SubjectAdapter, DEFAULT_DELAY_MS, true, false, 4096)

  def observable: Observable[String] = subject.asObservable.cache
  def stop() = {
    tailer.stop()
    stopped = true
  }

  private class SubjectAdapter extends TailerListenerAdapter with Logging {

    override def handle(line: String) =
      if (!stopped) {
        debug(s"$file: $line")
        subject.onNext(line)
      } else warn("should be stopped")

    override def handle(ex: Exception) =
      subject.onError(ex)

    override def fileNotFound() =
      subject.onError(new FileNotFoundException)
  }
}
