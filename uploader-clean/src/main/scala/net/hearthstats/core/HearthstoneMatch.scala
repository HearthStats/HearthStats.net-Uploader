package net.hearthstats.core

import scala.collection.JavaConversions.mutableMapAsJavaMap
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONObject
import com.github.nscala_time.time.Imports.DateTime
import grizzled.slf4j.Logging

//TODO use options
//TODO avoid mutable 
class HearthstoneMatch(var mode: GameMode = GameMode.UNDETECTED,
  var userClass: HeroClass = HeroClass.UNDETECTED,
  var opponentClass: HeroClass = HeroClass.UNDETECTED,
  var coin: Option[Boolean] = None,
  var result: Option[MatchOutcome] = None,
  var deck: Option[Deck] = None,
  var opponentName: String = null,
  var rankLevel: Option[Rank] = None,
  var numTurns: Int = -1,
  var duration: Int = -1,
  var notes: String = null,
  var id: Int = -1) extends Logging {

  debug("new HearthstoneMatch")

  private var _userClassUnconfirmed: Boolean = true
  //needed for java calls
  def this() = this(mode = null)
  
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

  def editUrl: String =
    s"http://hearthstats.net/constructeds/$id/edit"
    
  def endMatch = duration =  Math.round((System.currentTimeMillis - startedAt) / 1000)
}
