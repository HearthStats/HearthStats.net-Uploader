package net.hearthstats.modules

import net.hearthstats.HearthstoneMatch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object ReplayHandlerMain extends App {
  val future = ReplayHandler.handleNewReplay("""C:\Users\tyrcho\AppData\Local\Temp\HSReplay578323316009031172video.mp4""",
    new HearthstoneMatch(_userClass = "Druid", opponentClass = "Warlock", result = "Win", opponentName = "opp"))
  future.onSuccess {
    case n => println(s"uploaded $n")
  }
  Await.ready(future, 10.minutes)
}