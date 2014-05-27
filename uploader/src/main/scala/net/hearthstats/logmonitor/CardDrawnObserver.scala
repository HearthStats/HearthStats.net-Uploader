package net.hearthstats.logmonitor

trait CardDrawnObserver {
  def cardDrawn(card: String): Unit
  //when you mulligan
  def cardPutBack(card: String): Unit
}