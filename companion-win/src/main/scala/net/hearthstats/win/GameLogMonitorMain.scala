package net.hearthstats.win

import net.hearthstats.ui.log.Log
import com.softwaremill.macwire.MacwireMacros.wire
import org.mockito.Mockito.mock
import net.hearthstats.util.FileObserver
import net.hearthstats.game.LogParser
import java.io.File
import net.hearthstats.game.HearthstoneLogMonitor
import rx.schedulers.Schedulers

object GameLogMonitorMain extends App {
  val environment = new EnvironmentWin
  val uiLog = mock(classOf[Log])
  val file = new File(environment.hearthstoneLogFile)
  file.delete()
  file.createNewFile()
  val fileObserver = wire[FileObserver]
  val logParser = wire[LogParser]
  val monitor = wire[HearthstoneLogMonitor]

  monitor.gameEvents.toBlockingObservable.foreach(e => println(e))
}