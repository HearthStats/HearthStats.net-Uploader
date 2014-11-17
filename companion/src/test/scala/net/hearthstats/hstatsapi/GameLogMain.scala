package net.hearthstats.hstatsapi

import net.hearthstats.game.LogParser
import net.hearthstats.game.TurnPassedEvent
import net.hearthstats.game.CardEvent
import net.hearthstats.core.GameLog._
import net.hearthstats.game.CardEvent
import net.hearthstats.game.StartupEvent

object GameLogMain extends App {
  val p = new LogParser
  val lines = io.Source.fromInputStream(classOf[CardEvent].getResourceAsStream("gamelog.txt")).getLines

  val events = for {
    l <- lines
    r <- p.analyseLine(l)
    _ = println(r)
  } yield r

  val gameLog = events.foldLeft(List.empty[Turn]) {
    case (Nil, StartupEvent) => List(Turn(0, Nil))
    case (t :: turns, TurnPassedEvent) =>
      val drawn = t.actions.last
      val rest = t.actions.dropRight(1)
      List(Turn(0, List(drawn))) ::: t.copy(actions = rest) :: turns
    case (t :: turns, e: CardEvent) => t.addEvent(e, 0) :: turns
    case (turns, _) => turns
  }

  println(gameLogToString(gameLog))
}