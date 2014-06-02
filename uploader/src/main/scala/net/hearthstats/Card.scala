package net.hearthstats

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import Card._

object Card {
  val LEGENDARY = 5
  private lazy val imageCacheFolder: String = Config.getImageCacheFolder
}

case class Card(
  id: Int,
  name: String,
  count: Int = 0,
  cost: Int = 0,
  rarity: Int = 0,
  collectible: Boolean = true)
  extends Comparable[Card] {

  def isLegendary: Boolean = rarity == LEGENDARY

  def fileName: String =
    String.format("%s.png", name.replaceAll("[ :']+", "-").replaceAll("""[^a-zA-Z0-9\-]""", "").toLowerCase)

  def localURL: URL = localFile.toURI.toURL

  def localFile: File = new File(imageCacheFolder, fileName)

  def url: String =
    String.format("https://s3-us-west-2.amazonaws.com/hearthstats/cards/%s", fileName)

  override def compareTo(c: Card): Int = {
    val costs = Integer.compare(cost, c.cost)
    if (costs != 0) return costs
    val counts = Integer.compare(count, c.count)
    if (counts != 0) return counts
    name.compareTo(c.name)
  }
}
