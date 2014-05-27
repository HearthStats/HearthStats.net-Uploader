package net.hearthstats

import net.hearthstats.util.Rank
import org.apache.commons.lang3.StringUtils
import org.json.simple.JSONObject
//remove if not needed
import scala.collection.JavaConversions._

//TODO use options
//TODO avoid mutable 
class HearthstoneMatch(var mode: String = null,
  var userClass: String = null,
  var opponentClass: String = null,
  var coin: Boolean = false,
  var result: String = null,
  var deckSlot: Int = -1,
  var opponentName: String = null,
  var rankLevel: Rank = null,
  var numTurns: Int = -1,
  var duration: Int = -1,
  var notes: String = null,
  var id: Int = -1) {
  //needed for java calls
  def this() = this(mode = null)

  private def _propertyOrUnknown(propertyVal: String): String = {
    if (propertyVal == null) "[undetected]" else propertyVal
  }

  override def toString: String = {
    val deck = DeckUtils.getDeckFromSlot(deckSlot)
    _propertyOrUnknown(mode) +
      (if ("Ranked" == mode) " level " + rankLevel else "") +
      " " +
      (if (coin) "" else "no ") +
      "coin " +
      _propertyOrUnknown(userClass) +
      " vs. " +
      _propertyOrUnknown(opponentClass) +
      " " +
      "(" +
      _propertyOrUnknown(opponentName) +
      ") " +
      result +
      " " +
      (if (deck == null) "" else " deck: " + deck.name) +
      " (" +
      numTurns +
      " turns)"
  }

  def toJsonObject: JSONObject = {
    val r = result match {
      case "Victory" | "Win" => "Win"
      case "Defeat" | "Loss" => "Loss"
      case "Draw" => "Draw"
    }

    val map = collection.mutable.Map(
      "mode" -> mode,
      "slot" -> deckSlot,
      "class" -> userClass,
      "oppclass" -> opponentClass,
      "oppname" -> opponentName,
      "coin" -> coin.toString,
      "result" -> result,
      "notes" -> notes,
      "numturns" -> numTurns,
      "duration" -> duration)

    val (lvl, legend) =
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
      StringUtils.isNotBlank(opponentName)

  def editUrl: String =
    s"http://hearthstats.net/constructeds/$id/edit"
}
