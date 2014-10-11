package net.hearthstats.companion

import java.awt.image.BufferedImage
import scala.concurrent.duration.DurationInt
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.game.GameEvent
import net.hearthstats.game.Screen._
import net.hearthstats.game.ScreenEvent
import rx.lang.scala.Observable
import net.hearthstats.game.imageanalysis.LobbyAnalyser
import net.hearthstats.core.GameMode._
import net.hearthstats.game.imageanalysis.LobbyAnalyser
import grizzled.slf4j.Logging
import net.hearthstats.game.imageanalysis.Casual
import net.hearthstats.game.imageanalysis.Ranked
import net.hearthstats.game.ScreenGroup
import net.hearthstats.ui.log.Log
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.HeroClass
import net.hearthstats.core.HeroClass._
import net.hearthstats.game.imageanalysis.HsClassAnalyser
import net.hearthstats.game.ocr.BackgroundImageSave
import net.hearthstats.ui.HearthstatsPresenter
import net.hearthstats.game.imageanalysis.InGameAnalyser
import net.hearthstats.ui.deckoverlay.DeckOverlaySwing
import net.hearthstats.hstatsapi.DeckUtils
import net.hearthstats.ui.deckoverlay.DeckOverlayPresenter
import net.hearthstats.game.ocr.OpponentNameRankedOcr
import net.hearthstats.game.ocr.OpponentNameUnrankedOcr
import net.hearthstats.game.ocr.OpponentNameOcr
import org.apache.commons.io.input.Tailer
import rx.subjects.PublishSubject
import rx.lang.scala.JavaConversions.toScalaObservable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import net.hearthstats.game.FirstTurn
import net.hearthstats.game.StartingHand
import net.hearthstats.game.MatchState
import net.hearthstats.core.HearthstoneMatch
import java.util.concurrent.ScheduledFuture
import javax.imageio.ImageIO
import net.hearthstats.game.FindingOpponent
import net.hearthstats.hstatsapi.MatchUtils
import scala.util.Random

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  deckUtils: DeckUtils,
  matchUtils: MatchUtils,
  companionState: CompanionState,
  matchState: MatchState,
  lobbyAnalyser: LobbyAnalyser,
  classAnalyser: HsClassAnalyser,
  inGameAnalyser: InGameAnalyser,
  uiLog: Log,
  hsPresenter: HearthstatsPresenter,
  deckOverlay: DeckOverlayModule,
  imageToEvent: ImageToEvent) extends Logging {

  import lobbyAnalyser._
  import companionState.iterationsSinceClassCheckingStarted

  private val subject = PublishSubject.create[Boolean]
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

  val gameEvents: Observable[GameEvent] = gameImages.
    map(imageToEvent.eventFromImage).
    filter(_.isDefined).
    map(_.get)

  gameEvents.subscribe(handleGameEvent _)

  private def handleGameEvent(evt: GameEvent): Unit = try {
    debug(evt)
    evt match {
      case s: ScreenEvent => handleScreenEvent(s)

      case FindingOpponent =>
        uiLog.info(s"Finding opponent, new match will start soon ...")
        uiLog.divider()
        matchState.nextMatch(companionState)

      case FirstTurn(image) =>
        testForCoin(image)
        testForOpponentName(image)

      case StartingHand(image) =>
        testForCoin(image)
        testForOpponentName(image)
        testForYourClass(image)
        testForOpponentClass(image)
        iterationsSinceClassCheckingStarted += 1
    }
  } catch {
    case t: Throwable =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleScreenEvent(evt: ScreenEvent) = {
    evt.screen match {
      case PLAY_LOBBY =>
        handlePlayLobby(evt)

      case PRACTICE_LOBBY if companionState.mode != Some(PRACTICE) =>
        uiLog.info("Practice Mode detected")
        companionState.mode = Some(PRACTICE)

      case VERSUS_LOBBY if companionState.mode != Some(FRIENDLY) =>
        uiLog.info("Versus Mode detected")
        companionState.mode = Some(FRIENDLY)

      case ARENA_LOBBY if companionState.mode != Some(ARENA) =>
        uiLog.info("Arena Mode detected")
        companionState.mode = Some(ARENA)
        companionState.isNewArenaRun = isNewArenaRun(evt.image)

      case _ =>
        debug("no change in game mode")
    }
    evt.screen.group match {
      case ScreenGroup.MATCH_PLAYING => testForOpponentOrYourTurn(evt.image)
      case ScreenGroup.MATCH_END => testForVictoryOrDefeat(evt.image)
      case _ =>
    }

    detectDeck(evt)
  }

  private def testForVictoryOrDefeat(image: BufferedImage) {
    if (!victoryOrDefeatDetected) {
      info("Testing for victory or defeat")
      inGameAnalyser.imageShowsVictoryOrDefeat(image) match {
        case Some(outcome) =>
          uiLog.info(s"Result detected by screen capture : $outcome")
          if (matchState.currentMatch.isEmpty) {
            uiLog.warn("Result detected but match start was not detected")
            matchState.nextMatch(companionState)
          }
          hsMatch.result = Some(outcome)
          matchUtils.submitMatchResult()
        case _ =>
          BackgroundImageSave.savePngImage(image, "match_end" + Random.nextInt)
          debug("Result not detected on screen capture")
      }
    }
  }

  def victoryOrDefeatDetected = matchState.currentMatch.flatMap(_.result).isDefined

  private def testForOpponentOrYourTurn(image: BufferedImage) {
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

  private def detectDeck(evt: ScreenEvent): Unit = {
    if (Seq(ScreenGroup.PLAY, ScreenGroup.PRACTICE) contains evt.screen.group) {
      for {
        deckSlot <- imageIdentifyDeckSlot(evt.image)
        if Some(deckSlot) != companionState.deckSlot
        deck <- deckUtils.getDeckFromSlot(deckSlot)
      } {
        uiLog.info(s"deck $deck detected")
        companionState.deckSlot = Some(deckSlot)
        deckOverlay.show(deck)
      }
    }
  }

  private def handlePlayLobby(evt: ScreenEvent): Unit = {
    if (matchState.lastMatch.isDefined && !matchState.submitted) {
      matchUtils.submitMatchResult()
    }
    mode(evt.image) match {
      case Some(Casual) if companionState.mode != Some(CASUAL) =>
        uiLog.info("Casual Mode detected")
        companionState.mode = Some(CASUAL)
      case Some(Ranked) if companionState.mode != Some(RANKED) =>
        uiLog.info("Ranked Mode detected")
        companionState.mode = Some(RANKED)
      case _ => // assuming no change in the mode
    }
    if (companionState.mode == Some(RANKED) && companionState.rank.isEmpty) {
      companionState.rank = lobbyAnalyser.analyzeRankLevel(evt.image)
      uiLog.info(s"rank ${companionState.rank} detected")
    }
  }

  private def testForCoin(image: BufferedImage): Unit = {
    if (inGameAnalyser.imageShowsCoin(image)) {
      uiLog.info("Coin detected")
      hsMatch.coin = Some(true)
      hsPresenter.setCoin(true)
    }
  }

  private def testForYourClass(image: BufferedImage): Unit = {
    if (UNDETECTED == hsMatch.userClass) {
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
    if (UNDETECTED == hsMatch.opponentClass) {
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

  def hsMatch = matchState.currentMatch.get
}