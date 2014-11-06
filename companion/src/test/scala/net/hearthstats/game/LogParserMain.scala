package net.hearthstats.game

import rx.lang.scala.Observable
import rx.lang.scala.schedulers._
import scala.concurrent.duration.DurationInt

object LogParserMain extends App {
  val p = new LogParser
  val lines = io.Source.fromInputStream(getClass.getResourceAsStream("several_games_log.txt")).getLines

  for {
    l <- lines
    r <- p.analyseLine(l)
  } {
    println(r)
  }
}