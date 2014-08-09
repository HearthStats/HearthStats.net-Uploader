package net.hearthstats.game.imageanalysis

import java.awt.image.BufferedImage
import UniquePixel._
import net.hearthstats.game.ocr.RankLevelOcr
import net.hearthstats.core.Rank

sealed trait LobbyMode
case object Casual extends LobbyMode
case object Ranked extends LobbyMode

class LobbyAnalyser {
  val individualPixelAnalyser = new IndividualPixelAnalyser
  val rankLevelOcr = new RankLevelOcr

  def mode(image: BufferedImage): Option[LobbyMode] = {
    val casual = imageShowsCasualPlaySelected(image)
    val ranked = imageShowsRankedPlaySelected(image)
    if (casual && !ranked) Some(Casual)
    else if (ranked && !casual) Some(Ranked)
    else None
  }

  def analyzeRankLevel(image: BufferedImage): Option[Rank] = {
    val rankInteger = rankLevelOcr.processNumber(image)
    Option(rankInteger) map Rank.fromInt
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