package net.hearthstats.util;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

/**
 * Factory for Google Analytics Tracker
 * 
 * @author tyrcho
 */
public final class AnalyticsTracker {
	// Singleton
	private AnalyticsTracker() {
	}

	public static JGoogleAnalyticsTracker tracker() {
		return new JGoogleAnalyticsTracker(new AnalyticsConfigData(
				"UA-45442103-3"), GoogleAnalyticsVersion.V_4_7_2);
	}
}
