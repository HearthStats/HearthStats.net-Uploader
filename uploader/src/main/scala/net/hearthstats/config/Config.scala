package net.hearthstats.config

trait Config {

  def monitoringMethod: ConfigValue[MonitoringMethod]

  def notifyOverall: ConfigValue[Boolean]
  def notifyHsFound: ConfigValue[Boolean]
  def notifyHsClosed: ConfigValue[Boolean]
  def notifyScreen: ConfigValue[Boolean]
  def notifyMode: ConfigValue[Boolean]
  def notifyDeck: ConfigValue[Boolean]
  def notifyTurn: ConfigValue[Boolean]
  def notificationType: ConfigValue[NotificationType]

  def windowX: ConfigValue[Int]
  def windowY: ConfigValue[Int]
  def windowWidth: ConfigValue[Int]
  def windowHeight: ConfigValue[Int]

  def deckX: ConfigValue[Int]
  def deckY: ConfigValue[Int]
  def deckWidth: ConfigValue[Int]
  def deckHeight: ConfigValue[Int]

  implicit def getConfig[T](cfg: ConfigValue[T]) = cfg.get

}

trait ConfigValue[T] {
  def get: T
  def set(value: T): Unit
}
