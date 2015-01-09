package net.hearthstats.hstatsapi

import java.io.{ File, FileOutputStream, IOException }
import java.net.URL
import java.nio.channels.Channels
import grizzled.slf4j.Logging
import net.hearthstats.config.Environment
import net.hearthstats.ui.log.Log
import scala.concurrent._
import scala.util.{ Failure, Success }
import net.hearthstats.core.CardData
import net.hearthstats.core.Card
import rapture.json._
import rapture.json.jsonBackends.jawn._

class CardUtils(hsAPI: API, uiLog: Log, environment: Environment) extends Logging {

  def byName(n: String): Option[Card] = cards.values.find(_.name == n)

  def byCode(code: String): Option[Card] =
    if (code == "") None
    else byName(CardData.byId(code).name)

  lazy val cards: Map[Int, Card] = {
    val cardsList = hsAPI.get("cards").get.as[List[Json]]
    (for {
      card <- cardsList
      json""" { 
      	"id" = $id, 
      	"mana" = $cost, 
      	"type_id" = $typeId, 
      	"rarity_id" = $rarity, 
      	"collectible" = $collectible,
      	"name" = $originalName
      	} """ = card
      idInt = id.as[Int]
    } yield idInt -> Card(
      rarity = rarity.as[Int],
      id = idInt,
      cost = cost.as[Int],
      typeId = typeId.as[Int],
      originalName = originalName.as[String],
      collectible = collectible.as[Boolean])).toMap
  }

  def withLocalFile(c: Card) =
    c.copy(localFile = Some(
      environment.imageCacheFile(c.originalName)))

  def downloadImages(cards: List[Card]): Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    (new File(environment.imageCacheFolder)).mkdirs()
    val futures = for (card <- cards) yield Future {
      val file = card.localFile getOrElse environment.imageCacheFile(card.originalName)
      if (file.length < 30000) {
        val fos = new FileOutputStream(file)
        try {
          val channel = Channels.newChannel(new URL(card.url).openStream)
          fos.getChannel.transferFrom(channel, 0, Long.MaxValue)
          fos.close()
          channel.close()
          uiLog.debug(card.originalName + " saved to cache folder")
        } catch {
          case e: IOException => uiLog.warn(s"Could not download $card", e)
        }
      } else {
        debug(card.originalName + " already in cache, skipping")
      }
    }
    val all = Future.sequence(futures)

    all.onComplete {
      case Success(_) => debug("All images downloaded successfully (if any were missing)")
      case Failure(e) => uiLog.warn("Could not download an image", e)
    }

    all.map(_ => ())
  }
}
