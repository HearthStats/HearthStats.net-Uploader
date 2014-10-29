package net.hearthstats.ui

import net.hearthstats.core.HeroClass
import net.hearthstats.core.HearthstoneMatch

trait HearthstatsPresenter {
  def setOpponentClass(heroClass: HeroClass): Unit
  def setYourClass(heroClass: HeroClass): Unit
  def setOpponentName(n: String): Unit
  def setCoin(coin: Boolean): Unit
  def matchSubmitted(m: HearthstoneMatch, description: String): Unit
}