package net.hearthstats.osx.jna;

import org.rococoa.NSClass;
import org.rococoa.NSObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSDate;

/**
 * @author gtch
 */
public interface NSUserNotification extends NSObject {
    public static final _Class CLASS = Rococoa.createClass("NSUserNotification", _Class.class);

    public interface _Class extends NSClass {
        NSUserNotification alloc();
    }


    String title();
    String subtitle();
    String informativeText();
    NSDate deliveryDate();

    void setTitle(String title);
    void setSubtitle(String subtitle);

}
