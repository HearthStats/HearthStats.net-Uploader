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
import java.io.IOException

object CardUtils {

  lazy val cards: Map[Int, Card] =
    (for {
      json <- API.getCards
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

  def downloadImages(cards: List[Card]): Future[Unit] = {
    import ExecutionContext.Implicits.global
    val futures = for (card <- cards) yield future {
      val file = card.localFile
      if (file.length < 30000) {
        val fos = new FileOutputStream(file)
        try {
          val channel = Channels.newChannel(new URL(card.url).openStream)
          fos.getChannel.transferFrom(channel, 0, Long.MaxValue)
          fos.close()
          channel.close()
          Log.debug(card.name + " saved to cache folder")
        } catch {
          case e: IOException => Log.warn(s"Could not download $card", e)
        }
      } else
        Log.debug(card.name + " already in cache, skipping")
    }
    val all = Future.sequence(futures)

    all.onComplete {
      case Success(_) => Log.info("All images downloaded successfully")
      case Failure(e) => Log.warn("Could not download an image", e)
    }

    all.map(_ => ())
  }
}
