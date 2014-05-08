package jna.osx;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSDate;

/**
 * @author gtch
 */
public interface NSRunningApplication extends ObjCObject {

    public static final _Class CLASS = Rococoa.createClass("NSRunningApplication", _Class.class);

    public interface _Class extends ObjCClass {
        NSArray runningApplicationsWithBundleIdentifier(String bundleIdentifier);
        NSArray runningApplicationWithProcessIdentifier(int pid);
    }

    String bundleIdentifier();
    NSDate launchDate();
    int processIdentifier();


}
