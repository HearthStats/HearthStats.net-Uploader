package jna.osx;

import org.rococoa.NSClass;
import org.rococoa.NSObject;
import org.rococoa.Rococoa;

/**
 * @author gtch
 */
public interface CGWindow extends NSObject {

    public static final _Class CLASS = Rococoa.createClass("CGWindow", _Class.class);

    /** List all windows in this user session, including both on- and
       off-screen windows. The parameter `relativeToWindow' should be
       `kCGNullWindowID'. */
    public static final int kCGWindowListOptionAll = 0;

    /* List all on-screen windows in this user session, ordered from front to
    back. The parameter `relativeToWindow' should be `kCGNullWindowID'. */
    public static final int kCGWindowListOptionOnScreenOnly = (1 << 0);

    /* List all on-screen windows above the window specified by
    `relativeToWindow', ordered from front to back. */
    public static final int kCGWindowListOptionOnScreenAboveWindow = (1 << 1);

    /* List all on-screen windows below the window specified by
    `relativeToWindow', ordered from front to back. */
    public static final int kCGWindowListOptionOnScreenBelowWindow = (1 << 2);

    /* Include the window specified by `relativeToWindow' in any list,
    effectively creating `at-or-above' or `at-or-below' lists. */
    public static final int kCGWindowListOptionIncludingWindow = (1 << 3);

    /* Exclude any windows from the list that are elements of the desktop. */
    public static final int kCGWindowListExcludeDesktopElements = (1 << 4);


    public interface _Class extends NSClass {
//        NSObject CGWindowListCopyWindowInfo(int option, int relativeToWindow);
    }

}
