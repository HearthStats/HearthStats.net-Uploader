package net.hearthstats.core

import scala.collection.JavaConversions.mutableMapAsJavaMap
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONObject
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
  duration: Int = -1,
  notes: String = null,
  replayFile: Future[String] = Promise[String].future,
  id: Int = -1) extends Logging {

  info("new HearthstoneMatch")

  def this() = this(GameMode.UNDETECTED)

  val startedAt: Long = System.currentTimeMillis

  def describeResult: String = result match {
    case Some(r) => r.toString
    case None => "UnknownResult"
  }

  def deckSlot: Option[Int] = for {
    d <- deck
    s <- d.activeSlot
  } yield s

  def toJsonObject: JSONObject = {
    val map = collection.mutable.Map(
      "mode" -> mode.toString,
      "slot" -> deckSlot.getOrElse(-1),
      "class" -> userClass.toString,
      "oppclass" -> opponentClass.toString,
      "oppname" -> opponentName,
      "coin" -> coin.toString,
      "result" -> describeResult,
      "notes" -> notes,
      "numturns" -> numTurns,
      "duration" -> duration)

    if (mode == GameMode.RANKED) {
      if (Rank.LEGEND == rankLevel) {
        map += "ranklvl" -> 26
        map += "legend" -> "true"
      } else {
        map += "ranklvl" -> rankLevel.get.number
        map += "legend" -> "false"
      }
    }
    new JSONObject(map)
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
      StringUtils.isNotBlank(opponentName) &&
      mode != GameMode.UNDETECTED

  def editUrl: String = {
    val m = if (mode == GameMode.ARENA) "arenas" else "constructeds"
    s"http://hearthstats.net/$m/$id/edit"
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
  def withNewTurn = copy(numTurns = numTurns + 1)

  def endMatch() =
    copy(duration = Math.round((System.currentTimeMillis - startedAt) / 1000))
}
