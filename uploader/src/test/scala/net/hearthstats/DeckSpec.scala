package net.hearthstats

import java.net.URL
import org.junit.Test
import scala.collection.GenIterable
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DeckSpec extends FlatSpec with Matchers {

  "A Deck with 30 cards" should "be a valid deck" in {
    Deck(cards = Deck.parseCardString("86_2,297_2,175_2,135_2,212_2,272_2,253_2,281_1,160_2,87_1,78_2,24_2,257_2,349_1,361_1,53_2,218_2"))
      .isValid shouldBe true
    Deck(cards = Deck.parseCardString("4_1,18_2,30_2,40_2,45_2,55_1,58_2,92_2,127_2,211_2,241_1,258_1,289_2,303_1,308_2,362_2,374_2,378_1"))
      .isValid shouldBe true
  }

  "A Deck with less than 30 cards" should "not be a valid deck" in {
    Deck(cards = Nil).isValid shouldBe false
    Deck(cards = Deck.parseCardString("4_1,18_2")).isValid shouldBe false
  }
}