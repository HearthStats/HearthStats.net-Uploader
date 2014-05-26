package net.hearthstats.ui

import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions.asScalaBuffer

import javax.swing.Box
import javax.swing.Box.createHorizontalBox
import javax.swing.Box.createVerticalBox
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.SwingUtilities
import net.hearthstats.Card
import net.hearthstats.Deck
import net.hearthstats.log.Log
import scala.swing.Swing._

object ClickableDeckBox {

  def makeBox(deck: Deck): Box = {
    downloadImages(deck)
    val container = createHorizontalBox
    val box = createVerticalBox
    val imageLabel = new JLabel
    imageLabel.setPreferredSize(new Dimension(289, 398))
    for (card <- deck.cards) {
      val cardLabel = new ClickableLabel(card)
      box.add(cardLabel)
      cardLabel.addMouseListener(new MouseHandler(card, imageLabel))
    }
    container.add(box)
    container.add(imageLabel)
    container
  }

  private def downloadImages(deck: Deck) {
    val scheduledExecutorService = Executors.newScheduledThreadPool(30)
    for (card <- deck.cards) {
      scheduledExecutorService.submit(new Runnable {

        override def run {
          try {
            val rbc = Channels.newChannel(new URL(card.url).openStream)
            val file = card.localFile
            if (file.length < 30000) {
              val fos = new FileOutputStream(file)
              fos.getChannel.transferFrom(rbc, 0, Long.MaxValue)
              fos.close()
              rbc.close()
              Log.debug(card.name + " saved to cache folder")
            } else
              Log.debug(card.name + " already in cache, skipping")
          } catch {
            case e: Exception => Log.error("Could not download image for " + card.name, e)
          }
        }
      })
    }
    scheduledExecutorService.shutdown
    scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS)
    Log.info("all images downloaded successfully")
  }

  private class MouseHandler(var card: Card, var imageLabel: JLabel) extends MouseAdapter {

    override def mouseEntered(e: MouseEvent) {
      onEDT(imageLabel.setIcon(new ImageIcon(card.localURL)))
    }
  }
}
