package net.hearthstats.companion

import grizzled.slf4j.Logging
import net.hearthstats.core.GameMode
import net.hearthstats.core.Rank
import net.hearthstats.game.Screen
import net.hearthstats.modules.video.OngoingVideo
import net.hearthstats.modules.video.TimedImage
import scala.collection.mutable.Buffer
import java.awt.image.BufferedImage
import net.hearthstats.modules.video.TimedImage

/**
 * Current perception of HearthStone game by the companion.
 */
class CompanionState extends Logging {
  debug("new CompanionState")

  var mode = GameMode.UNDETECTED
  var deckSlot: Option[Int] = None
  var rank: Option[Rank] = None
  var isNewArenaRun = false
  var isYourTurn = false
  var firstPlayerName: Option[String] = None
  var otherPlayerName: Option[String] = None

  var ongoingVideo: Option[OngoingVideo] = None

  var lastScreen: Option[Screen] = None

  var matchStartedAt: Long = 0

  def currentDurationMs: Long =
    timeMs - matchStartedAt

  def startMatch(): Unit = {
    matchStartedAt = timeMs
    lastImages.clear()
    info(s"start match at $matchStartedAt ms")
  }

  def timeMs = System.nanoTime / 1000000

  var iterationsSinceScreenMatched = 0

  var playerId1: Option[Int] = None // used in CoinReceived and id for zone events
  var opponentId1: Option[Int] = None // used in CoinReceived and id for zone events

  val maxImages = 20

  private val lastImages = Buffer.empty[TimedImage]

  def addImage(bi: BufferedImage): Unit = lastImages.synchronized {
    if (lastImages.size >= maxImages) {
      lastImages.remove(0)
    }
    info(s"image captured $currentDurationMs ms after game start")
    lastImages.append(TimedImage(bi, currentDurationMs))
  }

  def imagesAfter(afterMs: Long): Iterable[TimedImage] = lastImages.synchronized {
    lastImages.toList.filter(_.timeMs > afterMs)
  }

  def reset() {
    playerId1 = None
    opponentId1 = None
    otherPlayerName = None
    firstPlayerName = None
    lastImages.clear()
    info("reset companion state")
  }
}

