package net.hearthstats.win

import java.io._
import net.hearthstats.config.Environment
import javax.swing.{ JOptionPane, JLabel }
import grizzled.slf4j.Logging
import net.hearthstats.ui.HyperLinkHandler
import net.hearthstats.Main
import com.softwaremill.macwire.MacwireMacros._

/**
 * Main object for the Windows application, starts up the HearthStats Companion.
 */
object HearthStatsWin extends TesseractSetup with App {

  val environment = new EnvironmentWin
  val helper = new ProgramHelperWindows
  val main: Main = wire[Main]

  setupTesseract()
  main.start()

}