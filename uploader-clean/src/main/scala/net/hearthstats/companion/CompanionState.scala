package net.hearthstats.companion

import grizzled.slf4j.Logging
import net.hearthstats.core.GameMode

/**
 * Current perception of HearthStone game by the companion.
 */
class CompanionState extends Logging {

  var mode: Option[GameMode] = None
  var deckSlot: Option[Int] = None


}