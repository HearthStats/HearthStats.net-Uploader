package net.hearthstats.core

import org.apache.commons.lang3.StringUtils
import rapture.json.jsonBackends.jawn._
import rapture.json._
import com.github.nscala_time.time.Imports.DateTime
import grizzled.slf4j.Logging
import scala.concurrent.Future
import scala.concurrent.Promise
import java.util.regex.MatchResult

case class HearthstoneMatch(mode: GameMode = GameMode.UNDETECTED,
  userClass: HeroClass = HeroClass.UNDETECTED,
  opponentClass: HeroClass = HeroClass.UNDETECTED,
  coin: Option[Boolean] = None,
  result: Option[MatchOutcome] = None,
  deck: Option[Deck] = None,
  opponentName: String = null,
  rankLevel: Option[Rank] = None,
  numTurns: Int = -1,
  duration: Long = -1, // in seconds
  notes: String = null,
  jsonLog: String = null,
  replayFile: Future[String] = Promise[String].future,
  id: Int = -1) extends Logging {

  def this() = this(GameMode.UNDETECTED)

  def describeResult: String = result match {
    case Some(r) => r.toString
    case None => "UnknownResult"
  }

  def deckSlot: Option[Int] = for {
    d <- deck
    s <- d.activeSlot
  } yield s

  def toJsonObject: Json = {
    val slot = deckSlot.getOrElse(-1)
    val main = json""" {
      "mode" : ${mode.toString},
      "slot" : $slot,
      "class" : ${userClass.toString},
      "oppclass" : ${opponentClass.toString},
      "oppname" : $opponentName,
      "coin" : ${coin.getOrElse(false).toString},
      "result" : $describeResult,
      "notes" : $notes,
      "log" : $jsonLog,
      "numturns" : ${numTurns / 2},
      "duration" : $duration
      }"""
    val ranks =
      if (mode == GameMode.RANKED) {
        val (lvl, legend) =
          if (Rank.LEGEND == rankLevel) (26, true)
          else (rankLevel.get.number, false)
        json""" {"ranklvl" :$lvl, "legend" : $legend } """
      } else Json.empty

    Json(main ++ ranks)
  }

  /**
   * Determines if the data for this match is complete.
   *
   * @return true if there is enough data to submit the match, false if some data is missing
   */
  def isDataComplete: Boolean =
    mandatoryFieldsOK &&
      (mode match {
        case GameMode.RANKED => rankLevel.isDefined && deckSlotOk
        case GameMode.CASUAL => deckSlotOk
        case _ => true
      })

  def deckSlotOk = deckSlot.isDefined &&
    deckSlot.get >= 1 && deckSlot.get <= 9

  def mandatoryFieldsOK =
    result.isDefined &&
      userClass != HeroClass.UNDETECTED &&
      opponentClass != HeroClass.UNDETECTED &&
      mode != GameMode.UNDETECTED

  def matchUrl: String = {
    s"http://hearthstats.net/matches/$id"
  }
  //methods to update the match (return an updated copy)

  def withDeck(d: Deck) = copy(deck = Some(d))
  def withMode(m: GameMode) = copy(mode = m)
  def withRankLevel(r: Rank) = copy(rankLevel = Some(r))
  def withCoin(b: Boolean) = copy(coin = Some(b))
  def withResult(r: MatchOutcome) = copy(result = Some(r))
  def withOpponentName(n: String) = copy(opponentName = n)
  def withOpponentClass(c: HeroClass) = copy(opponentClass = c)
  def withUserClass(c: HeroClass) = copy(userClass = c)
  def withReplay(r: Future[String]) = copy(replayFile = r)
  def withDuration(d: Long) = copy(duration = d)
  def withJsonLog(log: GameLog) = copy(jsonLog = log.toJson)
}
