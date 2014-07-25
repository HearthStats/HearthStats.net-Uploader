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
import rx.lang.scala.observables.ConnectableObservable
import net.hearthstats.OldConfig
import java.awt.BorderLayout
import javax.swing.JCheckBox
import scala.swing.Swing._
import scala.concurrent.Future
import net.hearthstats.config.EnvironmentComponent
import ConfigComponent

trait DeckOverlaySwingComponent extends DeckOverlayPresenterComponent { self: ConfigComponent =>
  val deckOverlay = new DeckOverlaySwing

  class DeckOverlaySwing extends JFrame with DeckOverlayPresenter {
    import config._

    var cardLabels: Map[String, ClickableLabel] = Map.empty

    def showDeck(deck: Deck) {
      val imagesReady = CardUtils.downloadImages(deck.cards)

      val content = getContentPane
      content.setLayout(new BorderLayout)
      val box = createVerticalBox
      val imageLabel = new JLabel
      imageLabel.setPreferredSize(new Dimension(289, 398))
      cardLabels =
        (for {
          card <- deck.cards
          cardLabel = new ClickableLabel(card, imagesReady)
        } yield {
          box.add(cardLabel)
          cardLabel.addMouseListener(new MouseHandler(card, imageLabel))
          card.name -> cardLabel
        }).toMap

      val cardCheckBox = new JCheckBox("Display full card image")
      cardCheckBox.addChangeListener(ChangeListener(_ => imageLabel.setVisible(cardCheckBox.isSelected)))
      content.add(cardCheckBox, BorderLayout.NORTH)
      content.add(box, BorderLayout.CENTER)
      content.add(imageLabel, BorderLayout.EAST)
      imageLabel.setVisible(false)

      setAlwaysOnTop(true)
      setFocusableWindowState(true)
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

      setLocation(deckX, deckY)
      setSize(deckWidth, deckHeight)
      setVisible(true)

    }

    override def dispose(): Unit = {
      //    connection.unsubscribe()
      try {
        // Save the location of the window, if it is visible (it won't be visible if the selected deck is invalid)
        if (isVisible) {
          val p = getLocationOnScreen
          deckX.set(p.x)
          deckY.set(p.y)
          val rect = getSize()
          deckWidth.set(rect.getWidth.toInt)
          deckHeight.set(rect.getHeight.toInt)
        }
      } catch {
        case e: Exception =>
          Log.warn("Error occurred trying to save your settings, your deck overlay position may not be saved", e)
      }
      super.dispose()
    }

    //  val newEvents = cardEvents.publish
    //  val connection = newEvents.connect
    //  newEvents.subscribe {
    //    _ match {
    //      case CardEvent(card, DRAWN) => findLabel(card) map (_.decreaseRemaining())
    //      case CardEvent(card, REPLACED) => findLabel(card) map (_.increaseRemaining())
    //    }
    //  }

    private def findLabel(c: Card): Option[ClickableLabel] =
      if (c.collectible) {
        val l = cardLabels.get(c.name)
        if (l.isDefined) l
        else {
          Log.warn(s"card ${c.name} not found in deck")
          None
        }
      } else None

    def removeCard(card: Card)

    def addCard(card: Card)

    case class MouseHandler(card: Card, imageLabel: JLabel) extends MouseAdapter {
      override def mouseEntered(e: MouseEvent) {
        onEDT(imageLabel.setIcon(new ImageIcon(card.localURL)))
      }
    }
  }
}
