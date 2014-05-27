package net.hearthstats

import java.io.IOException
import net.hearthstats.log.Log
import org.json.simple.JSONObject
//remove if not needed
import scala.collection.JavaConversions._

object CardUtils {
  private lazy val api: API = new API

  lazy val cards: Map[Int, Card] =
    (for {
      json <- api.getCards
      id = Integer.parseInt(json.get("id").toString)
      cost = Integer.parseInt(json.get("mana").toString)
      rarityString = json.get("rarity_id")
      rarity = if (rarityString == null) 0 else Integer.parseInt(rarityString.toString)
    } yield id -> Card(
      rarity = rarity,
      id = id,
      cost = cost,
      name = json.get("name").toString)).toMap

}
