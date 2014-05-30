package net.hearthstats.util

import java.io.File
import java.io.FileNotFoundException

import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter

import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.Observable
import rx.subjects.PublishSubject

case class FileObserver(file: File) {
  import FileObserver._

  val DEFAULT_DELAY_MS = 500

  private val subject = PublishSubject.create[String]
  private val tailer = Tailer.create(file, new SubjectAdapter(subject), DEFAULT_DELAY_MS, true)

  def observable: Observable[String] = subject.asObservable
  def stop() = tailer.stop()
}

private class SubjectAdapter(subject: PublishSubject[String]) extends TailerListenerAdapter {

  override def handle(line: String) =
    subject.onNext(line)

  override def handle(ex: Exception) =
    subject.onError(ex)

  override def fileNotFound() =
    subject.onError(new FileNotFoundException)
}

