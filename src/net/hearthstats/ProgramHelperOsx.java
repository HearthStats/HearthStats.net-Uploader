package net.hearthstats;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import jna.osx.*;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSAutoreleasePool;
import org.rococoa.cocoa.foundation.NSString;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Implementation of {@link ProgramHelper} for (Mac) OS X.
 *
 * @author gtch
 */
public class ProgramHelperOsx extends ProgramHelper {

    /**
     * The height of the title of an OS X window. This number of pixels are removed from the top of the screenshot
     * so that the screenshot doesn't contain the window title.
     */
    private final static int WINDOW_TITLE_HEIGHT = 22;

    private final String _bundleIdentifier;

    private int _pid;
    private int _windowId;


    public ProgramHelperOsx(String bundleIdentifier) {
        _bundleIdentifier = bundleIdentifier;
    }


    @Override
    public BufferedImage getScreenCapture() {
        final NSAutoreleasePool pool = NSAutoreleasePool.new_();
        try {

            if (_windowId > 0) {
                // We already know the ID of the program window.
                BufferedImage image = getWindowImage(_windowId);
                if (image == null) {
                    // We seem to have lost the window?
                    _notifyObserversOfChangeTo("Warning! Window " + _windowId + " could not be found. No detection possible.");
                    _windowId = 0;
                    _pid = 0;
                    return null;
                } else {
                    return image;
                }

            } else {
                // We don't know the ID of the program window, so look for it now.
                _windowId = findWindow(_pid);
                if (_windowId == 0) {
                    // The window couldn't be found, so maybe the program has been closed. Reset the pid to force it to start from the start
                    _pid = 0;

                } else {
                    // The program was found, so take an image
                    BufferedImage image = getWindowImage(_windowId);
                    System.out.println("* getScreenCapture() returning image for window " + _windowId);
                    return image;
                }
            }

            System.out.println("* getScreenCapture() did not find image for window " + _windowId);

        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            throw new RuntimeException("Unable to create screen capture for  " + _bundleIdentifier + " due to exception", ex);
        } finally {
            pool.drain();
        }

        return null;
    }

    private BufferedImage getWindowImage(int windowId) {
        final NSAutoreleasePool pool = NSAutoreleasePool.new_();
        try {
            // Create a CGRect with zero boundaries so that OS X automatically picks the correct size
            CoreGraphicsLibrary.CGRect bounds = new CoreGraphicsLibrary.CGRect.CGRectByValue();
            bounds.origin = new CoreGraphicsLibrary.CGPoint();
            bounds.origin.x = 0;
            bounds.origin.y = 0;
            bounds.size = new CoreGraphicsLibrary.CGSize();
            bounds.size.width = 0;
            bounds.size.height = 0;

            // Take a screenshot of the program window
            ID imageRef = CoreGraphicsLibrary.INSTANCE.CGWindowListCreateImage(bounds, CoreGraphicsLibrary.kCGWindowListOptionIncludingWindow | CoreGraphicsLibrary.kCGWindowListExcludeDesktopElements, windowId, CoreGraphicsLibrary.kCGWindowImageBoundsIgnoreFraming);

            // Convert the screenshot into a more useful ImageRep object, and retain the object so that it isn't lost before we extract the image data
            NSBitmapImageRep imageRep = NSBitmapImageRep.CLASS.alloc().initWithCGImage(imageRef).initWithCGImage(imageRef);
            imageRep.retain();

            int width = imageRep.pixelsWide();
            int height = imageRep.pixelsHigh();
            int heightWithoutTitle = height - WINDOW_TITLE_HEIGHT;

            int format = imageRep.bitmapFormat();

            Pointer bitmapPointer = imageRep.bitmapData();
            if (bitmapPointer == null || bitmapPointer == Pointer.NULL) {
                imageRep.release();
                return null;

            } else {
                int[] data = bitmapPointer.getIntArray(0, width * height);

                if (heightWithoutTitle > WINDOW_TITLE_HEIGHT) {
                    BufferedImage image = new BufferedImage(width, heightWithoutTitle, BufferedImage.TYPE_INT_RGB);

                    // Start on row WINDOW_TITLE_HEIGHT to exclude the window titlebar
                    int idx = WINDOW_TITLE_HEIGHT * width;

                    // Manually write each pixel to the raster because OS X generates ARGB screenshots but BufferedImage expects RGB data.
                    WritableRaster raster = image.getRaster();
                    for (int y = 0; y < heightWithoutTitle; y++) {

                        for (int x = 0; x < width; x++) {
                            int pixel = data[idx++];
                            raster.setSample(x, y, 0, pixel >> 8 & 0xFF);   // Red is the second byte
                            raster.setSample(x, y, 1, pixel >> 16 & 0xFF);  // Green is the third byte
                            raster.setSample(x, y, 2, pixel >> 24 & 0xFF);  // Blue is the fourth byte
                        }
                    }

                    // Now that we have a copy of the image in a Java object it's safe to release the native pointers
                    Foundation.cfRelease(imageRef);
                    imageRep.release();

                    return image;

                } else {
                    // The window is too small to generate an image
                    return null;
                }
            }

        } finally {
            pool.drain();
        }
    }

    private int findWindow(int pid) {
        final NSAutoreleasePool pool = NSAutoreleasePool.new_();
        try {

            final CFArrayRef originalArray = CoreGraphicsLibrary.INSTANCE.CGWindowListCopyWindowInfo(CGWindow.kCGWindowListExcludeDesktopElements, 0);

            long count = CoreFoundationLibrary.INSTANCE.CFArrayGetCount(originalArray);
            for (long i = 0; i < count; i++) {
                Pointer pointer = CoreFoundationLibrary.INSTANCE.CFArrayGetValueAtIndex(originalArray, i);

                CFDictionaryRef dictionaryRef = new CFDictionaryRef(pointer);
                NSString kCGWindowOwnerPID = CoreGraphicsLibrary.kCGWindowOwnerPID;
                Pointer pidPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowOwnerPID.id());

                NativeLongByReference longByReference = new NativeLongByReference();
                CoreFoundationLibrary.INSTANCE.CFNumberGetValue(pidPointer, CoreFoundationLibrary.CFNumberType.kCFNumberLongType, longByReference.getPointer());
                long pidLong = longByReference.getValue().longValue();

                if (pidLong == pid) {

                    NSString kCGWindowNumber = CoreGraphicsLibrary.kCGWindowNumber;
                    Pointer windowNumberPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowNumber.id());

                    if (windowNumberPointer != null) {
                        IntByReference windowIdRef = new IntByReference();
                        CoreFoundationLibrary.INSTANCE.CFNumberGetValue(windowNumberPointer, CoreFoundationLibrary.CFNumberType.kCFNumberIntType, windowIdRef.getPointer());
                        int windowId = windowIdRef.getValue();
                        return windowId;
                    }
                }
            }

            return 0;

        } finally {
            pool.drain();
        }
    }


    @Override
    public boolean foundProgram() {

        int newPid = findProgramPid(_bundleIdentifier);

        if (newPid != _pid) {
            // The process ID has changed, so reset the cached window ID (which was related to the old PID)
            _pid = newPid;
            _windowId = 0;
        }

        return _pid > 0;
    }



    /**
     * Looks for the program specified by {@link #_bundleIdentifier}, and if it finds it sets the {@link #_pid} to the process ID.
     * Resets the {@link #_pid} if the program could not be found (ie it's not running).
     */
    private int findProgramPid(String bundleIdentifier) {
        final NSAutoreleasePool pool;
        try {
            pool = NSAutoreleasePool.new_();
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            throw new RuntimeException("Unable to find program " + bundleIdentifier + " due to exception", ex);
        }
        try {
            final NSArray nsArray = NSRunningApplication.CLASS.runningApplicationsWithBundleIdentifier(bundleIdentifier);
            final int size = nsArray.count();
            for (int i = 0; i < size; i++) {
                final NSRunningApplication nsRunningApplication = Rococoa.cast(nsArray.objectAtIndex(i), NSRunningApplication.class);

                // This double-check of the bundle identifier is probably unnecessary...
                if (bundleIdentifier.equals(nsRunningApplication.bundleIdentifier())) {
                    // We've found the application, so we can skip the rest of the loop
                    return nsRunningApplication.processIdentifier();
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            throw new RuntimeException("Unable to find program " + bundleIdentifier + " due to exception", ex);
        } finally {
            pool.drain();
        }

        return 0;
    }

}
