package net.hearthstats.ui.deckoverlay

import java.awt.{ AlphaComposite, Color }
import java.awt.{ Dimension, Font }
import java.awt.{ Graphics, Graphics2D }
import java.awt.Color.{ BLACK, WHITE }
import java.awt.Font.{ BOLD, SANS_SERIF }
import java.awt.event.{ MouseAdapter, MouseEvent }
import java.awt.geom.AffineTransform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Swing.onEDT

import javax.swing.{ BorderFactory, ImageIcon, JLabel }
import net.hearthstats.core.Card

class ClickableLabel(card: Card, imagesReady: Future[Unit]) extends JLabel {
  import ClickableLabel._

  val backgroundSize = new Dimension(218 + 40, 35)
  val pictureSize = new Dimension(275, 384)

  def displaySize = getSize()

  var remaining = card.count
  var currentBack = cardBack
  var initialPosibilities = 100*(math floor(remaining.toDouble / 30.0d)*10000)/10000
  var posibilities = initialPosibilities
  var cardsLeft = 30
  
 // val newPosibilities:Future[Double] = Future{
 //   posibilities
 // }
  
  
  imagesReady.onSuccess {
    case _ =>
      cardImage = new ImageIcon(card.localFile.get.getAbsolutePath)
      revalidate()
      repaint()
  }
  var cardImage = cardBack

  val cost = card.cost.toString
  val name = card.name
  val imgDstX = 100
  val imgDstY = 0
  val imgDstW = 113 + 20
  val imgDstH = 35
  def imgSrcX = 81 * cardImage.getIconWidth / pictureSize.getWidth.toInt
  def imgSrcY: Int = 82 * cardImage.getIconHeight / pictureSize.getHeight.toInt
  def imgSrcW: Int = 130 * cardImage.getIconWidth / pictureSize.getWidth.toInt
  def imgSrcH: Int = 40 * cardImage.getIconHeight / pictureSize.getHeight.toInt
 
  setMaximumSize(backgroundSize * 1.2)
  setPreferredSize(backgroundSize)
  setMinimumSize(backgroundSize)

  implicit class DimensionOps(d: Dimension) {
    def *(r: Double) = new Dimension((d.getWidth * r).toInt, (d.getHeight * r).toInt)
  }
  setBorder(BorderFactory.createEmptyBorder)

  updateRemaining()
  
  val src = card.localFile.get.toURI.toURL
  setToolTipText(s"""<html><img src="$src"> $name""")

  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      onEDT(handleClick(e.getButton))
    }
  })

  protected override def paintComponent(g: Graphics): Unit = {
    val g2 = g.asInstanceOf[Graphics2D]
    val original = g2.getTransform
    val composite = g2.getComposite
    val pictureScale = displaySize.getWidth / pictureSize.getWidth
    g2.transform(AffineTransform.getScaleInstance(
      displaySize.getWidth / backgroundSize.getWidth,
      displaySize.getHeight / backgroundSize.getHeight))
    if (remaining < 1) {
      g2.setColor(BLACK)
      g2.fillRoundRect(0, 0, currentBack.getIconWidth, currentBack.getIconHeight, 15, 15)
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f))
    }
    g2.drawImage(cardImage.getImage, imgDstX, imgDstY, imgDstX + imgDstW, imgDstY + imgDstH, imgSrcX, imgSrcY, imgSrcX + imgSrcW, imgSrcY + imgSrcH, null)
    g2.drawImage(currentBack.getImage, 0, 0, null)
    g2.drawRect(imgDstX + imgDstW - 15, imgDstY, 45, imgDstH )
    g2.fillRect(imgDstX + imgDstW - 17, imgDstY, 45, imgDstH)
    g2.setFont(Font.decode(SANS_SERIF).deriveFont(BOLD, 18))
    if (card.cost < 10)
      outlineText(g2, cost, 9, 25, BLACK, WHITE)
    else
      outlineText(g2, cost, 5, 25, BLACK, WHITE)
    g2.setFont(Font.decode(SANS_SERIF).deriveFont(14))
    outlineText(g2, name, 35, 23, BLACK, WHITE)
    outlineText(g2, posibilities + "%", 222,25,BLACK, WHITE)
    g2.setTransform(original)
    g2.setComposite(composite)

    
    super.paintComponent(g2)
  }

  private def outlineText(g: Graphics, s: String, posX: Int, posY: Int, borderColor: Color, fontColor: Color): Unit = {
    g.setColor(borderColor)
    g.drawString(s, posX - 1, posY - 1)
    g.drawString(s, posX - 1, posY + 1)
    g.drawString(s, posX + 1, posY - 1)
    g.drawString(s, posX + 1, posY + 1)
    g.setColor(fontColor)
    g.drawString(s, posX, posY)
  }

  private def handleClick(button: Int): Unit =
    if (button == MouseEvent.BUTTON1 && remaining > 0)
      decreaseRemaining()
    else if (button != MouseEvent.BUTTON1 && remaining < card.count)
      increaseRemaining()

  def decreaseRemaining(): Unit = {
    remaining -= 1
    updateRemaining()
  }

  def increaseRemaining(): Unit = {
    remaining += 1
    updateRemaining()
  }
  
  def updateIncreaseCardsLeft():Unit = {
    cardsLeft += 1
    println(card.originalName + "cardsLeft increased")
    updatePosibilities()
  }
  
  def updateDecreaseCardsLeft():Unit = {
    cardsLeft -= 1
    println("updateDecreasedCardsLeft: " + cardsLeft)
    updatePosibilities()
  }
  
  def reset(): Unit = {
    remaining = card.count
    cardsLeft = 30
    updatePosibilities()
    updateRemaining()
  }
  
  
  
  def updatePosibilities(): Unit = {
    posibilities = calPosibilities()
    repaint()
  }

  private def updateRemaining(): Unit = {
    currentBack =
      if (remaining > 1) cardBack2
      else if (card.isLegendary && remaining >= 1) cardBackL
      else cardBack
    repaint()
  }
  
    def calPosibilities(): Double = 
      100*(math floor(remaining.toDouble/cardsLeft.toDouble)*10000)/10000

}

object ClickableLabel {

  val Seq(cardBack, cardBack2, cardBackL) =
    Seq("cardBack", "cardBack2", "cardBackL") map buildImage

  def buildImage(n: String) =
    new ImageIcon(classOf[ClickableLabel].getResource(s"/images/$n.png"))
}
