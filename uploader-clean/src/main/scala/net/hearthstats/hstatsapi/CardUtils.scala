package net.hearthstats.hstatsapi

import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent._
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._
import java.io.IOException
import net.hearthstats.core.Card
import net.hearthstats.config.Environment
import net.hearthstats.ui.log.Log

class CardUtils(hsAPI: API, uiLog: Log, environment: Environment) {

  lazy val cards: Map[Int, Card] =
    (for {
      json <- hsAPI.getCards
      id = Integer.parseInt(json.get("id").toString)
      cost = Integer.parseInt(json.get("mana").toString)
      rarityString = json.get("rarity_id")
      rarity = if (rarityString == null) 0 else Integer.parseInt(rarityString.toString)
      collectible = json.get("collectible")
    } yield id -> Card(
      rarity = rarity,
      id = id,
      cost = cost,
      originalName = json.get("name").toString,
      collectible = collectible != null && collectible.toString.toBoolean)).toMap

  def withLocalFile(c: Card) =
    c.copy(localFile = Some(
      environment.imageCacheFile(c.originalName)))

  def downloadImages(cards: List[Card]): Future[Unit] = {
    import ExecutionContext.Implicits.global
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
      } else
        uiLog.debug(card.originalName + " already in cache, skipping")
    }
    val all = Future.sequence(futures)

    all.onComplete {
      case Success(_) => uiLog.info("All images downloaded successfully")
      case Failure(e) => uiLog.warn("Could not download an image", e)
    }

    all.map(_ => ())
  }
}
