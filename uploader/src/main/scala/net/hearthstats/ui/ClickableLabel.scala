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
//remove if not needed
import scala.collection.JavaConversions._
import scala.swing.Swing._

class ClickableLabel(var card: Card) extends JLabel {

  var remaining = card.count
  private var cardImage = new ImageIcon(card.localURL)
  private var currentBack = cardBack

  val displaySize = new Dimension(214, 38)
  setPreferredSize(displaySize)
  setMaximumSize(displaySize)
  setMinimumSize(displaySize)

  setText(s"   ${card.cost}      ${card.name}")
  setFont(Font.decode(Font.SANS_SERIF).deriveFont(Font.BOLD, 14))
  setForeground(Color.WHITE)

  updateRemaining()

  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      onEDT(handleClick(e.getButton))
    }
  })

  protected override def paintComponent(g: Graphics) {
    val g2 = g.asInstanceOf[Graphics2D]
    val scale = 289F / 214
    val scaleTransform = AffineTransform.getScaleInstance(scale, scale)
    g2.transform(scaleTransform)
    if (remaining < 1) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f))
    g2.drawImage(cardImage.getImage, 0, -38, null)
    scaleTransform.invert()
    g2.transform(scaleTransform)
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f))
    g2.drawImage(currentBack.getImage, 0, 0, null)
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