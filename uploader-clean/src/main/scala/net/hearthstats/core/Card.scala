package net.hearthstats.core

import java.io.File
import java.net.URL
import net.hearthstats.util.Translation

case class Card(
  id: Int,
  originalName: String,
  count: Int = 0,
  cost: Int = 0,
  rarity: Int = 0,
  collectible: Boolean = true)
  extends Comparable[Card] {

  val LEGENDARY = 5

  def name(implicit translation: Translation): String =
    if (translation.has(id.toString))
      translation.t(id.toString)
    else
      originalName

  val isLegendary: Boolean = rarity == LEGENDARY

  val dashes = "[ :']+"
  val remove = """[^a-z0-9\-]+"""
  val fileName: String =
    originalName.toLowerCase.replaceAll(dashes, "-").replaceAll(remove, "") + ".png"

  //  val localFile: File = new File(imageCacheFolder, fileName)
  //  val localURL: URL = localFile.toURI.toURL

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
