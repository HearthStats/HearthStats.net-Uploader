package net.hearthstats.util

import java.io.File
import java.io.FileNotFoundException
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.Observable
import rx.subjects.PublishSubject
import grizzled.slf4j.Logging

case class FileObserver(file: File) extends Logging {
  import FileObserver._

  val DEFAULT_DELAY_MS = 500
  var stopped = false

  private val subject = PublishSubject.create[String]
  private val tailer = Tailer.create(file, new SubjectAdapter, DEFAULT_DELAY_MS, true)

  info("observing file " + file)

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
