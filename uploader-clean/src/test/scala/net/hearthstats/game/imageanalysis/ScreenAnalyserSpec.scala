package net.hearthstats.game.imageanalysis

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import javax.imageio.ImageIO
import org.scalatest.junit.JUnitRunner
import net.hearthstats.game.Screen

@RunWith(classOf[JUnitRunner])
class ScreenAnalyserSpec extends FlatSpec with Matchers {

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

  "Finding opponent" should "be detected" in
    checkScreen("finding", Screen.FINDING_OPPONENT)

  "Pandaria end" should "be detected" in
    checkScreen("victory_pandaria", Screen.MATCH_PANDARIA_END)

  "Pandaria end (win streak)" should "be detected" in
    checkScreen("win_streak_pandaria", Screen.MATCH_PANDARIA_END)

  "Defeat Stranglehorn" should "be detected" in
    checkScreen("defeat_stranglehorn", Screen.MATCH_STRANGLETHORN_END)

  "Orgrimmar" should "be detected" in
    checkScreen("orgrimmar_with_coin", Screen.MATCH_ORGRIMMAR)

  "Starting hand" should "be detected" in
    checkScreen("starting_hand_4_cards", Screen.MATCH_STARTINGHAND)

  "Match VS" should "be detected" in
    checkScreen("Druid_VS_Hunter", Screen.MATCH_VS)

  def checkScreen(fileName: String, screen: Screen) = {
    val img = ImageIO.read(getClass.getResourceAsStream(fileName + ".png"))
    (new ScreenAnalyser).identifyScreen(img, null) shouldBe screen
  }

}