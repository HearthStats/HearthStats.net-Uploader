package net.hearthstats.config

trait Config {

  var monitoringMethod: MonitoringMethod

  var notifyOverall: Boolean
  var notifyHsFound: Boolean
  var notifyHsClosed: Boolean
  var notifyScreen: Boolean
  var notifyMode: Boolean
  var notifyDeck: Boolean
  var notifyTurn: Boolean
  var notificationType: NotificationType

  var windowX: Int
  var windowY: Int
  var windowWidth: Int
  var windowHeight: Int
  var deckX: Int
  var deckY: Int
  var deckWidth: Int
  var deckHeight: Int

}
