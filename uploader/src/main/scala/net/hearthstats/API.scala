package net.hearthstats

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Observable

import net.hearthstats.config.Config

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsJavaMap

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

import grizzled.slf4j.Logging
import net.hearthstats.log.Log

//TODO : replace this JSON implementation with a more typesafe one
object API extends Observable with Logging {

  val DefaultApiBaseUrl: String = "http://hearthstats.net/api/v1/"

  var lastMatchId = -1
  var message = ""

  var config: Config = _

  /**
   * Sets up the API with the given configuration settings. Must be called on startup before the API is used.
   * Ideally API should be refactored to use dependency injection or have a config passed in on construction of an API instance.
   * @param config The configuration to use for the API.
   */
  def setConfig(config: Config) {
    this.config = config
  }

  def endCurrentArenaRun(): Option[ArenaRun] = {
    _get("arena_runs/end") match {
      case None =>
        Log.warn("Error occurred while ending the arena run")
        None
      case Some(resultObj) =>
        val arenaRun = new ArenaRun(resultObj.asInstanceOf[JSONObject])
        _dispatchResultMessage("Ended " + arenaRun.getUserClass + " arena run")
        Some(arenaRun)
    }
  }

  def createArenaRun(arenaRun: ArenaRun): Option[ArenaRun] =
    _post("arena_runs/new", arenaRun.toJsonObject) match {
      case Some(result) =>
        val resultingArenaRun = new ArenaRun(result)
        _dispatchResultMessage(resultingArenaRun.getUserClass + " run created")
        Some(resultingArenaRun)
      case None =>
        Log.warn("Error occurred while creating new arena run")
        None
    }

  def setDeckSlots(slots: Iterable[Option[Int]]): Unit = {
    val data = for ((d, i) <- slots.zipWithIndex)
      yield "slot_" + (i + 1) -> d.getOrElse(null)
    val jsonData = new JSONObject(data.toMap[Any, Any])
    _post("decks/slots", jsonData)
  }

  def createDeck(jsonDeck: JSONObject): Boolean = {
    _post("decks/create", jsonDeck) match {
      case Some(result) =>
        _dispatchResultMessage("Deck was exported to HearthStats.net successfully")
        true
      case None =>
        _dispatchResultMessage("Error occurred while exporting deck to HearthStats.net")
        false
    }
  }

  def createMatch(hsMatch: HearthstoneMatch): Unit = {
    _post("matches/new", hsMatch.toJsonObject) match {
      case Some(result) =>
        try {
          lastMatchId = result.get("id").asInstanceOf[java.lang.Long].toInt
          if (hsMatch.mode != "Arena") {
            _dispatchResultMessage(
              s"Success. <a href='http://hearthstats.net/constructeds/$lastMatchId/edit'>Edit match #$lastMatchId on HearthStats.net</a>")
          } else _dispatchResultMessage("Arena match successfully created")
        } catch {
          case e: Exception => Log.warn("Error occurred while creating new match", e)
        }
      case None => Log.warn("Error occurred while creating new match")
    }
  }

  def getLastArenaRun: ArenaRun =
    _get("arena_runs/show") match {
      case None => null
      case Some(resultObj) =>
        val arenaRun = new ArenaRun(resultObj.asInstanceOf[JSONObject])
        _dispatchResultMessage("Fetched current " + arenaRun.getUserClass + " arena run")
        arenaRun
    }

  //can return JSONObject or JSONArray
  private def _get(method: String): Option[AnyRef] = {
    val baseUrl = config.configApiBaseUrl.get + method + "?userkey="
    debug(s"API get $baseUrl********")
    val url = new URL(baseUrl + config.configUserKey.get)
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
            case e1: Exception => None
          }
        }
      } else {
        _throwError(result.get("message").asInstanceOf[String])
        None
      }
    } catch {
      case ignore: Exception =>
        _throwError("Error parsing reply")
        None
    }

  }

  private def _post(method: String, jsonData: JSONObject): Option[JSONObject] = {
    val baseUrl = config.configApiBaseUrl.get + method + "?userkey="
    debug(s"API post $baseUrl********")
    debug("API post data = " + jsonData.toJSONString)
    val url = new URL(baseUrl + config.configUserKey.get)
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
      _parseResult(resultString).map(_.asInstanceOf[JSONObject])
    } catch {
      case e: Exception => {
        Log.warn(s"Error communicating with HearthStats.net (POST $method)", e)
        _throwError("Error communicating with HearthStats.net")
        None
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
