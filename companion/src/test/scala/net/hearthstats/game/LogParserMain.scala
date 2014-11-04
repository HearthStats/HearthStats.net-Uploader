package net.hearthstats.game

object LogParserMain extends App {
  val p = new LogParser
  val lines = io.Source.fromInputStream(getClass.getResourceAsStream("gamelog.txt")).getLines

  for {
    l <- lines
    r <- p.analyseLine(l)
  } {
    println(r)
  }
}