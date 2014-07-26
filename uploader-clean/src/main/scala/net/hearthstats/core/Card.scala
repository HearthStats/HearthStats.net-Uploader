package net.hearthstats.core

import java.io.File
import java.net.URL

case class Card(
  id: Int,
  originalName: String, //original name in English
  count: Int = 0,
  cost: Int = 0,
  rarity: Int = 0,
  localFile: Option[File] = None,
  localizedName: Option[String] = None, // Some(frenchName) if the game is in French
  collectible: Boolean = true)
  extends Comparable[Card] {

  val LEGENDARY = 5

  def name: String = localizedName match {
    case Some(n) => n
    case None => originalName
  }

  val isLegendary: Boolean = rarity == LEGENDARY

  val dashes = "[ :']+"
  val remove = """[^a-z0-9\-]+"""
  val fileName: String =
    originalName.toLowerCase.replaceAll(dashes, "-").replaceAll(remove, "") + ".png"

  val url: String =
    String.format("https://s3-us-west-2.amazonaws.com/hearthstats/cards/%s", fileName)

  override def compareTo(c: Card): Int = {
    val costs = Integer.compare(cost, c.cost)
    if (costs != 0) return costs
    val counts = Integer.compare(count, c.count)
    if (counts != 0) return counts
    originalName.compareTo(c.originalName)
  }
}
