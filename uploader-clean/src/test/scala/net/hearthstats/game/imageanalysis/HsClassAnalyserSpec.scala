package net.hearthstats.game.imageanalysis

import org.junit.runner.RunWith

import org.scalatest.{ FlatSpec, Matchers }
import javax.imageio.ImageIO
import org.scalatest.junit.JUnitRunner
import net.hearthstats.game.Screen
import net.hearthstats.core.HeroClass._

@RunWith(classOf[JUnitRunner])
class HsClassAnalyserSpec extends FlatSpec with Matchers {

  "Both classes" should "be detected" in {
    val img = ImageIO.read(getClass.getResourceAsStream("Druid_VS_Hunter.png"))
    (new HsClassAnalyser).imageIdentifyYourClass(img) shouldBe Some(DRUID)
    (new HsClassAnalyser).imageIdentifyOpponentClass(img) shouldBe Some(HUNTER)
  }
}