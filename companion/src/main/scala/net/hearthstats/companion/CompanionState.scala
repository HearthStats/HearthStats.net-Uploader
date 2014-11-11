package net.hearthstats.companion

import grizzled.slf4j.Logging
import net.hearthstats.core.GameMode
import net.hearthstats.core.Rank
import net.hearthstats.game.Screen
import net.hearthstats.modules.video.OngoingVideo

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

  var iterationsSinceScreenMatched = 0

  var playerId1: Option[Int] = None // used in CoinReceived and id for zone events
  var opponentId1: Option[Int] = None // used in CoinReceived and id for zone events

  def reset() {
    playerId1 = None
    opponentId1 = None
    otherPlayerName = None
    firstPlayerName = None
    info("reset companion state")
  }
}

