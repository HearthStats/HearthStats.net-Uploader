package net.hearthstats.analysis

import java.awt.image.BufferedImage

import grizzled.slf4j.Logging
import net.hearthstats.analysis.DeckAnalyser.CardsVisibleOnScreen
import net.hearthstats.ocr.{DeckCardOcr, DeckNameOcr}
import net.hearthstats.util.Coordinate
import net.hearthstats.{BackgroundImageSave, Card, CardUtils, Deck}
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ListBuffer

/**
 * Analyses screenshots of the deck screen in Hearthstone and returns a deck object.
 *
 * @author gtch
 */
class DeckAnalyser(val imgWidth: Int, val imgHeight: Int) extends CoordinateCacheBase with Logging {

  val ocr = new DeckCardOcr

  // Create a map with card name as the key, ID as the value
  val cardList = CardUtils.cards.values


  def identifyDeck(img1: BufferedImage, img2: BufferedImage): Option[Deck] = {
    if (img1.getWidth != imgWidth || img1.getHeight() != imgHeight) throw new RuntimeException("Image 1 is not the expected size")
    if (img2.getWidth != imgWidth || img2.getHeight() != imgHeight) throw new RuntimeException("Image 2 is not the expected size")

    BackgroundImageSave.savePngImage(img1, "de-img1")
    BackgroundImageSave.savePngImage(img2, "de-img2")

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

    Some(new Deck(cards = cards, name = deckName))
  }



  private def identifyCards(img: BufferedImage): List[Card] = {
    val cards = new ListBuffer[Card]


    for (i <- 0 until CardsVisibleOnScreen) {

      val cardImg = extractCardImage(img, i)

//      BackgroundImageSave.savePngImage(cardImg, "card-" + i)

      ocr.setCardNo(i)
      val roughName = ocr.process(cardImg);

//      val countImg = extractCountImage(img, i)
//      BackgroundImageSave.savePngImage(countImg, "-count-" + i)

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


//  private def extractCountImage(img: BufferedImage, cardNo: Int): BufferedImage = {
//    val yoffset = (135f + (45.5f * cardNo.toFloat)).toInt
//
//    val topLeft = getCoordinate(1484, yoffset)
//    val bottomRight = getCoordinate(1503, yoffset + 22)
//
//    debug(s"Extracting ${topLeft.x}x${topLeft.y} to ${bottomRight.x}x${bottomRight.y}")
//
//    img.getSubimage(topLeft.x, topLeft.y,
//      bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
//  }
//
//
  private def identifyCard(roughName: String): Option[Card] = {
    val bestCard = cardList.maxBy(c => StringUtils.getJaroWinklerDistance(roughName, c.name))

    // Only return the card if the distance score is higher than 0.7; lower scores mean the card wasn't found
    if (StringUtils.getJaroWinklerDistance(roughName, bestCard.name) > 0.7) {
      Some(bestCard)
    } else {
      None
    }
  }

  private def identifyCount(img: BufferedImage, cardNo: Int): Int = {
    val yoffset = (45.5f * cardNo.toFloat).toInt

    if (checkPixelIsYellow(img, 1492, 151 + yoffset) &&
        checkPixelIsYellow(img, 1489, 144 + yoffset) &&
        checkPixelIsYellow(img, 1497, 142 + yoffset) &&
        !checkPixelIsYellow(img, 1486, 138 + yoffset) &&
        !checkPixelIsYellow(img, 1501, 149 + yoffset)) {
      // This pixels match a yellow number two
      2
    } else {
      1
    }
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