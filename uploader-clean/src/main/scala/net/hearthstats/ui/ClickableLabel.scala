package net.hearthstats.ui

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Color.BLACK
import java.awt.Color.WHITE
import java.awt.Dimension
import java.awt.Font
import java.awt.Font.BOLD
import java.awt.Font.SANS_SERIF
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import scala.swing.Swing.onEDT
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JLabel
import ClickableLabel._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import net.hearthstats.core.Card

class ClickableLabel(card: Card, imagesReady: Future[Unit]) extends JLabel {
  val backgroundSize = new Dimension(218, 35)
  val pictureSize = new Dimension(275, 384)

  def displaySize = getSize()

  var remaining = card.count
  var currentBack = cardBack

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
  val imgDstW = 113
  val imgDstH = 35
  def imgSrcX = 81 * cardImage.getIconWidth / pictureSize.getWidth.toInt
  def imgSrcY: Int = 62 * cardImage.getIconHeight / pictureSize.getHeight.toInt
  def imgSrcW: Int = 130 * cardImage.getIconWidth / pictureSize.getWidth.toInt
  def imgSrcH: Int = 40 * cardImage.getIconHeight / pictureSize.getHeight.toInt

  setMaximumSize(backgroundSize * 2)
  setPreferredSize(backgroundSize * 1)
  setMinimumSize(backgroundSize * 0.5)

  implicit class DimensionOps(d: Dimension) {
    def *(r: Double) = new Dimension((d.getWidth * r).toInt, (d.getHeight * r).toInt)
  }
  setBorder(BorderFactory.createEmptyBorder)

  updateRemaining()

  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      onEDT(handleClick(e.getButton))
    }
  })

  protected override def paintComponent(g: Graphics) {
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
    g2.setFont(Font.decode(SANS_SERIF).deriveFont(BOLD, 18))
    if (card.cost < 10)
      outlineText(g2, cost, 9, 25, BLACK, WHITE)
    else
      outlineText(g2, cost, 5, 25, BLACK, WHITE)
    g2.setFont(Font.decode(SANS_SERIF).deriveFont(14))
    outlineText(g2, name, 35, 23, BLACK, WHITE)
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

  private def updateRemaining() {
    currentBack =
      if (remaining > 1) cardBack2
      else if (card.isLegendary && remaining >= 1) cardBackL
      else cardBack
    repaint()
  }
}

object ClickableLabel {

  val Seq(cardBack, cardBack2, cardBackL) =
    Seq("cardBack", "cardBack2", "cardBackL") map buildImage

  def buildImage(n: String) =
    new ImageIcon(classOf[ClickableLabel].getResource(s"/images/$n.png"))
}