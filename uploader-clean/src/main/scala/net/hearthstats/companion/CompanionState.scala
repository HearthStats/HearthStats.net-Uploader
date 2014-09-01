package net.hearthstats.companion

import grizzled.slf4j.Logging
import net.hearthstats.core.GameMode
import net.hearthstats.core.Rank

/**
 * Current perception of HearthStone game by the companion.
 */
class CompanionState extends Logging {
  debug("new CompanionState")

  var gameDetected: GameDetectionStatus = Unknown
  var mode: Option[GameMode] = None
  var deckSlot: Option[Int] = None
  var rank: Option[Rank] = None
  var isNewArenaRun = false
}

sealed trait GameDetectionStatus

case object GameDetected extends GameDetectionStatus
case object GameNotDetected extends GameDetectionStatus
case object Unknown extends GameDetectionStatus