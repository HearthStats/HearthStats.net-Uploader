package net.hearthstats

import java.net.URL
import org.junit.Test
import scala.collection.GenIterable
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import net.hearthstats.util.Rank
import net.hearthstats.util.MatchOutcome._
import net.hearthstats.analysis.HearthstoneAnalyser
import java.io.FileWriter
import java.io.BufferedWriter
import net.hearthstats.util.MatchOutcome
import grizzled.slf4j.Logging
import java.awt.Toolkit
import java.awt.GraphicsEnvironment

@RunWith(classOf[JUnitRunner])
class LogResultDetectionSpec extends FlatSpec with Matchers with BeforeAndAfterEach with OneInstancePerTest with Logging {
  val writer = new BufferedWriter(new FileWriter(EnvironmentTest.hearthstoneLogFile))

  if (!GraphicsEnvironment.getLocalGraphicsEnvironment.isHeadlessInstance) {
    debug("mock log file :" + EnvironmentTest.hearthstoneLogFile)
    val monitor = new Monitor(EnvironmentTest) {
      override def checkMatchResult(hsMatch: HearthstoneMatch) {}
    }
    HearthstoneAnalyser.monitor = monitor
    HearthstoneAnalyser.addObserver(monitor)
    monitor.setMonitorHearthstoneLog(true)

    "A game win" should "be detected" in {
      sendGame(gameWonLog)
      HearthstoneAnalyser.hsMatch.result shouldBe Some(MatchOutcome.VICTORY)
    }

    "A game lost" should "be detected" in {
      sendGame(gameLostLog)
      HearthstoneAnalyser.hsMatch.result shouldBe Some(MatchOutcome.DEFEAT)
    }

    "A game lost" should "override a game won detected in the screen capture" in {
      HearthstoneAnalyser.hsMatch.result = Some(MatchOutcome.VICTORY)
      sendGame(gameLostLog)
      HearthstoneAnalyser.hsMatch.result shouldBe Some(MatchOutcome.DEFEAT)
    }
  }

  override def beforeEach() {
    HearthstoneAnalyser.hsMatch.initialized = false
    HearthstoneAnalyser.handleMatchStart()
  }

  def sendGame(msg: String) {
    writer.write(msg)
    writer.flush()
    Thread.sleep(600)
  }

  val gameLostLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from FRIENDLY PLAY (Hero) -> FRIENDLY GRAVEYARD
    """
  val gameWonLog = """[Zone] ZoneChangeList.ProcessChanges() - id=120 local=False [name=Malfurion Stormrage id=58 zone=GRAVEYARD zonePos=0 cardId=HERO_06 player=2] zone from OPPOSING PLAY (Hero) -> OPPOSING GRAVEYARD
    """
}