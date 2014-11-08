package net.hearthstats.game

import rx.lang.scala.Observable
import rx.lang.scala.schedulers._
import scala.concurrent.duration.DurationInt
import scala.collection.JavaConversions._
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.Subject
import net.hearthstats.util.ObservableExtensions

object LogParserMain extends App {
  val p = new LogParser
  val lines = io.Source.fromInputStream(getClass.getResourceAsStream("several_games_log.txt")).getLines

  for {
    l <- lines
    r <- p.analyseLine(l)
  } {
    println(r)
    if (r.isInstanceOf[GameOver]) System.exit(0)
  }
}

object Tumb extends App {
  val numbers = Observable.from(1 to 15).take(100).publish
  import ObservableExtensions._
  numbers.connect
  val tu = numbers.span(_ % 2 == 0)
  val firstGameTurns = for {
    group <- tu
    e <- group
  } yield s"$e from $group"
  firstGameTurns.toBlocking.toList foreach println
}

