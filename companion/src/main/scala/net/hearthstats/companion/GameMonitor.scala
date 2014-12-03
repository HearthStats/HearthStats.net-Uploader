package net.hearthstats.companion

import java.awt.image.BufferedImage
import java.util.concurrent.{ ScheduledThreadPoolExecutor, TimeUnit }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import akka.actor.{ ActorSystem, Cancellable, actorRef2Scala }
import akka.actor.ActorDSL.{ Act, actor }
import akka.event.LoggingReceive
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.core.{ HearthstoneMatch, MatchOutcome }
import net.hearthstats.core.GameMode._
import net.hearthstats.game._
import net.hearthstats.game.imageanalysis.{ Casual, LobbyAnalyser, Ranked }
import net.hearthstats.hstatsapi.{ DeckUtils, MatchUtils }
import net.hearthstats.modules.VideoEncoderFactory
import net.hearthstats.ui.HearthstatsPresenter
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.ui.log.Log
import net.hearthstats.core.Rank
import net.hearthstats.ui.notification.DialogNotification

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
  deckOverlay: DeckOverlayModule,
  notifier: NotificationQueue) extends Logging {

  import config._
  import lobbyAnalyser._

  implicit val system = ActorSystem("companion")

  val delay = config.pollingDelayMs.get

  var programFoundIterations = 0
  var lastFound = false
  var lastCaptureMs = System.nanoTime / 1000000

  var notFoundCount = 1

  var checkIfRunning: Option[Cancellable] = None
  val executor = new ScheduledThreadPoolExecutor(1)

  def start() {
    val checkIfRunning = Some(executor.scheduleAtFixedRate(new Runnable {
      def run() = handleProgramFound(programHelper.foundProgram)
    }, 0, delay, TimeUnit.MILLISECONDS))
    info("started")
  }

  def stop(): Unit = {
    checkIfRunning.map(_.cancel())
    VideoEncoder.stop()
    info("stopped")
  }

  import akka.actor.ActorDSL._

  private def handleProgramFound(found: Boolean): Unit = {
    if (!found && notFoundCount == 1) {
      uiLog.warn("Hearthstone not detected")
      logMonitor.stop()
    }
    if (found && notFoundCount > 0) {
      uiLog.info("Hearthstone detected")
      logMonitor.start()
    }
    if (found) {
      notFoundCount = 0
      val now = System.nanoTime / 1000000
      if (now > lastCaptureMs + delay) {
        screenEvents.handleImage(programHelper.getScreenCapture)
        lastCaptureMs = now
      }
    } else {
      notFoundCount += 1
    }
  }

  screenEvents.addReceive({
    case e: ScreenEvent => handleScreenEvent(e)
  }, Some("GameMonitor"))

  logMonitor.addReceive({
    case e: GameEvent => handleLogEvent(e)
  }, Some("LogFileMonitor"))

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
      case LegendRank(rank) =>
        handleLegendRank(rank)

      case _ => debug(s"Ignoring event $evt")
    }
    matchState.updateLog(evt, companionState.currentDurationMs)
  } catch {
    case NonFatal(t) =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleScreenEvent(evt: ScreenEvent): Unit = try {
    debug(evt)
    val image = evt.image
    evt.screen match {
      case MatchStartScreen => handleGameStartScreen()
      case PlayLobby => handlePlayLobby(evt)
      case PracticeLobby => handlePracticeLobby(image)
      case FriendlyLobby => handleFriendlyLobby(image)
      case ArenaLobby => handleArenaLobby(image)
      case other => debug(s"$other, no action taken")
    }
  } catch {
    case NonFatal(t) =>
      error(t.getMessage, t)
      uiLog.error(t.getMessage, t)
  }

  private def handleGameStartScreen() =
    if (companionState.ongoingVideo.isEmpty) {
      companionState.startMatch()
      VideoEncoder.start()
    }

  private def handleGameStart(heroChosen: HeroChosen): Unit = {
    val matchStart = // this is the first time the method is called for this match
      (heroChosen.opponent && companionState.playerId1.isEmpty) ||
        (!heroChosen.opponent && companionState.opponentId1.isEmpty)
    if (matchStart) {
      companionState.startMatch()
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
      if (enableDeckOverlay) deckOverlay.startMonitoringCards(heroChosen.player)
    }
    for {
      slot <- companionState.deckSlot
      d <- deckUtils.getDeckFromSlot(slot)
    } {
      updateMatch(_.withDeck(d))
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
    updateMatch(_.withJsonLog(matchState.jsonGameLog.get))
    updateMatch(_.withDuration(companionState.currentDurationMs / 1000))
    matchUtils.submitMatchResult()
    deckOverlay.reset()
    companionState.reset()
    VideoEncoder.stop()
  }

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
      if (enableDeckOverlay) {
        deckUtils.getDeckFromSlot(deckSlot) match {
          case Some(deck) =>
	          if (notifier != null) notifier.add(s"Deck Detected", s"$deck", false)
	          deckOverlay.show(deck)
          case None =>
            if (notifier != null) notifier.add(s"Could not find deck in slot $deckSlot", "", false)
        }
      }
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

  private def handleLegendRank(rank: Int) {
    if (rank > 0) {
      companionState.rank = Some(Rank.LEGEND)
      uiLog.info(s"Legand rank $rank detected")
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

  object VideoEncoder {
    def start(): Unit = {
      info("start recording video")
      val videoEncoder = videoEncoderFactory.newInstance(!recordVideo)
      companionState.ongoingVideo = Some(videoEncoder.newVideo(videoFps, videoWidth, videoHeight))
      videoEncoderActor ! EncodeAfter(0)
    }

    def stop(): Unit =
      videoEncoderActor ! StopRecording

    val videoEncoderActor = actor(new Act {
      val encoding: Receive = LoggingReceive {
        case StopRecording =>
          become(stopped)
          info("stop recording video")
        case EncodeAfter(timeMs) =>
          companionState.ongoingVideo match {
            case Some(video) =>
              val images = companionState.imagesAfter(timeMs).toList
              debug(s"batch of ${images.size} to encode")
              if (images.nonEmpty) {
                video.encodeImages(images).onSuccess {
                  case time: Long => self ! EncodeAfter(time)
                }
              } else { // we are faster to encode, no screenshots ready yet
                system.scheduler.scheduleOnce(delay.millis, self, EncodeAfter(timeMs))
              }
            case None => system.scheduler.scheduleOnce(delay.millis, self, EncodeAfter(timeMs))
            // video not started yet, try again soon
          }
      }

      val stopped: Receive = LoggingReceive {
        case StopRecording =>
        case e =>
          become(encoding)
          self ! e
      }

      become(stopped)
    })

    case object StopRecording
    case class EncodeAfter(timeMs: Long)
  }
}
