package net.hearthstats.companion

import java.awt.image.BufferedImage
import java.util.concurrent.{ Executors, ScheduledFuture, TimeUnit }
import scala.util.control.NonFatal
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.core.GameMode.{ ARENA, CASUAL, FRIENDLY, PRACTICE, RANKED }
import net.hearthstats.core.HeroClass
import net.hearthstats.game._
import net.hearthstats.game.GameEvents.screenToObject
import net.hearthstats.game.Screen._
import net.hearthstats.game.imageanalysis._
import net.hearthstats.game.ocr.{ OpponentNameOcr, OpponentNameRankedOcr, OpponentNameUnrankedOcr }
import net.hearthstats.hstatsapi.{ DeckUtils, MatchUtils }
import net.hearthstats.ui.HearthstatsPresenter
import net.hearthstats.ui.log.Log
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.Observable
import rx.subjects.PublishSubject
import net.hearthstats.game.ocr.BackgroundImageSave

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  deckUtils: DeckUtils,
  matchUtils: MatchUtils,
  companionState: CompanionState,
  matchState: MatchState,
  lobbyAnalyser: LobbyAnalyser,
  individualPixelAnalyser: IndividualPixelAnalyser,
  classAnalyser: HsClassAnalyser,
  inGameAnalyser: InGameAnalyser,
  screenAnalyser: ScreenAnalyser,
  uiLog: Log,
  hsPresenter: HearthstatsPresenter,
  deckOverlay: DeckOverlayModule) extends Logging {

  import lobbyAnalyser._
  import companionState.iterationsSinceClassCheckingStarted

  val subject = PublishSubject.create[Boolean]
  val hsFound: Observable[Boolean] = subject.asObservable.cache

  var checkIfRunning: Option[ScheduledFuture[_]] = None

  def start() {
    checkIfRunning = Some(Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable {
      def run(): Unit = {
        val found = programHelper.foundProgram
        trace(s"HS found ? :$found ")
        subject.onNext(found)
      }
    }, config.pollingDelayMs.get, config.pollingDelayMs.get, TimeUnit.MILLISECONDS))

    info("started")
  }

  def stop(): Unit = {
    info("stopping")
    checkIfRunning.map(_.cancel(true))
  }

  hsFound.distinctUntilChanged.subscribe(found =>
    if (found) {
      uiLog.info("Hearthstone detected")
    } else {
      uiLog.warn("Hearthstone not detected")
    })

  val gameImages: Observable[BufferedImage] =
    hsFound.map { found =>
      if (found)
        Some(programHelper.getScreenCapture)
      else
        None
    }.filter(_.isDefined).map(_.get)

  val gameEvents: Observable[ScreenEvent] = gameImages.
    map(eventFromImage).
    filter(_.isDefined).
    map(_.get)

  gameEvents.subscribe(handleGameEvent _)

  private def handleGameEvent(evt: ScreenEvent): Unit = try {
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
      case OngoingGameScreen => testForOpponentOrYourTurn(image)
      case GameResultScreen => testForVictoryOrDefeat(image)
      case _ =>
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
    hsMatch.deck = for {
      slot <- companionState.deckSlot
      d <- deckUtils.getDeckFromSlot(slot)
    } yield d
  }

  private def handleStartingHand(image: BufferedImage): Unit = {
    testForCoin(image)
    testForOpponentName(image)
  }

  private def handleMatchStart(image: BufferedImage): Unit = {
    companionState.findingOpponent = false
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

  private def testForVictoryOrDefeat(image: BufferedImage) {
    if (!victoryOrDefeatDetected) {
      info("Testing for victory or defeat")
      inGameAnalyser.imageShowsVictoryOrDefeat(image) match {
        case Some(outcome) =>
          uiLog.info(s"Result detected by screen capture : $outcome")
          hsMatch.result = Some(outcome)
          hsMatch.endMatch
          matchUtils.submitMatchResult()
          deckOverlay.reset
        case _ =>
          debug("Result not detected on screen capture")
      }
    }
  }

  private def victoryOrDefeatDetected =
    matchState.currentMatch.flatMap(_.result).isDefined

  private def testForOpponentOrYourTurn(image: BufferedImage) {
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
          hsMatch.numTurns += 1
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
        hsMatch.opponentName = opponentName
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
      hsMatch.coin = Some(true)
      hsPresenter.setCoin(true)
    }
  }

  private def testForYourClass(image: BufferedImage): Unit = {
    if (HeroClass.UNDETECTED == hsMatch.userClass) {
      debug("Testing for your class")
      classAnalyser.imageIdentifyYourClass(image) match {
        case Some(newClass) =>
          hsMatch.userClass = newClass
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
          hsMatch.opponentClass = newClass
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

  private def hsMatch = {
    if (matchState.currentMatch.isEmpty) {
      uiLog.warn("Match start was not detected")
      matchState.nextMatch(companionState)
    }
    matchState.currentMatch.get
  }

  private def eventFromImage(bi: BufferedImage): Option[ScreenEvent] = try {
    import companionState._
    if (iterationsSinceScreenMatched > 10) { lastScreen = None }
    Option(screenAnalyser.identifyScreen(bi, lastScreen.getOrElse(null))) match {
      case Some(screen) =>
        iterationsSinceScreenMatched = 0
        val e = eventFromScreen(screen, bi)
        debug(s"screen $screen => event $e")
        e
      case None =>
        info(s"no screen match on image, last match was $lastScreen $iterationsSinceScreenMatched iterations ago")
        iterationsSinceScreenMatched += 1
        None
    }
  } catch {
    case NonFatal(e) => error(e.getMessage, e); None
  }

  private def eventFromScreen(newScreen: Screen, image: BufferedImage): Option[ScreenEvent] = {
    import companionState._
    import net.hearthstats.game.GameEvents._

    if (newScreen == PLAY_LOBBY && individualPixelAnalyser.testAllPixelsMatch(image, UniquePixel.allBackgroundPlay))
      //      Sometimes the OS X version captures a screenshot where, apparently, Hearthstone hasn't finished compositing the screen
      //    and so we only get the background. This can happen whenever there is something layered over the main screen, for example
      //    during the 'Finding Opponent', 'Victory' and 'Defeat' screens.</p>
      //   At the moment I haven't worked out how to ensure we always get the completed screen. So this method detects when
      //    we've receive and incomplete play background instead of the 'Finding Opponent' screen, so we can reject it and try again.</p>
      None
    else if (lastScreen == FINDING_OPPONENT && iterationsSinceFindingOpponent < 5) {
      iterationsSinceFindingOpponent += 1
      None
    } else {
      iterationsSinceFindingOpponent = 0
      Some(ScreenEvent(newScreen, image))
    }
  }

}