package net.hearthstats.ui

import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import scala.swing.Swing.onEDT
import javax.swing.Box.createVerticalBox
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import net.hearthstats.Card
import net.hearthstats.CardUtils
import net.hearthstats.Deck
import net.hearthstats.log.Log
import rx.lang.scala.Observable
import net.hearthstats.logmonitor.CardEvent
import net.hearthstats.logmonitor.CardEventType._
import javax.swing.JFrame
import scala.swing.Frame
import scala.swing.MainFrame
import scala.swing.BorderPanel
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.WindowConstants

class ClickableDeckBox(deck: Deck, cardEvents: Observable[CardEvent]) extends JPanel {

  setLayout(new BoxLayout(this, BoxLayout.X_AXIS))
  val box = createVerticalBox
  val imageLabel = new JLabel
  imageLabel.setPreferredSize(new Dimension(289, 398))
  val cardLabels: Map[String, ClickableLabel] = (for (card <- deck.cards) yield {
    val cardLabel = new ClickableLabel(card)
    box.add(cardLabel)
    cardLabel.addMouseListener(new MouseHandler(card, imageLabel))

    card.name -> cardLabel
  }).toMap
  add(box)
  add(imageLabel)

  val subscription = cardEvents.subscribe {
    _ match {
      case CardEvent(card, DRAWN) => findLabel(card) map (_.decreaseRemaining())
      case CardEvent(card, REPLACED) => findLabel(card) map (_.increaseRemaining())
    }
  }

  private def findLabel(c: Card): Option[ClickableLabel] =
    if (c.collectible) {
      val l = cardLabels.get(c.name)
      if (l.isDefined) l
      else {
        Log.warn(s"card ${c.name} not found in deck")
        None
      }
    } else None
}

object ClickableDeckBox {
  def showBox(deck: Deck): ClickableDeckBox = showBox(deck, Observable.empty)

  def showBox(deck: Deck, cardEvents: Observable[CardEvent]): ClickableDeckBox = {
    CardUtils.downloadImages(deck.cards)
    val box = new ClickableDeckBox(deck, cardEvents)
    val frame = new JFrame {
      setAlwaysOnTop(true)
      setFocusableWindowState(true)
      getContentPane.add(box)
      pack()
      setVisible(true)

      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      addWindowListener(new WindowAdapter {
        override def windowClosed(e: WindowEvent): Unit = {
          box.subscription.unsubscribe()
        }
      })

    }
    box
  }
}

case class MouseHandler(card: Card, imageLabel: JLabel) extends MouseAdapter {

  override def mouseEntered(e: MouseEvent) {
    onEDT(imageLabel.setIcon(new ImageIcon(card.localURL)))
  }
}
