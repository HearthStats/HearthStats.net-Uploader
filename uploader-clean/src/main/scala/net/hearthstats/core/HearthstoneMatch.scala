package net.hearthstats.core

import scala.collection.JavaConversions.mutableMapAsJavaMap
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONObject
import com.github.nscala_time.time.Imports.DateTime
import grizzled.slf4j.Logging

//TODO use options
//TODO avoid mutable 
class HearthstoneMatch(var mode: Option[GameMode] = None,
  var userClass: HeroClass = HeroClass.UNDETECTED,
  var opponentClass: HeroClass = HeroClass.UNDETECTED,
  var coin: Option[Boolean] = None,
  var result: Option[MatchOutcome] = None,
  var deckSlot: Option[Int] = None,
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

  /**
   * Sets the deck slot being used for this match. This will apply the hero class of that deck to
   * the match, unless a hero class has already been detected and set.
   * @param value
   */
  //  def deckSlot_=(value: Int) {
  //    _deckSlot = value
  //    if (_userClassUnconfirmed) {
  //      val deck = DeckUtils.getDeckFromSlot(value)
  //      if (deck.isDefined) {
  //        // Set the user class, but it may be overridden later if it's detected to be different
  //        _userClass = deck.get.hero
  //      }
  //    }
  //  }

  val startedAt = DateTime.now
  private def propertyOrUnknown(propertyVal: String): String = {
    if (propertyVal == null) "[undetected]" else propertyVal
  }

  //  override def toString: String =
  //    describeMode + " " +
  //      describeCoin + " " +
  //      describePlayers + " " +
  //      describeResult + " " +
  //      describeDeck + " " +
  //      describeTurns + " "

  //  def describeMode: String = mode match {
  //    case "Arena" => t("match.end.mode.arena")
  //    case "Casual" => t("match.end.mode.casual")
  //    case "Ranked" => t("match.end.mode.ranked", rankLevel)
  //    case "Practice" => t("match.end.mode.practice")
  //    case "Friendly" => t("match.end.mode.friendly")
  //    case _ => "unknown mode"
  //  }
  //
  //  def describeCoin: String = coin match {
  //    case true => t("match.end.coin.true")
  //    case _ => t("match.end.coin.false")
  //  }
  //
  //  def describePlayers: String = opponentName match {
  //    case null => t("match.end.vs.unnamed", propertyOrUnknown(userClass), propertyOrUnknown(opponentClass))
  //    case _ => t("match.end.vs.named", propertyOrUnknown(userClass), propertyOrUnknown(opponentClass), opponentName)
  //  }
  //
  def describeResult: String = result match {
    case Some(r) => r.toString
    case None => "UnknownResult"
  }

  //  def describeDeck: String = mode match {
  //    case "Arena" => ""
  //    case _ => {
  //      val deck = DeckUtils.getDeckFromSlot(deckSlot)
  //      t("match.end.deck.name", deck match {
  //        case Some(d) => d.name
  //        case None => "[unknown]"
  //      })
  //    }
  //  }
  //
  //  def describeTurns: String = t("match.end.turns", numTurns)

  def toJsonObject: JSONObject = {
    val map = collection.mutable.Map(
      "mode" -> mode.get.toString,
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
      (mode.get match {
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
      mode.isDefined

  def editUrl: String =
    s"http://hearthstats.net/constructeds/$id/edit"
}
