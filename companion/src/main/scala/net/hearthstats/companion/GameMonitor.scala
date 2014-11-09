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
  classAnalyser: HsClassAnalyser,
  inGameAnalyser: InGameAnalyser,
  videoEncoderFactory: VideoEncoderFactory,
  uiLog: Log,
  hsPresenter: HearthstatsPresenter,
  deckOverlay: DeckOverlayModule) extends Logging {

  import config._
  import companionState.iterationsSinceClassCheckingStarted
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
    val start: Receive = {
      case false =>
        uiLog.warn("Hearthstone not detected")
        become(notFound)
      case true =>
        uiLog.info("Hearthstone detected")
        screenEvents.handleImage(programHelper.getScreenCapture)
        become(found)
    }
    val found: Receive = {
      case false =>
        uiLog.warn("Hearthstone not detected")
        become(notFound)
      case true =>
        screenEvents.handleImage(programHelper.getScreenCapture)
    }
    val notFound: Receive = {
      case true =>
        uiLog.info("Hearthstone detected")
        screenEvents.handleImage(programHelper.getScreenCapture)
        become(found)
      case _ =>
    }
    become(start)
  })

  screenEvents.addReceive {
    case e: ScreenEvent => handleScreenEvent(e)
  }

  logMonitor.addReceive {
    case e: GameEvent => handleGameEvent(e)
  }

  private def handleGameEvent(evt: GameEvent): Unit = try {
    info(evt)
    evt match {
      case GameOver(outcome) =>
        uiLog.info(s"Result $outcome detected in log file")
        endGameImpl(outcome)
      case MatchStart(_) =>
        uiLog.info(s"Match start detected in log file")
        matchStartImpl()

      case _ => debug(s"Ignoring event $evt")
    }
  } catch {
    case NonFatal(t) =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleScreenEvent(evt: ScreenEvent): Unit = try {
    debug(evt)
    if (evt.screen != FindingOpponent) {
      companionState.findingOpponent = false
    }
    val image = evt.image
    evt.screen match {
      case FindingOpponent if !companionState.findingOpponent => handleFindingOpponent()
      case StartingHandScreen => handleStartingHand(image)
      case MatchStartScreen => handleMatchStart(image)
      case PlayLobby => handlePlayLobby(evt)
      case PracticeLobby if companionState.mode != PRACTICE => handlePracticeLobby(image)
      case FriendlyLobby if companionState.mode != FRIENDLY => handleFriendlyLobby(image)
      case ArenaLobby if companionState.mode != ARENA => handleArenaLobby(image)
      case OngoingGameScreen => handleOngoingGame(image)
      case GameResultScreen => handleEndResult(image)
      case other => info(s"$other, no action taken")
    }
  } catch {
    case NonFatal(t) =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleFindingOpponent(): Unit = {
    companionState.findingOpponent = true
    uiLog.info(s"Finding opponent, new match will start soon ...")
    uiLog.divider()
    matchState.nextMatch(companionState)
    for {
      slot <- companionState.deckSlot
      d <- deckUtils.getDeckFromSlot(slot)
    } {
      updateMatch(_.withDeck(d))
    }
  }

  private def handleStartingHand(image: BufferedImage): Unit = {
    addImageToVideo(image)
    testForCoin(image)
    testForOpponentName(image)
  }

  private def matchStartImpl(): Unit = {
    companionState.findingOpponent = false
    if (companionState.ongoingVideo.isEmpty) {
      val videoEncoder = videoEncoderFactory.newInstance(!recordVideo)
      companionState.ongoingVideo = Some(videoEncoder.newVideo(videoFps, videoWidth, videoHeight))
    }
  }

  private def handleMatchStart(image: BufferedImage): Unit = {
    //    matchStartImpl()
    addImageToVideo(image)
    testForCoin(image)
    testForOpponentName(image)
    testForYourClass(image)
    testForOpponentClass(image)
    iterationsSinceClassCheckingStarted += 1
  }

  private def handlePracticeLobby(image: BufferedImage): Unit = {
    uiLog.info("Practice Mode detected")
    companionState.mode = PRACTICE
    detectDeck(image)
  }

  private def handleArenaLobby(image: BufferedImage): Unit = {
    uiLog.info("Arena Mode detected")
    companionState.mode = ARENA
    companionState.isNewArenaRun = isNewArenaRun(image)
  }

  private def handleFriendlyLobby(image: BufferedImage): Unit = {
    uiLog.info("Versus Mode detected")
    companionState.mode = FRIENDLY
    detectDeck(image)
  }

  private def handleEndResult(image: BufferedImage) {
    addImageToVideo(image)
    //    if (!victoryOrDefeatDetected) {
    //      info("Testing for victory or defeat")
    //      inGameAnalyser.imageShowsVictoryOrDefeat(image) match {
    //        case Some(outcome) =>
    //          uiLog.info(s"Result detected by screen capture : $outcome")
    //          endGameImpl(outcome)
    //        case _ =>
    //          debug("Result not detected on screen capture")
    //      }
    //    }
  }

  private def endGameImpl(outcome: MatchOutcome): Unit = {
    updateMatch(_.withResult(outcome))
    updateMatch(_.endMatch)
    matchUtils.submitMatchResult()
    deckOverlay.reset()

  }

  private def addImageToVideo(i: BufferedImage): Unit =
    companionState.ongoingVideo.map(_.encodeImage(i))

  private def victoryOrDefeatDetected =
    matchState.currentMatch.flatMap(_.result).isDefined

  private def handleOngoingGame(image: BufferedImage) {
    addImageToVideo(image)
    if (!matchState.started) {
      testForCoin(image)
      testForOpponentName(image)
      matchState.started = true
    }
    import companionState._
    import inGameAnalyser._
    if (isYourTurn) {
      debug("Testing for opponent turn")
      if (imageShowsOpponentTurn(image)) {
        iterationsSinceYourTurn += 1
        if (iterationsSinceYourTurn > 2) {
          isYourTurn = false
          uiLog.info("Opponent turn")
          iterationsSinceYourTurn = 0
        }
      } else iterationsSinceYourTurn = 0
    } else {
      debug("Testing for your turn")
      if (imageShowsYourTurn(image)) {
        iterationsSinceOpponentTurn += 1
        if (iterationsSinceOpponentTurn > 2) {
          isYourTurn = true
          updateMatch(_.withNewTurn)
          uiLog.info("Your turn")
          iterationsSinceOpponentTurn = 0
        }
      } else iterationsSinceOpponentTurn = 0
    }
  }

  private val opponentNameRankedOcr = new OpponentNameRankedOcr
  private val opponentNameUnrankedOcr = new OpponentNameUnrankedOcr

  private def opponentNameOcr: OpponentNameOcr =
    if (companionState.mode == RANKED) opponentNameRankedOcr
    else opponentNameUnrankedOcr

  private def testForOpponentName(image: BufferedImage) {
    if (hsMatch.opponentName == null) {
      debug("Testing for opponent name")
      if (inGameAnalyser.imageShowsOpponentName(image)) {
        val opponentName = opponentNameOcr.process(image)
        updateMatch(_.withOpponentName(opponentName))
        hsPresenter.setOpponentName(opponentName)
        uiLog.info(s"Opponent name : $opponentName")
      }
    }
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

  private def testForCoin(image: BufferedImage): Unit = {
    if (hsMatch.coin.isEmpty && inGameAnalyser.imageShowsCoin(image)) {
      uiLog.info("Coin detected")
      updateMatch(_.withCoin(true))
      hsPresenter.setCoin(true)
    }
  }

  private def testForYourClass(image: BufferedImage): Unit = {
    if (HeroClass.UNDETECTED == hsMatch.userClass) {
      debug("Testing for your class")
      classAnalyser.imageIdentifyYourClass(image) match {
        case Some(newClass) =>
          updateMatch(_.withUserClass(newClass))
          hsPresenter.setYourClass(newClass)
          uiLog.info(s"Your class detected : $newClass")
        case None =>
      }
      if (iterationsSinceClassCheckingStarted > 3 && (iterationsSinceClassCheckingStarted & 3) == 0) {
        val filename = "class-yours-" + (iterationsSinceClassCheckingStarted >> 2)
        BackgroundImageSave.saveCroppedPngImage(image, filename, 204, 600, 478, 530)
      }
    }
  }

  private def testForOpponentClass(image: BufferedImage): Unit = {
    if (HeroClass.UNDETECTED == hsMatch.opponentClass) {
      debug("Testing for opponent class")
      classAnalyser.imageIdentifyOpponentClass(image) match {
        case Some(newClass) =>
          updateMatch(_.withOpponentClass(newClass))
          hsPresenter.setOpponentClass(newClass)
          uiLog.info(s"Opponent class detected : $newClass")
        case None =>
      }
      if (iterationsSinceClassCheckingStarted > 3 && (iterationsSinceClassCheckingStarted & 3) == 0) {
        val filename = "class-opponent-" + (iterationsSinceClassCheckingStarted >> 2)
        BackgroundImageSave.saveCroppedPngImage(image, filename, 1028, 28, 478, 530)
      }
    }
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