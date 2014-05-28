package net.hearthstats.logmonitor

import net.hearthstats.Card

trait CardDrawnObserver {
  def cardDrawn(card: Card): Unit
  //when you mulligan
  def cardPutBack(card: Card): Unit
}