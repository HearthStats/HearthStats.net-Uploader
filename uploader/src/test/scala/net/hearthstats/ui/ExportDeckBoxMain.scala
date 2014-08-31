package net.hearthstats.ui

import net.hearthstats.analysis.HearthstoneAnalyser
import net.hearthstats.state.Screen
import net.hearthstats.{Monitor, EnvironmentTest}

/**
 * Simple test app that displays the Deck Export popup so that you can test the UI.
 */
object ExportDeckBoxMain extends App {

  val monitor = new Monitor(EnvironmentTest)

  // Change this screen to test different modes of the popup
  HearthstoneAnalyser.screen = Screen.MAIN

  ExportDeckBox.open(monitor)

}
