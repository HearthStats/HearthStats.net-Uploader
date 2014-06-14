package net.hearthstats.logmonitor

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import net.hearthstats.DeckUtils
import javax.swing.JOptionPane
import net.hearthstats.ui.ClickableDeckBox
import javax.swing.JFrame
import javax.swing.JDialog
import net.hearthstats.Config
import net.hearthstats.util.TranslationCard
import net.hearthstats.config.GameLanguage

object LogMonitorDeckOverlayMain extends App {
  val tempLogFile = File.createTempFile("hssample", "log")
  println(s"monitorin $tempLogFile ")
  val monitor = new HearthstoneLogMonitor(tempLogFile.getAbsolutePath)
  Config.setGameLanguage(GameLanguage.EU)
  TranslationCard.changeTranslation()
  val deck = DeckUtils.getDeck(20034)
  ClickableDeckBox.showBox(deck, monitor.cardEvents)
  new Thread {
    override def run() = {
      val writer = new BufferedWriter(new FileWriter(tempLogFile))
      Thread.sleep(500)
      writer.write(initialHand)
      writer.flush()
      Thread.sleep(5000)
      //      ClickableDeckBox.showBox(deck, monitor.cardEvents)
      writer.write(mulligan)
      writer.close()
    }
  }.start()

  def initialHand =
    """
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Soulfire id=17 zone=HAND zonePos=0 cardId=EX1_308 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Sunfury Protector id=31 zone=HAND zonePos=0 cardId=EX1_058 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Twilight Drake id=13 zone=HAND zonePos=0 cardId=EX1_043 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
    """

  def mulligan =
    """
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Soulfire id=17 zone=DECK zonePos=1 cardId=EX1_308 player=1] zone from FRIENDLY HAND -> FRIENDLY DECK
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Sunfury Protector id=31 zone=DECK zonePos=2 cardId=EX1_058 player=1] zone from FRIENDLY HAND -> FRIENDLY DECK
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Twilight Drake id=13 zone=DECK zonePos=3 cardId=EX1_043 player=1] zone from FRIENDLY HAND -> FRIENDLY DECK

[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Ancient Watcher id=20 zone=HAND zonePos=0 cardId=EX1_045 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=4 local=False [name=Twilight Drake id=13 zone=HAND zonePos=3 cardId=EX1_043 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
    """
}