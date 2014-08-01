package net.hearthstats.game.imageanalysis

import java.awt.image.BufferedImage

import UniquePixel._

sealed trait LobbyMode
case object Casual extends LobbyMode
case object Ranked extends LobbyMode

class LobbyAnalyser {
  val individualPixelAnalyser = new IndividualPixelAnalyser

  def mode(image: BufferedImage): Option[LobbyMode] = {
    val casual = imageShowsCasualPlaySelected(image)
    val ranked = imageShowsRankedPlaySelected(image)
    if (casual && !ranked) Some(Casual)
    else if (ranked && !casual) Some(Ranked)
    else None
  }

  private def imageShowsCasualPlaySelected(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_CASUAL_1A, MODE_CASUAL_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_CASUAL_2A, MODE_CASUAL_2B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_CASUAL_3A, MODE_CASUAL_3B))

  private def imageShowsRankedPlaySelected(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_RANKED_1A, MODE_RANKED_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_RANKED_2A, MODE_RANKED_2B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_RANKED_3A, MODE_RANKED_3B))

}