package net.hearthstats.companion

import java.awt.image.BufferedImage
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}
import javax.swing.JOptionPane

import akka.actor.{ActorSystem, actorRef2Scala}
import akka.event.LoggingReceive
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.core.GameMode._
import net.hearthstats.core.{GameMode, HearthstoneMatch, MatchOutcome, Rank}
import net.hearthstats.game._
import net.hearthstats.game.imageanalysis.{Casual, LobbyAnalyser, Ranked}
import net.hearthstats.hstatsapi.{DeckUtils, MatchUtils}
import net.hearthstats.modules.VideoEncoderFactory
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.ui.{GeneralUI, HearthstatsPresenter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

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
  hsPresenter: HearthstatsPresenter with GeneralUI,
  deckOverlay: DeckOverlayModule,
  notifier: NotificationQueue) extends Logging {

  import config._
  import lobbyAnalyser._

  implicit val system = ActorSystem("companion")

  // Videos need more frequent polling; when videos aren't in use then reduce the polling speed
  val normalDelay = 200
  val videoDelay = config.pollingDelayMs.get

  var programFoundIterations = 0
  var lastFound = false
  var lastCaptureMs = System.nanoTime / 1000000

  var notFoundCount = 1

  var scheduledRun: Option[ScheduledFuture[_]] = None
  val executor = new ScheduledThreadPoolExecutor(1)

  def start() {
    schedule(normalDelay)
    info("HearthStats Companion started")
  }

  def stop(): Unit = {
    scheduledRun.map(_.cancel(true))
    VideoEncoder.stop()
    info("HearthStats Companion stopped")
  }

  /**
   * Runs the monitor at the given schedule.
   * Can be called at any time, even while already running, to allow the monitoring speed to be adjusted as needed.
   * @param delay The delay between each iteration of screen capture and analysis
   */
  def schedule(delay: Int) {
    debug(s"Scheduling GameMonitor to run every ${delay}ms")
    // If already running then delay the initial run to keep the interval consistent, otherwise start immediately
    val initialDelay = scheduledRun match {
      case Some(sr) =>
        val remaining = sr.getDelay(TimeUnit.MILLISECONDS)
        sr.cancel(false)
        if (remaining > 0) remaining else 0
      case _ => 0
    }
    scheduledRun = Some(executor.scheduleAtFixedRate(new Runnable {
      def run() = handleProgramFound(programHelper.foundProgram, delay)
    }, initialDelay, delay, TimeUnit.MILLISECONDS))
  }

  import akka.actor.ActorDSL._

  private def handleProgramFound(found: Boolean, delay: Int): Unit = {
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

  type EventHandler = PartialFunction[GameEvent, Unit]

  val spectator: EventHandler = {
    case EndSpectatorEvent =>
      uiLog.info("Spectator mode ends")
      become(normal)
  }

  val normal: EventHandler = {
    case BeginSpectatorEvent =>
      uiLog.info("Spectator mode")
      become(spectator)
    case GameOver(playerName, outcome) if playerName == userName.get =>
      uiLog.info(s"Result $outcome detected in log file for $playerName")
      endGameImpl(playerName, outcome)
    case MatchStart(hero) =>
      handleGameStart(hero)
    case TurnCount(turn) =>
      handleTurnChanged(turn)
    case TurnStart(player, _) =>
      uiLog.info(s"$player's turn")
    case FirstPlayer(name) =>
      handlePlayerName(name, true)
    case PlayerName(name, id) =>
      handlePlayerName(name, false)
    case LegendRank(rank) =>
      handleLegendRank(rank)
  }

  var eventHandler: EventHandler = normal

  private def become(handler: EventHandler): Unit =
    eventHandler = handler

  private def handleLogEvent(evt: GameEvent): Unit = try {
    debug(evt)
    eventHandler.lift(evt).getOrElse(debug(s"Ignoring event $evt"))
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

  private def handleGameStartScreen() = {
    companionState.startMatch()
    if (VideoEncoder.canStartVideo()) {
      schedule(videoDelay)
      VideoEncoder.start()
    }
  }

  private def handleGameStart(heroChosen: HeroChosen): Unit = {
    val newClass = heroChosen.heroClass
    if (heroChosen.opponent) {
      companionState.opponentId1 = Some(heroChosen.player)
      updateMatch(_.withOpponentClass(newClass))
      hsPresenter.setOpponentClass(newClass)
      uiLog.info(s"Opponent class detected: $newClass")
    } else {
      companionState.playerId1 = Some(heroChosen.player)
      updateMatch(_.withUserClass(newClass))
      hsPresenter.setYourClass(newClass)
      uiLog.info(s"Your class detected: $newClass")
      if (enableDeckOverlay && companionState.mode != GameMode.ARENA) deckOverlay.startMonitoringCards(heroChosen.player)
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
    import companionState._
    val matchStart = // this is the first time the method is called for this match
      firstPlayerName.isEmpty && otherPlayerName.isEmpty
    if (matchStart) {
      companionState.startMatch()
      uiLog.info(s"Match start detected in log file")
      uiLog.divider()
      matchState.nextMatch(companionState)
    }

    val newName = if (first) {
      firstPlayerName = Some(name)
      true
    } else if (firstPlayerName != Some(name)) {
      otherPlayerName = Some(name)
      true
    } else false

    (newName, firstPlayerName, otherPlayerName) match {
      case (true, Some(firstName), Some(otherName)) =>
        val coin = if (otherName == userName.get) true
        else if (firstName == userName.get) false
        else chooseUserName(otherName, firstName) == 0
        playerHasCoinAtStart(coin)
      case _ => // wait for the other name to be defined, or ignore duplicate info
    }
  }

  private def chooseUserName(n1: String, n2: String): Int = {
    val choices = Array(n1, n2)
    val choice = hsPresenter.showOptionDialog(
      "Who are you ? \n(We only need to ask this once)",
      "Confirm your login",
      JOptionPane.DEFAULT_OPTION,
      choices.toArray[AnyRef])
    userName.set(choices(choice))
    choice
  }

  private def handleOpponentName(name: String): Unit = {
    updateMatch(_.withOpponentName(name))
    hsPresenter.setOpponentName(name)
    uiLog.info(s"Opponent name: $name")

  }

  private def playerHasCoinAtStart(coin: Boolean): Unit = {
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

  private def endGameImpl(playerName: String, outcome: MatchOutcome): Unit =
    if (hsMatch.opponentName != playerName) {
      updateMatch(_.withResult(outcome))
      updateMatch(_.withJsonLog(matchState.jsonGameLog.get))
      updateMatch(_.withDuration(companionState.currentDurationMs / 1000))
      matchUtils.submitMatchResult()
      deckOverlay.reset()
      companionState.reset()
      VideoEncoder.stop()
      schedule(normalDelay)
    }

  private def victoryOrDefeatDetected =
    matchState.currentMatch.flatMap(_.result).isDefined

  private def handleTurnChanged(turn: Int) {
    updateMatch(_.copy(numTurns = turn))
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
    def start(): Unit = if (recordVideo) {
        info("Video recording started")
        val videoEncoder = videoEncoderFactory.newInstance(false)
        companionState.ongoingVideo = Some(videoEncoder.newVideo(videoFps, videoWidth, videoHeight))
        videoEncoderActor ! EncodeAfter(0)
      } else {
        info("Video recording is disabled")
      }

    def stop(): Unit =
      videoEncoderActor ! StopRecording

    /**
     * @return true if a video can be started, false if a video is already being recorded or videos are disabled
     */
    def canStartVideo() =
      recordVideo && companionState.ongoingVideo.isEmpty

    val videoEncoderActor = actor(new Act {
      val encoding: Receive = LoggingReceive {
        case StopRecording =>
          become(stopped)
          info("Video recording has stopped")
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
                system.scheduler.scheduleOnce(videoDelay.millis, self, EncodeAfter(timeMs))
              }
            case None => system.scheduler.scheduleOnce(videoDelay.millis, self, EncodeAfter(timeMs))
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
