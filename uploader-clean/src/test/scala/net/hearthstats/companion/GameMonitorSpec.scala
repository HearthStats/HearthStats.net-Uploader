package net.hearthstats.companion

import org.scalatest._
import org.junit.runner.RunWith
import com.softwaremill.macwire.MacwireMacros._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ Finders, FlatSpec, Matchers }
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.config.TestEnvironment
import net.hearthstats.config.UserConfig
import net.hearthstats.config.TestConfig
import net.hearthstats.ui.log.Log
import java.net.URL
import org.scalatest.mock.MockitoSugar
import net.hearthstats.ProgramHelper
import org.mockito.Mockito._
import org.mockito.Matchers._
import javax.imageio.ImageIO
import net.hearthstats.core.GameMode._
import net.hearthstats.core.Rank
import net.hearthstats.game.imageanalysis.LobbyAnalyser
import net.hearthstats.game.imageanalysis.RelativePixelAnalyser
import net.hearthstats.game.imageanalysis.ScreenAnalyser
import net.hearthstats.game.imageanalysis.IndividualPixelAnalyser
import net.hearthstats.game.Screen
import java.awt.image.BufferedImage
import net.hearthstats.game.imageanalysis.Ranked
import net.hearthstats.game.imageanalysis.Casual
import net.hearthstats.game.imageanalysis.LobbyMode
import org.mockito.ArgumentCaptor
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.game.imageanalysis.HsClassAnalyser
import net.hearthstats.ui.HearthstatsPresenter
import net.hearthstats.game.imageanalysis.InGameAnalyser
import net.hearthstats.hstatsapi.DeckUtils
import net.hearthstats.ui.deckoverlay.DeckOverlaySwing
import net.hearthstats.ui.deckoverlay.DeckOverlayPresenter
import net.hearthstats.core.Deck
import net.hearthstats.game.MatchState
import net.hearthstats.hstatsapi.MatchUtils
import net.hearthstats.modules.VideoEncoderFactory
import net.hearthstats.modules.ReplayHandler

@RunWith(classOf[JUnitRunner])
class GameMonitorSpec extends FlatSpec with Matchers with MockitoSugar with OneInstancePerTest with BeforeAndAfter {
  val config: UserConfig = TestConfig
  val state = new CompanionState
  val helper = mock[ProgramHelper]
  val screenAnalyser = mock[ScreenAnalyser]
  val individualPixelAnalyser = mock[IndividualPixelAnalyser]

  val lobbyAnalyser = mock[LobbyAnalyser]
  val classAnalyser = mock[HsClassAnalyser]
  val igAnalyser = mock[InGameAnalyser]

  val log = mock[Log]

  val deckUtils = mock[DeckUtils]
  val matchUtils = mock[MatchUtils]

  val matchState = wire[MatchState]
  val hsPresenter = mock[HearthstatsPresenter]
  val deckOverlay = mock[DeckOverlayModule]
  val videoEncoderFactory = wire[VideoEncoderFactory]
  val replayHandler = mock[ReplayHandler]
  val monitor = wire[GameMonitor]

  val rank8Lobby = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
  val newArenaRun = new BufferedImage(100, 10, BufferedImage.TYPE_INT_RGB)

  val sleep = config.pollingDelayMs.get * 2

  before { monitor.start() }

  after { monitor.stop() }

  "The monitor" should "warn when HS is not detected" in {
    verify(log, never).info(anyString)
    verify(log, never).warn(anyString, any[Exception])
    when(helper.foundProgram).thenReturn(false)

    Thread.sleep(sleep)
    verify(log, never).info(anyString)
    verify(log).warn("Hearthstone not detected")
  }

  "The monitor" should "info when HS is detected" in {
    verify(log, never).info(anyString)
    verify(log, never).warn(anyString, any[Exception])
    when(helper.foundProgram).thenReturn(false)

    when(helper.foundProgram).thenReturn(true)
    Thread.sleep(sleep)
    verify(log).info("Hearthstone detected")
    verify(log, never).warn(anyString, any[Exception])
  }

  "The monitor" should "detect ranked mode and rank" in {
    setupForPlayMode(Ranked)
    state.mode shouldBe RANKED
    state.rank shouldBe Some(Rank.RANK_8)
  }

  "The monitor" should "detect casual mode" in {
    setupForPlayMode(Casual)
    state.mode shouldBe CASUAL
    state.rank shouldBe None
  }

  "The monitor" should "detect deck slot" in {
    setupForPlayMode(Casual)
    state.deckSlot shouldBe Some(3)
  }

  "The monitor" should "detect new Arena run" in {
    setupForArena(true)
    state.deckSlot shouldBe None
    state.mode shouldBe ARENA
    state.isNewArenaRun shouldBe true
  }

  "The monitor" should "detect existing Arena run" in {
    setupForArena(false)
    state.isNewArenaRun shouldBe false
  }

  "The monitor" should "detect changes in game mode" in {
    setupForPlayMode(Ranked)
    state.mode shouldBe RANKED
    setupForPlayMode(Casual)
    state.mode shouldBe CASUAL
    setupForPlayMode(Ranked)
    state.mode shouldBe RANKED
  }

  def setupForPlayMode(mode: LobbyMode) {
    when(helper.foundProgram).thenReturn(true)
    when(helper.getScreenCapture).thenReturn(rank8Lobby)
    when(lobbyAnalyser.mode(rank8Lobby)).thenReturn(Some(mode))
    when(lobbyAnalyser.imageIdentifyDeckSlot(rank8Lobby)).thenReturn(Some(3))
    when(deckUtils.getDeckFromSlot(3)).thenReturn(Some(new Deck))
    when(lobbyAnalyser.analyzeRankLevel(rank8Lobby)).thenReturn(Some(Rank.RANK_8))
    when(screenAnalyser.identifyScreen(any[BufferedImage], any[Screen])).thenReturn(Screen.PLAY_LOBBY)
    Thread.sleep(sleep)
  }

  def setupForArena(isNew: Boolean) {
    when(helper.foundProgram).thenReturn(true)
    when(helper.getScreenCapture).thenReturn(newArenaRun)
    when(lobbyAnalyser.isNewArenaRun(newArenaRun)).thenReturn(isNew)
    when(screenAnalyser.identifyScreen(any[BufferedImage], any[Screen])).thenReturn(Screen.ARENA_LOBBY)
    Thread.sleep(sleep)
  }
}