package net.hearthstats.osx.jna;

import org.rococoa.NSClass;
import org.rococoa.NSObject;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSDate;

/**
 * @author gtch
 */
public interface NSRunningApplication extends NSObject {

    public static final _Class CLASS = Rococoa.createClass("NSRunningApplication", _Class.class);

    public interface _Class extends NSClass {
        NSArray runningApplicationsWithBundleIdentifier(String bundleIdentifier);
        NSArray runningApplicationWithProcessIdentifier(int pid);
    }

    String bundleIdentifier();
    NSDate launchDate();
    int processIdentifier();


}
