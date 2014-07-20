package net.hearthstats.config

/**
 * Gives access to all configuration values in read/write mode.
 * Abstract so it can be easily overriden in tests.
 * See UserConfig for the actual implementation which stores on the disk.
 */
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

  // so we can use config.prop instead of config.prop.get in the code
  implicit def getConfig[T](cfg: ConfigValue[T]) = cfg.get

}

/**
 * Atomic read/write piece of configuration.
 */
trait ConfigValue[T] {
  def get: T
  def set(value: T): Unit
}
