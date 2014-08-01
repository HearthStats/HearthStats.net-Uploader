package net.hearthstats.companion

import net.hearthstats.ProgramHelper
import rx.lang.scala._
import scala.concurrent.duration._
import net.hearthstats.config.UserConfig
import java.awt.image.BufferedImage
import net.hearthstats.game.GameEvent

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  companionState: CompanionState) {

  lazy val gameEvents :Observable[GameEvent]= gameImages.
    map(companionState.eventFromImage(_)).
    filter(_.isDefined).
    map(_.get)

  lazy val gameImages: Observable[BufferedImage] =
    Observable.interval(config.pollingDelayMs.get.millis).map { _ =>
      if (programHelper.foundProgram)
        Some(programHelper.getScreenCapture)
      else
        None
    }.filter(_.isDefined).map(_.get)
}