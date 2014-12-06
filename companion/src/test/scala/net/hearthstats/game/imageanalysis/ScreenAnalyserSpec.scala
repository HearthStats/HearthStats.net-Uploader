package net.hearthstats.game.imageanalysis

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import javax.imageio.ImageIO
import org.scalatest.junit.JUnitRunner
import net.hearthstats.game.Screen
import net.hearthstats.game.Screen._

@RunWith(classOf[JUnitRunner])
class ScreenAnalyserSpec extends FlatSpec with Matchers {
  val analyser = new ScreenAnalyser

  "The main menu screen" should "be detected" in
    checkScreen("main_menu_glowing", MAIN)

  "The play lobby screen" should "be detected" in
    checkScreen("play_lobby", PLAY_LOBBY)

  "The practice lobby screen" should "be detected" in {
    checkScreen("practice_lobby", PRACTICE_LOBBY)
    checkScreen("practice_lobby2", PRACTICE_LOBBY)
  }

  "The arena choose hero screen" should "be detected" in
    checkScreen("arena_choose", ARENA_CHOOSE)

  "The versus lobby" should "be detected" in
    checkScreen("versus_lobby", VERSUS_LOBBY)

  "Finding opponent" should "be detected" in
    checkScreen("finding", FINDING_OPPONENT)

  "Finding opponent" should "be detected after Play Lobby" in
    checkScreen("finding", FINDING_OPPONENT, PLAY_LOBBY)

  "Match VS" should "be detected" in
    checkScreen("Druid_VS_Hunter", MATCH_VS)

  "Match VS" should "be detected after Find Opp" in
    checkScreen("Druid_VS_Hunter", MATCH_VS, FINDING_OPPONENT)

  "Starting hand" should "be detected" in
    checkScreen("starting_hand_4_cards", MATCH_STARTINGHAND)

  "Starting hand" should "be detected after Match VS" in
    checkScreen("starting_hand_4_cards", MATCH_STARTINGHAND, MATCH_VS)

  "Pandaria end" should "be detected" in
    checkScreen("victory_pandaria", MATCH_PANDARIA_END)

  "Pandaria end (win streak)" should "be detected" in
    checkScreen("win_streak_pandaria", MATCH_PANDARIA_END)

  "Defeat Stranglehorn" should "be detected" in
    checkScreen("defeat_stranglehorn", MATCH_STRANGLETHORN_END)

  "Orgrimmar" should "be detected" in
    checkScreen("orgrimmar_with_coin", MATCH_ORGRIMMAR)

  def checkScreen(fileName: String, screen: Screen, previous: Screen = null) = {
    val img = ImageIO.read(getClass.getResourceAsStream(fileName + ".png"))
    analyser.identifyScreen(img, previous) shouldBe screen
  }

}