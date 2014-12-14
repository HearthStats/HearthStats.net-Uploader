package net.hearthstats.companion

import java.awt.image.BufferedImage

import scala.collection.mutable.Buffer

import grizzled.slf4j.Logging
import net.hearthstats.core.{ GameMode, Rank }
import net.hearthstats.game.Screen
import net.hearthstats.modules.video.{ OngoingVideo, TimedImage }

/**
 * Current perception of HearthStone game by the companion.
 */
class CompanionState extends Logging {
  debug("new CompanionState")

  var mode = GameMode.UNDETECTED
  var deckSlot: Option[Int] = None
  var rank: Option[Rank] = None
  var isNewArenaRun = false
  var firstPlayerName: Option[String] = None
  var otherPlayerName: Option[String] = None

  var ongoingVideo: Option[OngoingVideo] = None

  var lastScreen: Option[Screen] = None

  var matchStartedAt: Long = 0

  def currentDurationMs: Int =
    (timeMs - matchStartedAt).toInt

  //this is deliberately called twice :
  //once on game log start (to clear duration and avoid double submit)
  //also on game start screen (to get precise duration for video recording sync with game log)
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
    // Only add images during video recording
    if (ongoingVideo.isDefined) {
      if (lastImages.size >= maxImages) {
        lastImages.remove(0)
      }
      debug(s"image captured $currentDurationMs ms after game start")
      lastImages.append(TimedImage(bi, currentDurationMs))
    }
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

