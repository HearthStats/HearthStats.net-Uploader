package net.hearthstats

import rx.lang.scala._
import rx.lang.scala.schedulers._
import scala.concurrent.duration._

object RxScalaMain extends App {
  val o = Observable.interval(200 millis).take(5)
  o.subscribe(n => println("n = " + n))
  o.toBlockingObservable.toIterable.last

  println("done")
}