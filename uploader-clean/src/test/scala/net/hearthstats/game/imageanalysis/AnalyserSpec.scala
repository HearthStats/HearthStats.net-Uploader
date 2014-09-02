package net.hearthstats.game.imageanalysis

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import javax.imageio.ImageIO
import org.scalatest.junit.JUnitRunner
import net.hearthstats.game.Screen

@RunWith(classOf[JUnitRunner])
class AnalyserSpec extends FlatSpec with Matchers {

  "The play lobby screen" should "be detected" in
    checkScreen("play_lobby", Screen.PLAY_LOBBY)

  "The practice lobby screen" should "be detected" in {
    checkScreen("practice_lobby", Screen.PRACTICE_LOBBY)
    checkScreen("practice_lobby2", Screen.PRACTICE_LOBBY)
  }

  "The arena choose hero screen" should "be detected" in
    checkScreen("arena_choose", Screen.ARENA_CHOOSE)

  "The versus lobby" should "be detected" in
    checkScreen("versus_lobby", Screen.VERSUS_LOBBY)

  def checkScreen(fileName: String, screen: Screen) = {
    val img = ImageIO.read(getClass.getResourceAsStream(fileName + ".png"))
    (new ScreenAnalyser).identifyScreen(img, null) shouldBe screen
  }

}