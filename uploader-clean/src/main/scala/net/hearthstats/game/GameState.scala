package net.hearthstats.game

import net.hearthstats.core.HearthstoneMatch

class GameState {
  var currentMatch: Option[HearthstoneMatch] = None
  var lastMatch: Option[HearthstoneMatch] = None

  def setOpponentName(n: String) = currentMatch.get.opponentName = n
  def setNotes(n: String) = currentMatch.get.notes = n
  def setCoin(c: Boolean) = currentMatch.get.coin = c

  def lastMatchUrl: Option[String] =
    for (m <- lastMatch) yield {
      if ("Arena" == m.mode) "http://hearthstats.net/arenas/new"
      else m.editUrl
    }

}