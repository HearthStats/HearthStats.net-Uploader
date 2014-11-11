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
  var iterationsSinceClassCheckingStarted = 0
  var isYourTurn = false

  var ongoingVideo: Option[OngoingVideo] = None

  var lastScreen: Option[Screen] = None

  var iterationsSinceScreenMatched = 0

  var playerId1: Option[Int] = None // used in CoinReceived and id for zone events
  var opponentId1: Option[Int] = None // used in CoinReceived and id for zone events
  var playerId2: Option[Int] = None // used in FirstPlayer and PlayerName events
  var opponentId2: Option[Int] = None // used in FirstPlayer and PlayerName events

  def reset() {
    playerId1 = None
    opponentId1 = None
    playerId2 = None
    opponentId2 = None
    info("reset companion state")
  }
}

