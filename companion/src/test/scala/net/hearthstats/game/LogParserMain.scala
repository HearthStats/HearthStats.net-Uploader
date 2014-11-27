package net.hearthstats.game

import scala.concurrent.duration.DurationInt
import scala.collection.JavaConversions._

object LogParserMain extends App {
  val p = new LogParser
  val lines = io.Source.fromInputStream(getClass.getResourceAsStream("legend_log.txt")).getLines

  for {
    l <- lines
    r <- p.analyseLine(l)
  } {
    println(r)
  }
}

