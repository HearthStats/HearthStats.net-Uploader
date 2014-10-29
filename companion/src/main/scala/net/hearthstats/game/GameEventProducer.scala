package net.hearthstats.game

import rx.lang.scala.Observable

trait GameEventProducer {
  val gameEvents: Observable[GameEvent]

}