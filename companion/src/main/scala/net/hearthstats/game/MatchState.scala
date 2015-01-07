package net.hearthstats.game

import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.Rank
import net.hearthstats.core.GameMode
import net.hearthstats.companion.CompanionState
import net.hearthstats.core.HeroClass
import net.hearthstats.core.GameLog

class MatchState {
  var currentMatch: Option[HearthstoneMatch] = None
  var lastMatch: Option[HearthstoneMatch] = None
  var submitted = false
  var started = false
  var jsonGameLog: Option[GameLog] = None

  def setOpponentName(n: String) = updateMatch(_.withOpponentName(n))
  def setNotes(n: String) = updateMatch(_.copy(notes = n))
  def setCoin(c: Boolean) = updateMatch(_.withCoin(c))
  def setOpponentClass(hc: HeroClass) = updateMatch(_.withOpponentClass(hc))
  def setUserClass(hc: HeroClass) = updateMatch(_.withUserClass(hc))

  def lastMatchUrl: Option[String] =
    for (m <- lastMatch) yield {
      if ("Arena" == m.mode) "http://hearthstats.net/arenas/new"
      else m.matchUrl 
      }

  def nextMatch(companionState: CompanionState): Unit = {
    lastMatch = currentMatch
    currentMatch = Some(new HearthstoneMatch(
      mode = companionState.mode,
      rankLevel = companionState.rank))
    jsonGameLog = Some(GameLog())
    submitted = false
    started = false
  }

  def updateLog(event: GameEvent, time: Int) =
    jsonGameLog = jsonGameLog.map(_.addEvent(event, time))

  private def updateMatch(f: HearthstoneMatch => HearthstoneMatch): Unit =
    currentMatch = Some(f(currentMatch.get))

}