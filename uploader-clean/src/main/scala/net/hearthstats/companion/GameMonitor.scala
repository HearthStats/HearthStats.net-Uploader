package net.hearthstats.companion

import java.awt.image.BufferedImage
import scala.concurrent.duration.DurationInt
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.game.GameEvent
import net.hearthstats.game.Screen._
import net.hearthstats.game.ScreenEvent
import rx.lang.scala.Observable
import net.hearthstats.game.imageanalysis.LobbyAnalyser._
import net.hearthstats.core.GameMode._

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  companionState: CompanionState,
  imageToEvent: ImageToEvent) {

  val gameImages: Observable[BufferedImage] =
    Observable.interval(config.pollingDelayMs.get.millis).map { _ =>
      if (programHelper.foundProgram)
        Some(programHelper.getScreenCapture)
      else
        None
    }.filter(_.isDefined).map(_.get)

  val gameEvents: Observable[GameEvent] = gameImages.
    map(imageToEvent.eventFromImage).
    filter(_.isDefined).
    map(_.get)

  gameEvents.subscribe(handleGameEvent _)

  private def handleGameEvent(evt: GameEvent): Unit = evt match {
    case s: ScreenEvent => handleScreenEvent(s)
  }

  private def handleScreenEvent(evt: ScreenEvent) = evt.screen match {
    case PLAY_LOBBY =>
      companionState.mode match {
        case (Some(RANKED) | None) if imageShowsCasualPlaySelected(evt.image) =>
          companionState.mode=Some(CASUAL)
        case (Some(CASUAL) | None) if imageShowsRankedPlaySelected(evt.image) =>
          companionState.mode=Some(RANKED)
      }

  }
}