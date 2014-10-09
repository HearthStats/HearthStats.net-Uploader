package net.hearthstats.game

import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.Rank
import net.hearthstats.core.GameMode
import net.hearthstats.companion.CompanionState

class MatchState {
  var currentMatch: Option[HearthstoneMatch] = None
  var lastMatch: Option[HearthstoneMatch] = None
  var startTime: Long = _
  var submitted = false

  def setOpponentName(n: String) = currentMatch.get.opponentName = n
  def setNotes(n: String) = currentMatch.get.notes = n
  def setCoin(c: Boolean) = currentMatch.get.coin = Some(c)

  def lastMatchUrl: Option[String] =
    for (m <- lastMatch) yield {
      if ("Arena" == m.mode) "http://hearthstats.net/arenas/new"
      else m.editUrl
    }

  def nextMatch(companionState: CompanionState): Unit = {
    lastMatch = currentMatch
    currentMatch = Some(new HearthstoneMatch(
      mode = companionState.mode,
      rankLevel = companionState.rank,
      deckSlot = companionState.deckSlot))
    submitted = false
  }

}