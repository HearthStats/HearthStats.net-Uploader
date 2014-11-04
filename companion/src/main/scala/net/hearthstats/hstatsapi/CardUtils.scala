package net.hearthstats.hstatsapi

import java.io.{ File, FileOutputStream, IOException }
import java.net.URL
import java.nio.channels.Channels

import grizzled.slf4j.Logging
import net.hearthstats.config.Environment
import net.hearthstats.core.Card
import net.hearthstats.ui.log.Log

import scala.concurrent._
import scala.util.{ Failure, Success }

class CardUtils(hsAPI: API, uiLog: Log, environment: Environment) extends Logging {

  def byName(n: String): Option[Card] = cards.values.find(_.name == n)

  lazy val cards: Map[Int, Card] =
    (for {
      json <- hsAPI.getCards
      id = Integer.parseInt(json.get("id").toString)
      cost = Integer.parseInt(json.get("mana").toString)
      typeId = Integer.parseInt(json.get("type_id").toString)
      rarityString = json.get("rarity_id")
      rarity = if (rarityString == null) 0 else Integer.parseInt(rarityString.toString)
      collectible = json.get("collectible")
    } yield id -> Card(
      rarity = rarity,
      id = id,
      cost = cost,
      typeId = typeId,
      originalName = json.get("name").toString,
      collectible = collectible != null && collectible.toString.toBoolean)).toMap

  def withLocalFile(c: Card) =
    c.copy(localFile = Some(
      environment.imageCacheFile(c.originalName)))

  def downloadImages(cards: List[Card]): Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    (new File(environment.imageCacheFolder)).mkdirs()
    val futures = for (card <- cards) yield future {
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
