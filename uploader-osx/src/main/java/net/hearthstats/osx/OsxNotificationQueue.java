package net.hearthstats.osx;

import net.hearthstats.osx.jna.NSUserNotification;
import net.hearthstats.osx.jna.NSUserNotificationCenter;
import net.hearthstats.ui.notification.NotificationQueue;
import org.rococoa.cocoa.foundation.NSAutoreleasePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsxNotificationQueue implements NotificationQueue {

    private final static Logger debugLog = LoggerFactory.getLogger(OsxNotificationQueue.class);

    @Override
    public void add(String header, String message, boolean allowFocus) {
        debugLog.debug("    Showing OS X notification \"{}\", \"{}\"", header, message );

        final NSAutoreleasePool pool = NSAutoreleasePool.new_();
        try {
            NSUserNotification nsUserNotification = NSUserNotification.CLASS.alloc();

            nsUserNotification.setTitle(header);
            nsUserNotification.setSubtitle(message);

            NSUserNotificationCenter defaultNotificationCenter = NSUserNotificationCenter.CLASS.defaultUserNotificationCenter();
            defaultNotificationCenter.deliverNotification(nsUserNotification);
        } finally {
            pool.drain();
        }

    }
}
