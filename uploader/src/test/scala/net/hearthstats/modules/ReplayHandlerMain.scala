package net.hearthstats.modules

import net.hearthstats.HearthstoneMatch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import net.hearthstats.util.MatchOutcome
import java.io.File

object ReplayHandlerMain extends App {
  val video = File.createTempFile("samplevideo", ".mp4").getAbsolutePath
  val hsMatch = new HearthstoneMatch(
    _userClass = "Druid",
    opponentClass = "Warlock",
    result = Some(MatchOutcome.VICTORY),
    opponentName = "opp")

  val future = ReplayHandler.handleNewReplay(video, hsMatch)
  future.onSuccess {
    case n => println(s"uploaded $n")
  }
  Await.ready(future, 10.minutes)
}