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

  public static interface NSApplicationActivationOptions {
    // all of the application's windows are brought forward.
    public static final int NSApplicationActivateAllWindows = 1 << 0;
    // the application is activated regardless of the currently active app, potentially stealing focus from the user
    public static final int NSApplicationActivateIgnoringOtherApps = 1 << 1;
  }

  String bundleIdentifier();
  NSDate launchDate();
  int processIdentifier();
  boolean activateWithOptions(int options);

}
