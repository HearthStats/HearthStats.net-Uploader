package jna.osx;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSDate;

/**
 * @author gtch
 */
public interface NSUserNotification extends ObjCObject {
    public static final _Class CLASS = Rococoa.createClass("ObjCUserNotification", _Class.class);

    public interface _Class extends ObjCClass {
        NSUserNotification alloc();
    }


    String title();
    String subtitle();
    String informativeText();
    NSDate deliveryDate();

    void setTitle(String title);
    void setSubtitle(String subtitle);

}
