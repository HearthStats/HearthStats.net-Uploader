package net.hearthstats.modules

import net.hearthstats.HearthstoneMatch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object ReplayHandlerMain extends App {
  val future = ReplayHandler.handleNewReplay("""C:\Users\tyrcho\hearthstats\videos\201407142352_Druid_VS_Warlock.mp4""",
    new HearthstoneMatch(userClass = "Druid", opponentClass = "Warlock"))
  future.onSuccess {
    case () => println("uploaded")
  }
  Await.ready(future, 10.minutes)
}