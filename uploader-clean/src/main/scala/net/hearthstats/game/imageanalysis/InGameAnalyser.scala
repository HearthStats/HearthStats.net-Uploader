package net.hearthstats.game.imageanalysis

import java.awt.image.BufferedImage
import UniquePixel._
import net.hearthstats.core.HeroClass
import net.hearthstats.core.HeroClass._
import net.hearthstats.core.MatchOutcome

class InGameAnalyser extends ImageAnalyser {
  val relativePixelAnalyser = new RelativePixelAnalyser

  def imageShowsCoin(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAnyPixelsMatch(image, Array(COIN_1, COIN_2, COIN_3, COIN_4, COIN_5))

  def imageShowsOpponentTurn(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_OPPONENT_1A, TURN_OPPONENT_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_OPPONENT_2A, TURN_OPPONENT_2B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_OPPONENT_3A, TURN_OPPONENT_3B))

  def imageShowsYourTurn(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_YOUR_1A, TURN_YOUR_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_YOUR_2A, TURN_YOUR_2B))

  def imageShowsOpponentName(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(NAME_OPPONENT_1A, NAME_OPPONENT_1B, NAME_OPPONENT_1C)) &&
      individualPixelAnalyser.testAllPixelsMatch(image, Array(NAME_OPPONENT_2A, NAME_OPPONENT_2B, NAME_OPPONENT_2C))

  def imageShowsVictoryOrDefeat(image: BufferedImage): Option[MatchOutcome] = {
    val referenceCoordinate =
      relativePixelAnalyser.findRelativePixel(image, VICTORY_DEFEAT_REFBOX_TL, VICTORY_DEFEAT_REFBOX_BR, 8, 11)
    if (null != referenceCoordinate) {
      val victory1Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(VICTORY_REL_1A, VICTORY_REL_1B))
      val victory2Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(VICTORY_REL_2A, VICTORY_REL_2B, VICTORY_REL_2C))
      val defeat1Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(DEFEAT_REL_1A, DEFEAT_REL_1B, DEFEAT_REL_1C, DEFEAT_REL_1D, DEFEAT_REL_1E))
      val defeat2Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(DEFEAT_REL_2A))
      val matchedVictory = victory1Matches > 0 && victory2Matches == 3 &&
        defeat1Matches == 0 && defeat2Matches == 0
      val matchedDefeat = victory1Matches == 0 && victory2Matches == 0 &&
        defeat1Matches > 0 && defeat2Matches == 1
      if (matchedVictory && matchedDefeat) {
        warn("Matched both victory and defeat, which shouldn't be possible. Will try again next iteration.")
        None
      } else if (matchedVictory) Some(MatchOutcome.VICTORY)
      else if (matchedDefeat) Some(MatchOutcome.DEFEAT)
      else None
    } else None
  }
}