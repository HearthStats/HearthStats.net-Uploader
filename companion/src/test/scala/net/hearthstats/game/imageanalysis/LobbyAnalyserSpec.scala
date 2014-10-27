package net.hearthstats.game.imageanalysis

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import javax.imageio.ImageIO
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LobbyAnalyserSpec extends FlatSpec with Matchers {
  val analyser = new LobbyAnalyser

  "selected deck slot 7" should "be detected" in {
    val img = ImageIO.read(getClass.getResourceAsStream("play_lobby.png"))
    analyser.imageIdentifyDeckSlot(img) shouldBe Some(7)
  }

  "selected deck slot 8" should "be detected" in {
    val img = ImageIO.read(getClass.getResourceAsStream("practice_lobby2.png"))
    analyser.imageIdentifyDeckSlot(img) shouldBe Some(8)
  }
}