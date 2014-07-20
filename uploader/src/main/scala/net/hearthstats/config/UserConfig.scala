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
class UserConfig extends Config with Logging {

  private val PreferencesRoot: String = "/net/hearthstats/companion"

  private val prefs: Preferences = Preferences.userRoot().node(PreferencesRoot)

  class BooleanPref(val key: String, val default: Boolean) {
    def get = prefs.getBoolean(key, default)
    def set(value: Boolean) = {
      info(s"Setting config $key to $value")
      prefs.putBoolean(key, value)
    }
  }

  class IntPref(val key: String, val default: Int) {
    def get = prefs.getInt(key, default)
    def set(value: Int) = {
      info(s"Setting config $key to $value")
      prefs.putInt(key, value)
    }
  }

  class EnumPref[T <: Enum[T]](val key: String, val default: T) {
    def get: T = {
      val stringValue = prefs.get(key, default.toString)
      try {
        Enum.valueOf(default.getClass.asInstanceOf[Class[T]], stringValue)
      }
      catch {
        case ex: Exception => {
          info(s"Unable to interpret value ${stringValue}, using default ${default} instead")
          default
        }
      }
    }
    def set(value: T) = {
      info(s"Setting config $key to $value")
      prefs.put(key, value.toString)
    }
  }


  val prefMonitoringMethod = new EnumPref[MonitoringMethod]("hs.monitoringmethod", MonitoringMethod.getDefault)
  def monitoringMethod: MonitoringMethod = prefMonitoringMethod.get
  def monitoringMethod_=(value: MonitoringMethod) = prefMonitoringMethod.set(value)

  val prefNotifyOverall = new BooleanPref("notify.overall", true)
  def notifyOverall: Boolean = prefNotifyOverall.get
  def notifyOverall_=(value: Boolean) = prefNotifyOverall.set(value)

  val prefNotifyHsFound = new BooleanPref("notify.hsfound", true)
  def notifyHsFound: Boolean = prefNotifyHsFound.get
  def notifyHsFound_=(value: Boolean) = prefNotifyHsFound.set(value)

  val prefNotifyHsClosed = new BooleanPref("notify.hsclosed", true)
  def notifyHsClosed: Boolean = prefNotifyHsClosed.get
  def notifyHsClosed_=(value: Boolean) = prefNotifyHsClosed.set(value)

  val prefNotifyScreen = new BooleanPref("notify.screen", true)
  def notifyScreen: Boolean = prefNotifyScreen.get
  def notifyScreen_=(value: Boolean) = prefNotifyScreen.set(value)

  val prefNotifyMode = new BooleanPref("notify.mode", true)
  def notifyMode: Boolean = prefNotifyMode.get
  def notifyMode_=(value: Boolean) = prefNotifyMode.set(value)

  val prefNotifyDeck = new BooleanPref("notify.deck", true)
  def notifyDeck: Boolean = prefNotifyDeck.get
  def notifyDeck_=(value: Boolean) = prefNotifyDeck.set(value)

  val prefNotifyTurn = new BooleanPref("notify.turn", true)
  def notifyTurn: Boolean = prefNotifyTurn.get
  def notifyTurn_=(value: Boolean) = prefNotifyTurn.set(value)

  val prefNotificationType = new EnumPref[NotificationType]("notify.osx", NotificationType.HEARTHSTATS)
  def notificationType: NotificationType = prefNotificationType.get
  def notificationType_=(value: NotificationType) = prefNotificationType.set(value)

  val prefWindowX = new IntPref("ui.window.x", 0)
  def windowX: Int = prefWindowX.get
  def windowX_=(value: Int) = prefWindowX.set(value)

  val prefWindowY = new IntPref("ui.window.y", 0)
  def windowY: Int = prefWindowY.get
  def windowY_=(value: Int) = prefWindowY.set(value)

  val prefWindowHeight = new IntPref("ui.window.height", 700)
  def windowHeight: Int = prefWindowHeight.get
  def windowHeight_=(value: Int) = prefWindowHeight.set(value)

  val prefWindowWidth = new IntPref("ui.window.width", 600)
  def windowWidth: Int = prefWindowWidth.get
  def windowWidth_=(value: Int) = prefWindowWidth.set(value)

  val prefDeckX = new IntPref("ui.deck.x", 0)
  def deckX: Int = prefDeckX.get
  def deckX_=(value: Int) = prefDeckX.set(value)

  val prefDeckY = new IntPref("ui.deck.y", 0)
  def deckY: Int = prefDeckY.get
  def deckY_=(value: Int) = prefDeckY.set(value)

  val prefDeckHeight = new IntPref("ui.deck.height", 600)
  def deckHeight: Int = prefDeckHeight.get
  def deckHeight_=(value: Int) = prefDeckHeight.set(value)

  val prefDeckWidth = new IntPref("ui.deck.width", 485)
  def deckWidth: Int = prefDeckWidth.get
  def deckWidth_=(value: Int) = prefDeckWidth.set(value)

}
