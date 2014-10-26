package net.hearthstats.ui

/**
 * Simple test app that displays the Deck Export popup so that you can test the UI.
 */
object ExportDeckBoxMain extends App {

  val monitor = new Monitor(EnvironmentTest)

  // Change this screen to test different modes of the popup
  HearthstoneAnalyser.screen = Screen.MAIN

  ExportDeckBox.open(monitor)

}
