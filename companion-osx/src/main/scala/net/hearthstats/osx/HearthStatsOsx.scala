package net.hearthstats.osx

import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.Main
import net.hearthstats.config.UserConfig


/**
 * Main object for the OS X bundle, starts up the HearthStats Companion.
 */
object HearthStatsOsx extends TesseractSetup with App {

  val environment = new EnvironmentOsx
  val helper = new ProgramHelperOsx
  val config = wire[UserConfig]
  val main: Main = wire[Main]

  setupTesseract()
  main.start()

}
