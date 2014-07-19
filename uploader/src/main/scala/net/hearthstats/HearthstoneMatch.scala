package net.hearthstats

import net.hearthstats.util.Rank
import net.hearthstats.util.Translations.t
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONObject
import scala.collection.JavaConversions._
import net.hearthstats.util.MatchOutcome

//TODO use options
//TODO avoid mutable 
class HearthstoneMatch(var mode: String = null,
  private var _userClass: String = null,
  var opponentClass: String = null,
  var coin: Boolean = false,
  var result: Option[MatchOutcome] = None,
  private var _deckSlot: Int = -1,
  var opponentName: String = null,
  var rankLevel: Rank = null,
  var numTurns: Int = -1,
  var duration: Int = -1,
  var notes: String = null,
  var id: Int = -1,
  var initialized: Boolean = false) {
  private var _userClassUnconfirmed: Boolean = true
  //needed for java calls
  def this() = this(mode = null)

  def deckSlot: Int = _deckSlot

  /**
   * Sets the deck slot being used for this match. This will apply the hero class of that deck to
   * the match, unless a hero class has already been detected and set.
   * @param value
   */
  def deckSlot_=(value: Int) {
    _deckSlot = value
    if (_userClassUnconfirmed) {
      val deck = DeckUtils.getDeckFromSlot(value)
      if (deck.isDefined) {
        // Set the user class, but it may be overridden later if it's detected to be different
        _userClass = deck.get.hero
      }
    }
  }

  def userClass: String = _userClass

  def userClass_=(value: String) {
    _userClassUnconfirmed = value == null
    _userClass = value
  }

  /**
   * Whether the user class is unconfirmed; it may be set based on the selected deck, but if unconfirmed
   * then it hasn't been detected yet. This value is set automatically when you set a userClass on the match.
   *
   * @return true if the user class is unconfirmed and thus may be wrong, false if the class has been detected
   */
  def userClassUnconfirmed: Boolean = _userClassUnconfirmed

  private def propertyOrUnknown(propertyVal: String): String = {
    if (propertyVal == null) "[undetected]" else propertyVal
  }

  override def toString: String =
    describeMode + " " +
      describeCoin + " " +
      describePlayers + " " +
      describeResult + " " +
      describeDeck + " " +
      describeTurns + " "

  def describeMode: String = mode match {
    case "Arena" => t("match.end.mode.arena")
    case "Casual" => t("match.end.mode.casual")
    case "Practice" => "Practice" //will be overriden by a merge soon
    case "Ranked" => t("match.end.mode.ranked", rankLevel)
  }

  def describeCoin: String = coin match {
    case true => t("match.end.coin.true")
    case _ => t("match.end.coin.false")
  }

  def describePlayers: String = opponentName match {
    case null => t("match.end.vs.unnamed", propertyOrUnknown(userClass), propertyOrUnknown(opponentClass))
    case _ => t("match.end.vs.named", propertyOrUnknown(userClass), propertyOrUnknown(opponentClass), opponentName)
  }

  def describeResult: String = result match {
    case Some(r) => r.toString
    case None => "UnknownResult"
  }

  def describeDeck: String = mode match {
    case "Arena" => ""
    case _ => {
      val deck = DeckUtils.getDeckFromSlot(deckSlot)
      t("match.end.deck.name", deck match {
        case Some(d) => d.name
        case None => "[unknown]"
      })
    }
  }

  def describeTurns: String = t("match.end.turns", numTurns)

  def toJsonObject: JSONObject = {
    val map = collection.mutable.Map(
      "mode" -> mode,
      "slot" -> deckSlot,
      "class" -> userClass,
      "oppclass" -> opponentClass,
      "oppname" -> opponentName,
      "coin" -> coin.toString,
      "result" -> describeResult,
      "notes" -> notes,
      "numturns" -> numTurns,
      "duration" -> duration)

    if ("Ranked" == mode) {
      if (Rank.LEGEND == rankLevel) {
        map += "ranklvl" -> 26
        map += "legend" -> "true"
      } else {
        map += "ranklvl" -> rankLevel.number
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
        case "Ranked" => rankLevel != null && deckSlotOk
        case "Casual" => deckSlotOk
        case _ => true
      })

  def deckSlotOk =
    deckSlot >= 1 && deckSlot <= 9

  def mandatoryFieldsOK =
    result != null &&
      userClass != null &&
      opponentClass != null &&
      StringUtils.isNotBlank(opponentName) &&
      StringUtils.isNotBlank(mode)

  def editUrl: String =
    s"http://hearthstats.net/constructeds/$id/edit"
}
