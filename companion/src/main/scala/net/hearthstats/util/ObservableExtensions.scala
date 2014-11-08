package net.hearthstats.util

import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.Observable
import rx.lang.scala.Subject
import scala.collection.mutable.ListBuffer

object ObservableExtensions extends App {
  implicit class ObservableExtended[T](obs: Observable[T]) {
    def span(ends: T => Boolean): Observable[Observable[T]] = {
      val groups = PublishSubject.apply[Observable[T]]()
      var current: Subject[T] = null
      val cache = ListBuffer.empty[T]

      def newCurrent(): Unit = {
        if (current == null) {
          current = PublishSubject.apply[T]()
          groups.onNext(current)
        }
      }

      def next(t: T) = {
        newCurrent()
        cache += t
        if (ends(t)) {
          flush()
        }
      }

      def flush(): Unit = {
        for (i <- cache) {
          current.onNext(i)
        }
        current.onCompleted()
        current = null
        cache.clear()
      }

      def completed() = {
        newCurrent()
        flush()
        groups.onCompleted()
      }

      obs.subscribe(next _, groups.onError _, completed)
      groups
    }
  }

}