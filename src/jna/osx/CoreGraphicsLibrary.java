package jna.osx;

import com.sun.jna.*;
import org.rococoa.ID;
import org.rococoa.cocoa.foundation.NSString;

import java.util.Arrays;
import java.util.List;

/**
 * @author gtch
 */
public interface CoreGraphicsLibrary extends Library {

    public static final CoreGraphicsLibrary INSTANCE = (CoreGraphicsLibrary) Native.loadLibrary("CoreGraphics", CoreGraphicsLibrary.class);


    ID CGWindowListCreateImage(CGRect screenBounds, int windowOption, int windowId, int imageOption);

    CFArrayRef CGWindowListCopyWindowInfo(int option, int relativeToWindow);

    void CGImageRelease(CGImage image);
    boolean CGImageIsMask(CGImage image);
    long CGImageGetWidth(CGImage image);
    long CGImageGetHeight(CGImage image);
    long CGImageGetBitsPerComponent(CGImage image);
    long CGImageGetBitsPerPixel(CGImage image);
    long CGImageGetBytesPerRow(CGImage image);


    final static NSString kCGWindowNumber =  NSString.getGlobalString("kCGWindowNumber");
    final static NSString kCGWindowOwnerPID =  NSString.getGlobalString("kCGWindowOwnerPID");
    final static NSString kCGWindowBounds =  NSString.getGlobalString("kCGWindowBounds");
    final static NSString kCGWindowSharingState =  NSString.getGlobalString("kCGWindowSharingState");
    final static NSString kCGWindowAlpha =  NSString.getGlobalString("kCGWindowAlpha");
    final static NSString kCGWindowLayer =  NSString.getGlobalString("kCGWindowLayer");
    final static NSString kCGWindowIsOnscreen =  NSString.getGlobalString("kCGWindowIsOnscreen");
    final static NSString kCGWindowName =  NSString.getGlobalString("kCGWindowName");

    public static final int kCGWindowListOptionAll = 0;
    public static final int kCGWindowListOptionOnScreenOnly = (1 << 0);
    public static final int kCGWindowListOptionOnScreenAboveWindow = (1 << 1);
    public static final int kCGWindowListOptionOnScreenBelowWindow = (1 << 2);
    public static final int kCGWindowListOptionIncludingWindow = (1 << 3);
    public static final int kCGWindowListExcludeDesktopElements = (1 << 4);

    public static final int kCGWindowImageDefault = 0;
    public static final int kCGWindowImageBoundsIgnoreFraming = (1 << 0);
    public static final int kCGWindowImageShouldBeOpaque = (1 << 1);
    public static final int kCGWindowImageOnlyShadows = (1 << 2);



    public static class __CFArray extends PointerType {
        public __CFArray(Pointer pointer) {
            super(pointer);
        }
        public __CFArray() {
            super();
        }
    }

    class CGPoint extends Structure {
        public double x;
        public double y;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("x", "y");
        }
    }

    class CGSize extends Structure {
        public double width;
        public double height;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("width", "height");
        }
    }

    class CGRect extends Structure implements Structure.ByValue {
        public static class CGRectByValue extends CGRect {
        }

        public CGPoint origin;
        public CGSize size;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("origin", "size");
        }
    }



}
