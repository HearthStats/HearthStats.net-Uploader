package net.hearthstats.ui

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.NoninvertibleTransformException
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.SwingUtilities
import net.hearthstats.Card
import ClickableLabel._
import scala.collection.JavaConversions._
import scala.swing.Swing._
import javax.swing.BorderFactory

class ClickableLabel(var card: Card) extends JLabel {
  val backgroundSize = new Dimension(218, 35)
  val pictureSize = new Dimension(275, 384)
  var displaySize = new Dimension((218 * 1.5).toInt, (1.5 * 35).toInt)

  var remaining = card.count
  private var cardImage = new ImageIcon(card.localURL)
  private var currentBack = cardBack

  setPreferredSize(displaySize)
  setMaximumSize(displaySize)
  setMinimumSize(displaySize)

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
    g2.transform(AffineTransform.getScaleInstance(pictureScale, pictureScale))
    g2.drawImage(cardImage.getImage, 0, -(pictureSize.getHeight * .2).toInt, null)
    g2.setTransform(original)
    g2.transform(AffineTransform.getScaleInstance(
      displaySize.getWidth / backgroundSize.getWidth,
      displaySize.getHeight / backgroundSize.getHeight))
    g2.drawImage(currentBack.getImage, 0, 0, null)
    g2.setFont(Font.decode(Font.SANS_SERIF).deriveFont(Font.BOLD, 14))
    g2.setColor(Color.WHITE)
    g2.drawString(card.name, 35, 25)
    if (card.cost > 9)
      g2.drawString(card.cost.toString, 7, 25)
    else
      g2.drawString(card.cost.toString, 9, 25)
    g2.setTransform(original)
    super.paintComponent(g2)
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