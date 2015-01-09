package net.hearthstats.hstatsapi

import java.io.IOException
import java.net.{ HttpURLConnection, URL }
import grizzled.slf4j.Logging
import net.hearthstats.config.UserConfig
import net.hearthstats.core.{ ArenaRun, HearthstoneMatch }
import scala.collection.JavaConversions.{ asScalaBuffer, mapAsJavaMap }
import rapture.json._
import rapture.json.jsonBackends.jawn._
import scala.util._
import net.hearthstats.core.ArenaRun
import net.hearthstats.core.CreateArenaRun
import net.hearthstats.core.Card
import net.hearthstats.core.Deck

class API(config: UserConfig) extends Logging {

  var lastMatchId = -1
  var message = ""

  lazy val awsKeys: Seq[String] = get("users/premium") match {
    case Success(json""" { "aws_access_key": $access, "aws_secret_key": $secret }""") =>
      Seq(access.as[String], secret.as[String])
    case _ =>
      info("You are not a premium user")
      Nil
  }

  lazy val premiumUserId: Option[String] = get("users/premium") match {
    case Success(data) =>
      Some(data.user.id.as[String])
    case _ =>
      info("You are not a premium user")
      None
  }

  def endCurrentArenaRun(): Try[ArenaRun] =
    for (data <- get("arena_runs/end")) yield {
      val arenaRun = data.as[ArenaRun]
      info("Ended " + arenaRun.userclass + " arena run")
      arenaRun
    }

  def createArenaRun(arenaRun: CreateArenaRun): Try[ArenaRun] =
    for (data <- post("arena_runs/new", Json(arenaRun))) yield {
      val arenaRun = data.as[ArenaRun]
      info(arenaRun.userclass + " arena run created")
      arenaRun
    }

  def setDeckSlots(slots: Iterable[Option[Int]]): Unit = {
    val data = (for ((d, i) <- slots.zipWithIndex)
      yield "slot_" + (i + 1) -> d).toMap
    _post("decks/slots", Json(data))
  }

  def createDeck(jsonDeck: Json): Try[String] =
    _post("decks/create", jsonDeck)

  /**
   * Returns the matchId if created OK.
   */
  def createMatch(hsMatch: HearthstoneMatch): Try[Int] = for {
    result <- post("matches/new", hsMatch.toJsonObject)
  } yield result.id.as[Long].toInt

  def getLastArenaRun: Try[ArenaRun] =
    for (res <- get("arena_runs/show"))
      yield res.as[ArenaRun]

  def get(method: String): Try[Json] = for {
    resString <- call(method)
    data <- _parseResult(resString)
  } yield data

  private def call(method: String): Try[String] = Try {
    val inputStream = buildConnection(method).getInputStream
    val resultString = io.Source.fromInputStream(inputStream)("UTF-8").getLines.mkString
    info(s"API get result = $resultString")
    inputStream.close()
    resultString
  }

  def buildConnection(method: String): HttpURLConnection = {
    val baseUrl = config.apiBaseUrl + method + "?userkey="
    debug(s"API get $baseUrl********")
    val timeout = config.apiTimeoutMs
    val url = new URL(baseUrl + config.userKey.get)
    val conn = url.openConnection()
    conn.setConnectTimeout(timeout)
    conn.setReadTimeout(timeout)
    conn.asInstanceOf[HttpURLConnection]
  }

  private def _parseResult(resultString: String): Try[Json] = {
    val result = Json(resultString)
    if (result.status.as[String] == "success")
      Success(result.data)
    else {
      val message = result.message.as[String]
      Failure(new Exception(s"API error : $message"))
    }
  }

  private def post(method: String, jsonData: Json): Try[Json] = {
    for {
      resString <- _post(method, jsonData)
      data <- _parseResult(resString)
    } yield data
  }

  private def _post(method: String, jsonData: Json): Try[String] = Try {
    val httpcon = buildConnection(method)
    httpcon.setDoOutput(true)
    httpcon.setRequestProperty("Content-Type", "application/json")
    httpcon.setRequestProperty("Accept", "application/json")
    httpcon.setRequestMethod("POST")
    httpcon.connect()
    val outputBytes = jsonData.toString.getBytes("UTF-8")
    val os = httpcon.getOutputStream
    os.write(outputBytes)
    os.close()
    val resultString = io.Source.fromInputStream(httpcon.getInputStream).getLines.mkString("\n")
    info("API post result = " + resultString)
    resultString
  }

}
