package net.hearthstats.core

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LogMonitorSpec extends FlatSpec with Matchers {

  it should "parse correctly collectible cards" in {
    val ambusher = CardData.collectible
      .filter {
        case c => c.playerClass == Some("Rogue") && c.mechanics.getOrElse(Nil).contains("Deathrattle")
      }.head
    ambusher.name shouldBe "Anub'ar Ambusher"
  }

  it should "parse correctly hero powers" in {
    val steadyShot = CardData.heroPowers
      .filter {
        case c => c.playerClass == Some("Hunter")
      }.head
    steadyShot.cost shouldBe Some(2)
  }

  it should "contain Acidic Swamp Ooze" in {
    CardData.byId("EX1_066").name shouldBe "Acidic Swamp Ooze"
  }

  it should "contain GvG (Annoy-o-Tron)" in {
    CardData.byId("GVG_085").name shouldBe "Annoy-o-Tron"
  }

}

