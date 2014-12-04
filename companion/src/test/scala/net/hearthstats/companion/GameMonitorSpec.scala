package net.hearthstats.companion

import java.awt.image.BufferedImage
import org.junit.runner.RunWith
import org.mockito.Matchers.{ any, anyString }
import org.mockito.Mockito.{ never, verify, when }
import org.scalatest.{ BeforeAndAfter, FlatSpec, Matchers, OneInstancePerTest }
import org.scalatest.mock.MockitoSugar
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.ProgramHelper
import net.hearthstats.config.{ TestConfig, UserConfig }
import net.hearthstats.core.Deck
import net.hearthstats.core.GameMode.{ ARENA, CASUAL, RANKED }
import net.hearthstats.core.Rank
import net.hearthstats.game.{ HearthstoneLogMonitor, MatchState, Screen }
import net.hearthstats.game.imageanalysis.{ Casual, IndividualPixelAnalyser, LobbyAnalyser, LobbyMode, Ranked, ScreenAnalyser }
import net.hearthstats.hstatsapi.{ DeckUtils, MatchUtils }
import net.hearthstats.modules.{ ReplayHandler, VideoEncoderFactory }
import net.hearthstats.ui.HearthstatsPresenter
import net.hearthstats.ui.CompanionFrame
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GameMonitorSpec extends FlatSpec with Matchers with MockitoSugar with OneInstancePerTest with BeforeAndAfter {
  val config: UserConfig = TestConfig
  val state = new CompanionState
  val helper = mock[ProgramHelper]
  val screenAnalyser = mock[ScreenAnalyser]
  val individualPixelAnalyser = mock[IndividualPixelAnalyser]

  val lobbyAnalyser = mock[LobbyAnalyser]

  val log = mock[Log]

  val deckUtils = mock[DeckUtils]
  val matchUtils = mock[MatchUtils]

  val matchState = wire[MatchState]
  val hsPresenter = mock[HearthstatsPresenter]
  val deckOverlay = mock[DeckOverlayModule]
  val videoEncoderFactory = wire[VideoEncoderFactory]
  val replayHandler = mock[ReplayHandler]
  val companionEvents = wire[ScreenEvents]
  val logMonitor = mock[HearthstoneLogMonitor]
  val monitor = wire[GameMonitor]
  val notificationQueue = mock[NotificationQueue]
  val rank8Lobby = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
  val newArenaRun = new BufferedImage(100, 10, BufferedImage.TYPE_INT_RGB)

  val sleep = config.pollingDelayMs.get * 4

  before { monitor.start() }

  after { monitor.stop() }

  ignore should "warn when HS is not detected" in {
    verify(log, never).info(anyString)
    when(helper.foundProgram).thenReturn(false)

    Thread.sleep(sleep)
    verify(log, never).info(anyString)
    verify(log).warn("Hearthstone not detected")
  }

  ignore should "info when HS is detected" in {
    verify(log, never).info(anyString)
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