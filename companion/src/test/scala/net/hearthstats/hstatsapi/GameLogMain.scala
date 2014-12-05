package net.hearthstats.hstatsapi

import net.hearthstats.game.LogParser
import net.hearthstats.core.GameLog

object GameLogMain extends App {
  val p = new LogParser
  //  val lines = io.Source.fromInputStream(classOf[CardEvent].getResourceAsStream("gamelog.txt")).getLines
  val lines = io.Source.fromFile("""C:\Program Files (x86)\Hearthstone\Hearthstone_Data\output_log.txt""").getLines

  val events = for {
    l <- lines
    r <- p.analyseLine(l)
  } yield {
    println(r)
    r
  }

  val gameLog = events.foldLeft(GameLog()) {
    case (log, e) => log.addEvent(e)
  }

  println(gameLog.toJson)
}