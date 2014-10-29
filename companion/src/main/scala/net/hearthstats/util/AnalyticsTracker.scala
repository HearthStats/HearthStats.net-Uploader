package net.hearthstats.util

import com.dmurph.tracking.JGoogleAnalyticsTracker
import com.dmurph.tracking.AnalyticsConfigData
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion._

object AnalyticsTrackerFactory {
  def tracker(enabled: Boolean): Tracker =
    if (enabled) GoogleAnalyticsTracker
    else NoTracker
}

sealed trait Tracker {
  def trackEvent(category: String, action: String): Unit
}

object NoTracker extends Tracker {
  def trackEvent(category: String, action: String): Unit = {}
}

object GoogleAnalyticsTracker extends Tracker {
  val impl = new JGoogleAnalyticsTracker(new AnalyticsConfigData("UA-45442103-3"), V_4_7_2)

  def trackEvent(category: String, action: String): Unit = {
    impl.trackEvent(category, action)
  }
}