package net.hearthstats.game

import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.Rank

class MatchState {
  var currentMatch: Option[HearthstoneMatch] = None
  var lastMatch: Option[HearthstoneMatch] = None
  var rankLevel: Rank = _
  var startTime: Long = _

  def setOpponentName(n: String) = currentMatch.get.opponentName = n
  def setNotes(n: String) = currentMatch.get.notes = n
  def setCoin(c: Boolean) = currentMatch.get.coin = Some(c)

  def lastMatchUrl: Option[String] =
    for (m <- lastMatch) yield {
      if ("Arena" == m.mode) "http://hearthstats.net/arenas/new"
      else m.editUrl
    }

  def nextMatch(): Unit = {
    lastMatch = currentMatch
    currentMatch = Some(new HearthstoneMatch)
  }

}