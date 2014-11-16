package net.hearthstats.companion

import java.awt.image.BufferedImage
import java.util.concurrent.{ Executors, ScheduledFuture, TimeUnit }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.core.GameMode.{ ARENA, CASUAL, FRIENDLY, PRACTICE, RANKED }
import net.hearthstats.core.HeroClass
import net.hearthstats.game._
import net.hearthstats.game.imageanalysis._
import net.hearthstats.game.ocr.{ BackgroundImageSave, OpponentNameOcr, OpponentNameRankedOcr, OpponentNameUnrankedOcr }
import net.hearthstats.hstatsapi.{ DeckUtils, MatchUtils }
import net.hearthstats.modules.{ ReplayHandler, VideoEncoderFactory }
import net.hearthstats.ui.HearthstatsPresenter
import net.hearthstats.ui.log.Log
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.MatchOutcome
import akka.actor.ActorSystem
import scala.concurrent.duration.DurationInt
import akka.actor.Cancellable
import net.hearthstats.util.FileObserver

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  deckUtils: DeckUtils,
  matchUtils: MatchUtils,
  companionState: CompanionState,
  screenEvents: ScreenEvents,
  logMonitor: HearthstoneLogMonitor,
  matchState: MatchState,
  lobbyAnalyser: LobbyAnalyser,
  videoEncoderFactory: VideoEncoderFactory,
  uiLog: Log,
  hsPresenter: HearthstatsPresenter,
  deckOverlay: DeckOverlayModule) extends Logging {

  import config._
  import lobbyAnalyser._

  implicit val system = ActorSystem("companion")

  val delay = config.pollingDelayMs.get.millis

  var checkIfRunning: Option[Cancellable] = None

  def start() {
    val checkIfRunning = Some(system.scheduler.schedule(delay, delay) {
      val found = programHelper.foundProgram
      trace(s"HS found ? :$found ")
      programFound ! found
    })
    info("started")
  }

  def stop(): Unit = {
    checkIfRunning.map(_.cancel())
    info("stopped")
  }

  import akka.actor.ActorDSL._

  val programFound = actor(new Act {
    val found: Receive = {
      case false =>
        become(maybeNotFound)
      case true =>
        screenEvents.handleImage(programHelper.getScreenCapture)
    }
    val notFound: Receive = {
      case true =>
        uiLog.info("Hearthstone detected")
        logMonitor.start()
        screenEvents.handleImage(programHelper.getScreenCapture)
        become(found)
      case _ =>
    }
    val maybeNotFound: Receive = {
      case true =>
        become(found)
        screenEvents.handleImage(programHelper.getScreenCapture)
      case false =>
        uiLog.warn("Hearthstone not detected")
        logMonitor.stop()
        become(notFound)
    }
    become {
      case false =>
        uiLog.warn("Hearthstone not detected")
        become(notFound)
      case true =>
        uiLog.info("Hearthstone detected")
        logMonitor.start()
        screenEvents.handleImage(programHelper.getScreenCapture)
        become(found)
    }
  })

  screenEvents.addReceive({
    case e: ScreenEvent => handleScreenEvent(e)
  }, Some("GameMonitor"))

  logMonitor.addReceive {
    case e: GameEvent => handleLogEvent(e)
  }

  private def handleLogEvent(evt: GameEvent): Unit = try {
    info(evt)
    evt match {
      case GameOver(outcome) =>
        uiLog.info(s"Result $outcome detected in log file")
        endGameImpl(outcome)
      case MatchStart(hero) =>
        handleGameStart(hero)
      case TurnPassedEvent =>
        handleTurnChanged()
      case CardEvent(_, _, CardEventType.RECEIVED, player) =>
        handleCoin(player)
      case FirstPlayer(name, id) =>
        handlePlayerName(name, true)
      case PlayerName(name, id) =>
        handlePlayerName(name, false)

      case _ => debug(s"Ignoring event $evt")
    }
  } catch {
    case NonFatal(t) =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleScreenEvent(evt: ScreenEvent): Unit = try {
    debug(evt)
    val image = evt.image
    evt.screen match {
      case StartingHandScreen => addImageToVideo(image)
      case MatchStartScreen => addImageToVideo(image)
      case PlayLobby => handlePlayLobby(evt)
      case PracticeLobby => handlePracticeLobby(image)
      case FriendlyLobby => handleFriendlyLobby(image)
      case ArenaLobby => handleArenaLobby(image)
      case OngoingGameScreen => addImageToVideo(image)
      case GameResultScreen => addImageToVideo(image)
      case other => debug(s"$other, no action taken")
    }
  } catch {
    case NonFatal(t) =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleGameStart(heroChosen: HeroChosen): Unit = {
    val matchStart = // this is the first time the method is called for this match
      (heroChosen.opponent && companionState.playerId1.isEmpty) ||
        (!heroChosen.opponent && companionState.opponentId1.isEmpty)
    if (matchStart) {
      uiLog.info(s"Match start detected in log file")
      uiLog.divider()
      matchState.nextMatch(companionState)
    }
    val newClass = heroChosen.heroClass
    if (heroChosen.opponent) {
      companionState.opponentId1 = Some(heroChosen.player)
      updateMatch(_.withOpponentClass(newClass))
      hsPresenter.setOpponentClass(newClass)
      uiLog.info(s"Opponent class detected : $newClass")
    } else {
      companionState.playerId1 = Some(heroChosen.player)
      updateMatch(_.withUserClass(newClass))
      hsPresenter.setYourClass(newClass)
      uiLog.info(s"Your class detected : $newClass")
      deckOverlay.startMonitoringCards(heroChosen.player)
    }
    for {
      slot <- companionState.deckSlot
      d <- deckUtils.getDeckFromSlot(slot)
    } {
      updateMatch(_.withDeck(d))
    }
    if (companionState.ongoingVideo.isEmpty) {
      val videoEncoder = videoEncoderFactory.newInstance(!recordVideo)
      companionState.ongoingVideo = Some(videoEncoder.newVideo(videoFps, videoWidth, videoHeight))
    }
  }

  // we need to store the first player name (the user) to differentiate it from the opponent
  private def handlePlayerName(name: String, first: Boolean): Unit = {
    if (first) {
      companionState.firstPlayerName = Some(name)
    } else if (companionState.firstPlayerName != Some(name)) {
      companionState.otherPlayerName = Some(name)
    }
  }

  private def handleOpponentName(name: String): Unit = {
    updateMatch(_.withOpponentName(name))
    hsPresenter.setOpponentName(name)
    uiLog.info(s"Opponent name : $name")

  }

  private def handleCoin(playerId: Int): Unit = {
    if (hsMatch.numTurns < 0) {
      // the coin can only be received before the first turn of the game
      // afterwards it can be wild growth => excess mana for instance
      if (companionState.playerId1 == Some(playerId)) {
        playerHasCoinAtStart(true)
      } else if (companionState.opponentId1 == Some(playerId)) {
        playerHasCoinAtStart(false)
      } else {
        uiLog.warn(s"Coin detection failed : unexpected playerId $playerId")
      }
    }
  }

  private def playerHasCoinAtStart(coin: Boolean): Unit = {
    companionState.isYourTurn = coin // turn changes before first turn
    updateMatch(_.withCoin(coin))
    hsPresenter.setCoin(coin)
    val oppName = if (coin) {
      uiLog.info("Opponent starts, you have the coin")
      companionState.firstPlayerName
    } else {
      uiLog.info("You start, opponent has the coin")
      companionState.otherPlayerName
    }
    oppName.map(handleOpponentName)
  }

  private def handlePracticeLobby(image: BufferedImage): Unit = {
    if (companionState.mode != PRACTICE) {
      uiLog.info("Practice Mode detected")
      companionState.mode = PRACTICE
    }
    detectDeck(image)
  }

  private def handleArenaLobby(image: BufferedImage): Unit = {
    if (companionState.mode != ARENA) {
      uiLog.info("Arena Mode detected")
      companionState.mode = ARENA
    }
    companionState.isNewArenaRun = isNewArenaRun(image)
  }

  private def handleFriendlyLobby(image: BufferedImage): Unit = {
    if (companionState.mode != FRIENDLY) {
      uiLog.info("Versus Mode detected")
      companionState.mode = FRIENDLY
    }
    detectDeck(image)
  }

  private def endGameImpl(outcome: MatchOutcome): Unit = {
    updateMatch(_.withResult(outcome))
    updateMatch(_.endMatch)
    matchUtils.submitMatchResult()
    deckOverlay.reset()
    companionState.reset()
  }

  private def addImageToVideo(i: BufferedImage): Unit =
    companionState.ongoingVideo.map(_.encodeImage(i))

  private def victoryOrDefeatDetected =
    matchState.currentMatch.flatMap(_.result).isDefined

  private def handleTurnChanged() {
    import companionState._
    if (isYourTurn) {
      updateMatch(_.withNewTurn)
      uiLog.info("Opponent turn")
    } else {
      uiLog.info("Your turn")
    }
    isYourTurn = !isYourTurn
  }

  private def detectDeck(image: BufferedImage): Unit =
    for {
      deckSlot <- imageIdentifyDeckSlot(image)
      if Some(deckSlot) != companionState.deckSlot
    } {
      uiLog.info(s"deck slot $deckSlot detected")
      companionState.deckSlot = Some(deckSlot)
      deckUtils.getDeckFromSlot(deckSlot) foreach deckOverlay.show
    }

  private def handlePlayLobby(evt: ScreenEvent): Unit = {
    if (matchState.lastMatch.isDefined && !matchState.submitted && matchState.started) {
      matchUtils.submitMatchResult()
    }
    mode(evt.image) match {
      case Some(Casual) if companionState.mode != CASUAL =>
        uiLog.info("Casual Mode detected")
        companionState.mode = CASUAL
      case Some(Ranked) if companionState.mode != RANKED =>
        uiLog.info("Ranked Mode detected")
        companionState.mode = RANKED
      case _ => // assuming no change in the mode
    }
    if (companionState.mode == RANKED) {
      for (r <- lobbyAnalyser.analyzeRankLevel(evt.image) if companionState.rank != Some(r)) {
        companionState.rank = Some(r)
        uiLog.info(s"rank $r detected")
      }
    }
    detectDeck(evt.image)
  }

  private def updateMatch(f: HearthstoneMatch => HearthstoneMatch): Unit =
    matchState.currentMatch = Some(f(hsMatch))

  private def hsMatch: HearthstoneMatch = {
    if (matchState.currentMatch.isEmpty) {
      uiLog.warn("Match start was not detected")
      matchState.nextMatch(companionState)
    }
    matchState.currentMatch.get
  }

}