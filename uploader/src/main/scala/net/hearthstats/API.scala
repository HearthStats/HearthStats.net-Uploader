package net.hearthstats

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Observable

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

import grizzled.slf4j.Logging
import net.hearthstats.log.Log

//TODO : replace this JSON implementation with a more typesafe one
object API extends Observable with Logging {
  var lastMatchId = -1
  var message = ""

  def endCurrentArenaRun(): Unit = {
    val resultObj = _get("arena_runs/end").asInstanceOf[JSONObject]
    val arenaRun = if (resultObj == null) null else new ArenaRun(resultObj)
    if (arenaRun != null) _dispatchResultMessage("Ended " + arenaRun.getUserClass + " arena run")
  }

  def createArenaRun(arenaRun: ArenaRun): Unit = {
    val result = _post("arena_runs/new", arenaRun.toJsonObject())
    if (result != null) {
      val resultingArenaRun = new ArenaRun(result)
      _dispatchResultMessage(resultingArenaRun.getUserClass + " run created")
    }
  }

  def setDeckSlots(slots: List[Option[Int]]): Unit = {
    val data = for ((d, i) <- slots.zipWithIndex)
      yield "slot_" + (i + 1) -> d.getOrElse(null)
    val jsonData = new JSONObject(data.toMap[Any, Any])
    _post("decks/slots", jsonData)
  }

  def createMatch(hsMatch: HearthstoneMatch): Unit = {
    val result = _post("matches/new", hsMatch.toJsonObject)
    if (result != null) {
      try {
        lastMatchId = result.get("id").asInstanceOf[java.lang.Long].toInt
      } catch {
        case e: Exception => Log.warn("Error occurred while creating new match", e)
      }
      if (hsMatch.mode != "Arena") {
        val id = result.get("id")
        _dispatchResultMessage(
          s"Success. <a href='http://hearthstats.net/constructeds/$id/edit'>Edit match #$id on HearthStats.net</a>")
      } else _dispatchResultMessage("Arena match successfully created")
    }
  }

  def getLastArenaRun(): ArenaRun = {
    val resultObj = _get("arena_runs/show").asInstanceOf[JSONObject]
    val arenaRun = if (resultObj == null) null else new ArenaRun(resultObj)
    if (arenaRun != null) {
      _dispatchResultMessage("Fetched current " + arenaRun.getUserClass + " arena run")
    }
    arenaRun
  }

  //can return JSONObject or JSONArray
  private def _get(method: String): Option[AnyRef] = {
    val baseUrl = Config.getApiBaseUrl + method + "?userkey="
    debug(s"API get $baseUrl********")
    val url = new URL(baseUrl + _getKey)
    try {
      val resultString = io.Source.fromURL(url, "UTF-8").getLines.mkString("\n")
      debug(s"API get result = $resultString")
      _parseResult(resultString)
    } catch {
      case e: IOException =>
        Log.warn("Error communicating with HearthStats.net (GET " + method + ")", e)
        _throwError("Error communicating with HearthStats.net")
        None
    }

  }

  //can return JSONObject or JSONArray
  private def _parseResult(resultString: String): Option[AnyRef] = {
    val parser = new JSONParser
    try {
      val result = parser.parse(resultString).asInstanceOf[JSONObject]
      if (result.get("status").toString.matches("success")) {
        try {
          message = result.get("message").toString
        } catch {
          case e: Exception => message = null
        }
        try {
          Some(result.get("data").asInstanceOf[JSONObject])
        } catch {
          case e: Exception => try {
            Some(result.get("data").asInstanceOf[JSONArray])
          } catch {
            case e1: Exception => null
          }
        }
      } else {
        _throwError(result.get("message").asInstanceOf[String])
        null
      }
    } catch {
      case ignore: Exception =>
        _throwError("Error parsing reply")
        None
    }

  }

  private def _post(method: String, jsonData: JSONObject): JSONObject = {
    val baseUrl = Config.getApiBaseUrl + method + "?userkey="
    debug(s"API post $baseUrl********")
    debug("API post data = " + jsonData.toJSONString)
    val url = new URL(baseUrl + _getKey)
    try {
      val httpcon = (url.openConnection()).asInstanceOf[HttpURLConnection]
      httpcon.setDoOutput(true)
      httpcon.setRequestProperty("Content-Type", "application/json")
      httpcon.setRequestProperty("Accept", "application/json")
      httpcon.setRequestMethod("POST")
      httpcon.connect()
      val outputBytes = jsonData.toJSONString.getBytes("UTF-8")
      val os = httpcon.getOutputStream
      os.write(outputBytes)
      os.close()
      val resultString = io.Source.fromInputStream(httpcon.getInputStream).getLines.mkString("\n")
      debug("API post result = " + resultString)
      _parseResult(resultString).asInstanceOf[JSONObject]
    } catch {
      case e: Exception => {
        Log.warn(s"Error communicating with HearthStats.net (POST $method)", e)
        _throwError("Error communicating with HearthStats.net")
        null
      }
    }
  }

  def getCards: List[JSONObject] =
    transformResult(_get("cards"), "Fetched all cards data from HearthStats.net")

  def getDecks: List[JSONObject] =
    transformResult(_get("decks/show"), "Fetched your decks from HearthStats.net")

  private def transformResult(result: Option[_], message: String) = result match {
    case Some(res) =>
      _dispatchResultMessage(message)
      res.asInstanceOf[java.util.List[_]].map(_.asInstanceOf[JSONObject]).toList
    case None => List.empty
  }

  private def _getKey: String = Config.getUserKey

  private def _dispatchResultMessage(m: String) {
    message = m
    setChanged()
    notifyObservers("result")
  }

  private def _throwError(m: String) {
    message = m
    setChanged()
    notifyObservers("error")
  }
}
