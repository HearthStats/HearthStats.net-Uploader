package net.hearthstats.ui

import net.hearthstats.core.HeroClass

trait HearthstatsPresenter {
  def setOpponentClass(heroClass: HeroClass): Unit
  def setYourClass(heroClass: HeroClass): Unit
  def setOpponentName(n: String): Unit
  def setCoin(coin: Boolean): Unit

}