package net.hearthstats.analysis

import java.awt.image.BufferedImage

import grizzled.slf4j.Logging
import net.hearthstats.analysis.DeckAnalyser.CardsVisibleOnScreen
import net.hearthstats.ocr.{DeckCardOcr, DeckNameOcr}
import net.hearthstats.state.UniquePixel
import net.hearthstats.state.UniquePixel._
import net.hearthstats.util.Coordinate
import net.hearthstats.{Card, CardUtils, Deck}
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ListBuffer

/**
 * Analyses screenshots of the deck screen in Hearthstone and returns a deck object.
 *
 * @author gtch
 */
class DeckAnalyser(val imgWidth: Int, val imgHeight: Int) extends CoordinateCacheBase with Logging {

  val ocr = new DeckCardOcr
  val individualPixelAnalyser = new IndividualPixelAnalyser

  // Create a map with card name as the key, ID as the value
  val cardList = CardUtils.cards.values


  def identifyDeck(img1: BufferedImage, img2: BufferedImage): Option[Deck] = {
    if (img1.getWidth != imgWidth || img1.getHeight() != imgHeight) throw new RuntimeException("Image 1 is not the expected size")
    if (img2.getWidth != imgWidth || img2.getHeight() != imgHeight) throw new RuntimeException("Image 2 is not the expected size")

    // Make a list of cards found on each screen; there may be some overlap
    val cards1 = identifyCards(img1)
    val cards2 = identifyCards(img2)

    if (cards1.size < 3 || cards2.size < 3) {
      // One of the images contained hardly any cards, which suggests the screen capture failed. This can happen if the image
      // was captured during a screen refreshes. Return None to indicate that a new screen capture is needed.
      debug(s"Deck identification failed because cards were not detected on a screen (img1 = ${cards1.size}, img2 = ${cards2.size})")
      return None
    }

    // Combine the two lists, using the card with the larger count if the same card appears in both lists
    val cards = (cards1 ++ cards2).groupBy(_.id).map(c => c._2.head.copy(count = c._2.foldLeft(0)((i,s) => i max s.count))).toList

    cards.foreach(c => debug(s"Identified card ${c.count} ${c.name}"))

    // Determine the deck name, checking both images and returning the longest name if they're not the same
    val ocr = new DeckNameOcr()
    val deckName1 = ocr.process(img1)
    val deckName2 = ocr.process(img2)
    var deckName = List(deckName1, deckName2).maxBy(_.length)

    val heroClass = identifyClass(img1) match {
      case Some(str) => str
      case None => ""
    }

    Some(new Deck(cards = cards, name = deckName, hero = heroClass))
  }



  private def identifyCards(img: BufferedImage): List[Card] = {
    val cards = new ListBuffer[Card]


    for (i <- 0 until CardsVisibleOnScreen) {

      val cardImg = extractCardImage(img, i)

      val roughName = ocr.process(cardImg);

      val card = identifyCard(roughName)
      card match {
        case Some(c) => {
          debug(s"Card $i: $roughName matched ${c.originalName}")
          cards.append(c.copy(count = identifyCount(img, i)))
        }
        case _ => debug(s"Card $i: $roughName DID NOT MATCH")
      }
    }

    // Group multiple cards together (golden and normal cards would otherwise be separate in the list)
    cards.groupBy(_.id).map(c => c._2.head.copy(count = c._2.foldLeft(0)((i,s) => i + s.count))).toList
  }


  private def extractCardImage(img: BufferedImage, cardNo: Int): BufferedImage = {
    val yoffset = (133f + (45.5f * cardNo.toFloat)).toInt

    val topLeft = getCoordinate(1282, yoffset)
    val bottomRight = getCoordinate(1470, yoffset + 24)

    debug(s"Extracting ${topLeft.x}x${topLeft.y} to ${bottomRight.x}x${bottomRight.y}")

    img.getSubimage(topLeft.x, topLeft.y,
      bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
  }


  private def identifyCard(roughName: String): Option[Card] = {
    val bestCard = cardList.maxBy(c => StringUtils.getJaroWinklerDistance(roughName, c.name))

    // Only return the card if the distance score is higher than 0.6; lower scores mean the card wasn't found
    if (StringUtils.getJaroWinklerDistance(roughName, bestCard.name) > 0.6) {
      Some(bestCard)
    } else {
      None
    }
  }


  private def identifyCount(img: BufferedImage, cardNo: Int): Int = {
    val yoffset = (45.5f * cardNo.toFloat).toInt

    // Look for a yellow number two in various offset positions
    if (isCountTwo(img, yoffset) || isCountTwo(img, yoffset + 2) || isCountTwo(img, yoffset + 4)) {
      2
    } else {
      1
    }
  }


  private def isCountTwo(img: BufferedImage, offset: Int): Boolean = {
    checkPixelIsYellow(img, 1492, 150 + offset) &&
      checkPixelIsYellow(img, 1490, 142 + offset) &&
      checkPixelIsYellow(img, 1497, 141 + offset) &&
      !checkPixelIsYellow(img, 1486, 137 + offset) &&
      !checkPixelIsYellow(img, 1501, 148 + offset)
  }


  private def checkPixelIsYellow(img: BufferedImage, x: Int, y: Int): Boolean = {
    val pixel = getCoordinate(x, y)
    val rgb = img.getRGB(pixel.x, pixel.y)
    val red = (rgb >> 16) & 0xFF
    val green = (rgb >> 8) & 0xFF
    val blue = (rgb & 0xFF)

    // debug(s"Pixel $x,$y: $red,$green,$blue yellow=$yellow")
    red > 166 && red < 230 && green > 148 && green < 210 && blue < 20
  }


  private def identifyClassPixel(image: BufferedImage, pixelRules: Iterable[(Array[UniquePixel], String)]): Option[String] =
    (for {
      (pixels, result) <- pixelRules
      if individualPixelAnalyser.testAllPixelsMatch(image, pixels)
    } yield result).headOption


  private def identifyClass(img: BufferedImage): Option[String] =
    identifyClassPixel(img, Seq(
      Array(DECK_DRUID_1, DECK_DRUID_2) -> "Druid",
      Array(DECK_HUNTER_1, DECK_HUNTER_2) -> "Hunter",
      Array(DECK_MAGE_1, DECK_MAGE_2) -> "Mage",
      Array(DECK_PALADIN_1, DECK_PALADIN_2) -> "Paladin",
      Array(DECK_PRIEST_1, DECK_PRIEST_2) -> "Priest",
      Array(DECK_ROGUE_1, DECK_ROGUE_2) -> "Rogue",
      Array(DECK_SHAMAN_1, DECK_SHAMAN_2) -> "Shaman",
      Array(DECK_WARLOCK_1, DECK_WARLOCK_2) -> "Warlock",
      Array(DECK_WARRIOR_1, DECK_WARRIOR_2) -> "Warrior")
    )


  /**
   * Calculates the correct pixel position for a given coordinate at the current image size.
   *
   * @param x The X position, relative to a 1600px width screen
   * @param y The Y position, relative to a 1200px high screen
   * @return The given coordinate converted into the current image size.
   */
  private def getCoordinate(x: Int, y: Int): Coordinate = {
    val upi: CoordinateCacheBase.UniquePixelIdentifier = new CoordinateCacheBase.UniquePixelIdentifier(x, y, imgWidth, imgHeight)
    getCachedCoordinate(upi)
  }

}

object DeckAnalyser {

  val CardsVisibleOnScreen = 21

}