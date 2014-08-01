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

  private def handleGameEvent(evt: GameEvent): Unit = evt match {
    case s: ScreenEvent => handleScreenEvent(s)
  }

  private def handleScreenEvent(evt: ScreenEvent) = evt.screen match {
    case PLAY_LOBBY =>
      companionState.mode match {
        case (Some(RANKED) | None) if imageShowsCasualPlaySelected(evt.image) =>
          debug("Casual Mode detected")
          companionState.mode = Some(CASUAL)
        case (Some(CASUAL) | None) if imageShowsRankedPlaySelected(evt.image) =>
          debug("Ranked Mode detected")
          companionState.mode = Some(RANKED)
        case _ => // assuming no change in the mode
      }

  }
}