package net.hearthstats

import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent._
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._

import net.hearthstats.log.Log

object CardUtils {
  private lazy val api: API = new API

  lazy val cards: Map[Int, Card] =
    (for {
      json <- api.getCards
      id = Integer.parseInt(json.get("id").toString)
      cost = Integer.parseInt(json.get("mana").toString)
      rarityString = json.get("rarity_id")
      rarity = if (rarityString == null) 0 else Integer.parseInt(rarityString.toString)
      collectible = json.get("collectible")
    } yield id -> Card(
      rarity = rarity,
      id = id,
      cost = cost,
      name = json.get("name").toString,
      collectible = collectible != null && collectible.toString.toBoolean)).toMap

  def downloadImages(cards: List[Card]) {
    import ExecutionContext.Implicits.global
    val futures = for (card <- cards) yield future {
      val rbc = Channels.newChannel(new URL(card.url).openStream)
      val file = card.localFile
      if (file.length < 30000) {
        val fos = new FileOutputStream(file)
        fos.getChannel.transferFrom(rbc, 0, Long.MaxValue)
        fos.close()
        rbc.close()
        Log.debug(card.name + " saved to cache folder")
      } else
        Log.debug(card.name + " already in cache, skipping")
    }
    val all = Future.sequence(futures)
    all.onComplete {
      case Success(_) => Log.info("all images downloaded successfully")
      case Failure(e) => Log.warn("could not download an image", e)
    }
    Await.result(all, 10.seconds)
  }

}
