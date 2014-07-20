package net.hearthstats.logmonitor

import java.io.{BufferedWriter, File, FileWriter}

import net.hearthstats.ui.ClickableDeckBox
import net.hearthstats.{DeckUtils, EnvironmentTest}

object LogMonitorDeckOverlayFor2DecksMain extends App {
  val environment = EnvironmentTest
  val tempLogFile = File.createTempFile("hssample", "log")
  println(s"monitorin $tempLogFile ")
  val monitor = new HearthstoneLogMonitor(tempLogFile.getAbsolutePath)

  val deck1 = DeckUtils.getDeck(20034)
  val deck2 = DeckUtils.getDeck(20462)

  val box1 = ClickableDeckBox.showBox(deck1, monitor.cardEvents, environment)

  new Thread {
    override def run() = {
      val writer = new BufferedWriter(new FileWriter(tempLogFile))
      Thread.sleep(500)
      writer.write(initialHand)
      writer.flush()
      Thread.sleep(3000)
      ClickableDeckBox.showBox(deck2, monitor.cardEvents, environment)
      Thread.sleep(500)
      writer.write(initialHand2)
      writer.flush()
      writer.close()
    }
  }.start()

  def initialHand =
    """
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Soulfire id=17 zone=HAND zonePos=0 cardId=EX1_308 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Sunfury Protector id=31 zone=HAND zonePos=0 cardId=EX1_058 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Twilight Drake id=13 zone=HAND zonePos=0 cardId=EX1_043 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
    """

  def initialHand2 =
    """
[Zone] ZoneChangeList.ProcessChanges() - id=2 local=False [name=Animal Companion id=17 zone=HAND zonePos=0 cardId=EX1_308 player=1] zone from FRIENDLY DECK -> FRIENDLY HAND
    """
}