package net.hearthstats.config

import java.util.prefs.Preferences

import grizzled.slf4j.Logging

/**
 * Stores and retrieves configuration for the current user by using the Java 7 Preferences API.
 * Config changes made are immediately stored by the operating system for the current user.
 *
 * This is the standard implementation of Config that it used when HearthStats Companion is being run as an app
 * (as opposed to unit tests which may use a different Config implementation).
 */
class UserConfigComponent extends ConfigComponent with Logging {
  val config = new UserConfig

  class UserConfig extends Config {
    val configApiBaseUrl = config("api.baseurl", "http://hearthstats.net/api/v1/")
    val configUserKey = config("api.userkey", "your_userkey_here")

    val enableAnalytics = config("enable.analytics", true)
    val enableDeckOverlay = config("enable.deckoverlay", false)
    val enableStartMin = config("enable.startmin", false)
    val enableMinToTray = config("enable.mintotray", true)
    val enableUpdateCheck = config("enable.updatecheck", true)

    val optionGameLanguage = config("option.gamelanguage", "en")
    val optionMatchPopup = enumConfig("option.matchpopup", MatchPopup.getDefault)
    val optionMonitoringMethod = enumConfig("option.monitoringmethod", MonitoringMethod.getDefault)
    val optionNotificationType = enumConfig("option.notificationtype", NotificationType.HEARTHSTATS)

    val notifyOverall = config("notify.overall", true)
    val notifyHsFound = config("notify.hsfound", true)
    val notifyHsClosed = config("notify.hsclosed", true)
    val notifyScreen = config("notify.screen", true)
    val notifyMode = config("notify.mode", true)
    val notifyDeck = config("notify.deck", true)
    val notifyTurn = config("notify.turn", true)

    val windowX = config("ui.window.x", 0)
    val windowY = config("ui.window.y", 0)
    val windowHeight = config("ui.window.height", 700)
    val windowWidth = config("ui.window.width", 600)

    val deckX = config("ui.deck.x", 0)
    val deckY = config("ui.deck.y", 0)
    val deckHeight = config("ui.deck.height", 600)
    val deckWidth = config("ui.deck.width", 485)
  }

  private val PreferencesRoot: String = "/net/hearthstats/companion"
  private val prefs: Preferences = Preferences.userRoot().node(PreferencesRoot)

  /**
   * Generates a new ConfigValue based on the key, the default value and
   * an implicit UserConfigStore (typeclass).
   */
  def config[T: UserConfigStore](key: String, default: T): ConfigValue[T] =
    new ConfigValue[T] {
      val store = implicitly[UserConfigStore[T]]

      def get = store.get(key, default)
      def set(value: T): Unit = {
        store.set(key, value)
      }
    }

  //We need an additional method for enums because implicit values 
  //cannot be genrated on the fly for any Enum[T]
  def enumConfig[T <: Enum[T]](key: String, default: T): ConfigValue[T] =
    config(key, default)(enum[T])

  //This is where the actual storage is done 
  abstract class UserConfigStore[T] {
    def get(key: String, default: T): T
    def set(key: String, value: T): Unit = {
      info(s"Setting config $key to $value")
      setImpl(key, value)
    }

    def setImpl(key: String, value: T): Unit
  }

  //After this are the different type class implementations of UserConfigStore[T]
  //for each T we are using 
  implicit val booleanPref: UserConfigStore[Boolean] = new UserConfigStore[Boolean] {
    def get(key: String, default: Boolean) =
      prefs.getBoolean(key, default)

    def setImpl(key: String, value: Boolean) =
      prefs.putBoolean(key, value)
  }

  implicit val intPref: UserConfigStore[Int] = new UserConfigStore[Int] {
    def get(key: String, default: Int) =
      prefs.getInt(key, default)

    def setImpl(key: String, value: Int) =
      prefs.putInt(key, value)
  }

  implicit val stringPref: UserConfigStore[String] = new UserConfigStore[String] {
    def get(key: String, default: String) =
      prefs.get(key, default)

    def setImpl(key: String, value: String) =
      prefs.put(key, value)
  }

  def enum[T <: Enum[T]] = new UserConfigStore[T] {
    def get(key: String, default: T) = {
      val stringValue = prefs.get(key, default.toString)
      try {
        val cl = default.getClass.asInstanceOf[Class[T]]
        Enum.valueOf(cl, stringValue)
      } catch {
        case ex: Exception => {
          warn(s"Unable to interpret value ${stringValue}, using default ${default} instead")
          default
        }
      }
    }

    def setImpl(key: String, value: T) =
      prefs.put(key, value.toString)
  }

}