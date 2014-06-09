package net.hearthstats

import rx.lang.scala._
import rx.lang.scala.schedulers._
import scala.concurrent.duration._
import rx.subjects.PublishSubject
import rx.lang.scala.JavaConversions._

object RxScalaMain extends App {
  val subject = PublishSubject.create[String]

  val obs: Observable[String] = subject.asObservable
  obs.subscribe(m => println(m))

  val transf = obs.cache.map { e =>
    println(s"transforming $e")
    e.length
  }

  transf.subscribe(i => ()).unsubscribe
  transf.subscribe(i => ())

  subject.onNext("hello")
  subject.onNext("world")

  val o = Observable.interval(200 millis).take(5)
  o.subscribe(n => println("n = " + n))
  o.toBlockingObservable.toIterable.last

  println("done")

}