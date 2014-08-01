package net.hearthstats.companion

import grizzled.slf4j.Logging
import net.hearthstats.core.GameMode
import net.hearthstats.core.Rank

/**
 * Current perception of HearthStone game by the companion.
 */
class CompanionState extends Logging {

  var mode: Option[GameMode] = None
  var deckSlot: Option[Int] = None
  var rank: Option[Rank] = None

}