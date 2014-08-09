package net.hearthstats.companion

import java.awt.image.BufferedImage
import scala.concurrent.duration.DurationInt
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.core.GameMode._
import net.hearthstats.game.GameEvent
import net.hearthstats.game.Screen._
import net.hearthstats.game.ScreenEvent
import net.hearthstats.game.imageanalysis.LobbyAnalyser
import rx.lang.scala.Observable
import akka.actor.ActorSystem
import net.hearthstats.AkkaSystem
import akka.actor.Props
import akka.actor.Actor
import akka.actor.Scheduler
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.Duration.Zero
import net.hearthstats.game.imageanalysis.Casual
import net.hearthstats.game.imageanalysis.Ranked
import net.hearthstats.game.ScreenGroup

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  companionState: CompanionState,
  lobbyAnalyser: LobbyAnalyser,
  imageToEvent: ImageToEvent) extends AkkaSystem with Logging {

  import lobbyAnalyser._

  actorSystem.scheduler.schedule(Zero, config.pollingDelayMs.get.millis) {
    if (programHelper.foundProgram) {
      val img = programHelper.getScreenCapture
      val evt = imageToEvent.eventFromImage(img)
      evt map handleGameEvent
    }
  }

  private def handleGameEvent(evt: GameEvent): Unit = {
    debug(evt)
    evt match {
      case s: ScreenEvent => handleScreenEvent(s)
    }
  }

  private def handleScreenEvent(evt: ScreenEvent) = {
    evt.screen match {
      case PLAY_LOBBY =>
        handlePlayLobby(evt)

      case PRACTICE_LOBBY if companionState.mode != Some(PRACTICE) =>
        info("Practice Mode detected")
        companionState.mode = Some(PRACTICE)

      case VERSUS_LOBBY if companionState.mode != Some(FRIENDLY) =>
        info("Versus Mode detected")
        companionState.mode = Some(FRIENDLY)

      case ARENA_LOBBY if companionState.mode != Some(ARENA) =>
        info("Arena Mode detected")
        companionState.mode = Some(ARENA)

      case _ =>
        debug("no change in game mode")
    }
    if (evt.screen.group == ScreenGroup.PLAY) {
      val deckSlot = imageIdentifyDeckSlot(evt.image)
      if (deckSlot.isDefined && deckSlot != companionState.deckSlot) {
        info(s"deck ${deckSlot.get} detected")
        companionState.deckSlot = deckSlot
      }
    }
  }

  private def handlePlayLobby(evt: ScreenEvent): Unit = {
    mode(evt.image) match {
      case Some(Casual) if companionState.mode != Some(CASUAL) =>
        info("Casual Mode detected")
        companionState.mode = Some(CASUAL)
      case Some(Ranked) if companionState.mode != Some(RANKED) =>
        info("Ranked Mode detected")
        companionState.mode = Some(RANKED)
      case _ => // assuming no change in the mode
    }
    if (companionState.mode == Some(RANKED) && companionState.rank.isEmpty) {
      companionState.rank = lobbyAnalyser.analyzeRankLevel(evt.image)
    }
  }

}