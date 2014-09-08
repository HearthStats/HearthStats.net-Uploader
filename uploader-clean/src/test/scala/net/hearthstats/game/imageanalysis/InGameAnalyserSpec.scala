package net.hearthstats.game.imageanalysis

import org.junit.runner.RunWith

import org.scalatest.{ FlatSpec, Matchers }
import javax.imageio.ImageIO
import org.scalatest.junit.JUnitRunner
import net.hearthstats.game.Screen
import net.hearthstats.core.HeroClass._
import net.hearthstats.core.MatchOutcome._

@RunWith(classOf[JUnitRunner])
class InGameAnalyserSpec extends FlatSpec with Matchers {
  val analyser = new InGameAnalyser

  "Victory" should "be detected" in {
    val img = ImageIO.read(getClass.getResourceAsStream("victory_pandaria.png"))
    analyser.imageShowsVictoryOrDefeat(img) shouldBe Some(VICTORY)
  }

  "Defeat" should "be detected" in {
    val img = ImageIO.read(getClass.getResourceAsStream("defeat_stranglehorn.png"))
    analyser.imageShowsVictoryOrDefeat(img) shouldBe Some(DEFEAT)
  }

  //TODO someday ...
  //  "Victory (win streak)" should "be detected" in {
  //    val img = ImageIO.read(getClass.getResourceAsStream("win_streak_pandaria.png"))
  //    analyser.imageShowsVictoryOrDefeat(img) shouldBe Some(VICTORY)
  //  }
}