package net.hearthstats.game.imageanalysis

import java.awt.image.BufferedImage
import UniquePixel._
import net.hearthstats.game.ocr.RankLevelOcr
import net.hearthstats.core.Rank

sealed trait LobbyMode
case object Casual extends LobbyMode
case object Ranked extends LobbyMode

class LobbyAnalyser extends ImageAnalyser {

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

  def isNewArenaRun(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(NEW_ARENA_RUN_A, NEW_ARENA_RUN_B, NEW_ARENA_RUN_C, NEW_ARENA_RUN_D, NEW_ARENA_RUN_E))

  def imageIdentifyDeckSlot(image: BufferedImage): Option[Int] = {
    val res =
      identify(image, Seq(
        Array(DECK_SLOT_1A, DECK_SLOT_1B) -> 1,
        Array(DECK_SLOT_2A, DECK_SLOT_2B) -> 2,
        Array(DECK_SLOT_3A, DECK_SLOT_3B) -> 3,
        Array(DECK_SLOT_4A, DECK_SLOT_4B) -> 4,
        Array(DECK_SLOT_5A, DECK_SLOT_5B) -> 5,
        Array(DECK_SLOT_6A, DECK_SLOT_6B) -> 6,
        Array(DECK_SLOT_7A, DECK_SLOT_7B) -> 7,
        Array(DECK_SLOT_8A, DECK_SLOT_8B) -> 8,
        Array(DECK_SLOT_9A, DECK_SLOT_9B) -> 9))
    res match {
      case Some(deckSlot) => info(s"deck $deckSlot selected")
      case None => info(s"deckslot not detected")
    }
    res
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