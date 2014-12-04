package net.hearthstats.osx;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import net.hearthstats.ProgramHelper;
import net.hearthstats.osx.jna.*;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSArray;
import org.rococoa.cocoa.foundation.NSAutoreleasePool;
import org.rococoa.cocoa.foundation.NSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Implementation of {@link ProgramHelper} for (Mac) OS X.
 *
 * @author gtch
 */
public class ProgramHelperOsx extends ProgramHelper {

    private final static Logger debugLog = LoggerFactory.getLogger(ProgramHelperOsx.class);

    private final String _bundleIdentifier = "unity.Blizzard Entertainment.Hearthstone";

    private int _pid;
    private int _windowId;

    private boolean _warningWindowSize = false;


    public ProgramHelperOsx() {
        debugLog.debug("Initialising ProgramHelperOsx with {}", _bundleIdentifier);
    }


    @Override
    public BufferedImage getScreenCaptureNative() {
        final NSAutoreleasePool pool = NSAutoreleasePool.new_();
        try {

            if (_windowId > 0) {
                // We already know the ID of the program window.
                BufferedImage image = getWindowImage(_windowId);
                if (image == null) {
                    // We seem to have lost the window?
                    debugLog.debug("    Window not found, resetting _windowId and _pid to 0");
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
                    debugLog.debug("    Window not found, resetting _pid to 0");
                    _pid = 0;

                } else {
                    // The program was found, so take an image
                    debugLog.debug("    Window found, setting _windowId to {}", _windowId);
                    BufferedImage image = getWindowImage(_windowId);
                    return image;
                }
            }

        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            throw new RuntimeException("Unable to create screen capture for  " + _bundleIdentifier + " due to exception", ex);
        } finally {
            pool.drain();
        }

        return null;
    }


    /**
     * <p>Gets a copy of the Hearthstone window with the provided window ID as an in-memory image.</p>
     *
     * @param windowId The window ID of Hearthstone, as reported by a call to Quartz Window Services
     * @return An image of the window, or null if the window doesn't exist or is too small to be an active window.
     */
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
            ID imageRef = CoreGraphicsLibrary.INSTANCE.CGWindowListCreateImage(bounds, CoreGraphicsLibrary.kCGWindowListOptionIncludingWindow | CoreGraphicsLibrary.kCGWindowListExcludeDesktopElements, windowId, CoreGraphicsLibrary.kCGWindowImageBoundsIgnoreFraming | CoreGraphicsLibrary.kCGWindowImageNominalResolution);

            // Convert the screenshot into a more useful ImageRep object, and retain the object so that it isn't lost before we extract the image data
            NSBitmapImageRep imageRep = NSBitmapImageRep.CLASS.alloc().initWithCGImage(imageRef).initWithCGImage(imageRef);
            imageRep.retain();

            int width = imageRep.pixelsWide();
            int height = imageRep.pixelsHigh();

            int windowTitleHeight = determineWindowTitleHeight(height, width);

            if (debugLog.isTraceEnabled()) {
                debugLog.trace("    Window height={} width={} titleHeight={}", new Object[] { height, width, windowTitleHeight});
            }

            int heightWithoutTitle = height - windowTitleHeight;

            Pointer bitmapPointer = imageRep.bitmapData();
            if (bitmapPointer == null || bitmapPointer == Pointer.NULL) {
                imageRep.release();
                return null;

            } else {
                int[] data = bitmapPointer.getIntArray(0, width * height);

                if (heightWithoutTitle > 512) {
                    BufferedImage image = new BufferedImage(width, heightWithoutTitle, BufferedImage.TYPE_INT_RGB);

                    // Start on row windowTitleHeight to exclude the window titlebar
                    int idx = windowTitleHeight * width;

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


    /**
     * <p>Determine whether there is a window title on a window given its size, and return the height of that window title.</p>
     * <p>The technique is quite naive but fast, based on the known screen sizes that Hearthstone supports with backup calculation
     * if the standard sizes aren't detected.</p>
     *
     * @param height Height of the Hearthstone window, in pixels.
     * @param width Width of the Hearthstone window, in pixels.
     * @return What the height of the titlebar on the window is likely to be, in pixels. Will be zero in full-screen modes.
     */
    private int determineWindowTitleHeight(int height, int width) {

        switch (width) {
            case 1:
                if (height == 1) {
                    return 0;               // Hearthstone has a brief period of its window being 1x1 when changing screen size
                }
                break;

            case 1024:
                if (height == 768) {
                    return 0;               // Full-screen 1024x768
                } else if (height == 790) {
                    return 22;              // Windowed 1024x768
                } else if (height == 742) {
                    return 22;              // Windowed 2048x1536 on Retina display, scaled
                }
                break;                      // Unknown

            case 1200:
                if (height == 742) {
                    return 22;              // Windowed 2560x1600 on Retina display, scaled
                }
                break;                      // Unknown

            case 1280:
                if (height == 720) {
                    return 0;               // Full-screen 1280x720
                } else if (height == 742) {
                    return 22;              // Windowed 1280x720
                } else if (height == 800) {
                    return 0;               // Full-screen 1280x800 or 2560x1600 on Retina display, scaled
                } else if (height == 822) {
                    return 22;              // Windowed 1280x800
                } else if (height == 1024) {
                    return 0;               // Full-screen 1280x1024
                } else if (height == 1046) {
                    return 22;              // Windowed 1280x1024
                }
                break;                      // Unknown

            case 1344:
                if (height == 756) {
                    return 0;               // Full-screen 1344x756
                } else if (height == 778) {
                    return 22;              // Windowed 1344x756
                }
                break;                      // Unknown

            case 1440:
                if (height == 900) {
                    return 0;               // Full-screen 1440x900 or 2880x1800 on Retina display, scaled
                }
                break;                      // Unknown

            case 1600:
                if (height == 900) {
                    return 0;               // Full-screen 1600x900
                } else if (height == 922) {
                    return 22;              // Windowed 1600x900
                } else if (height == 1200) {
                    return 0;               // Full-screen 1600x1200
                } else if (height == 1222) {
                    return 22;              // Windowed 1600x1200
                }
                break;                      // Unknown

            case 1680:
                if (height == 1050) {
                    return 0;               // Full-screen 1680x1050
                } else if (height == 1072) {
                    return 22;              // Windowed 1680x1050
                }
                break;                      // Unknown

            case 1920:
                if (height == 1080) {
                    return 0;               // Full-screen 1920x1080
                } else if (height == 1102) {
                    return 22;              // Windowed 1920x1080
                } else if (height == 1200) {
                    return 0;               // Full-screen 1920x1200
                } else if (height == 1222) {
                    return 22;              // Windowed 1920x1200
                }
                break;                      // Unknown

            case 2560:
                if (height == 1440) {
                    return 0;               // Full-screen 2560x1440
                } else if (height == 1462) {
                    return 22;              // Windowed 2560x1440
                }
                break;                      // Unknown

        }

        if (!_warningWindowSize) {
            // This is an unknown window size and we haven't logged a warning yet
            debugLog.debug("Encountered unknown window size {}x{} - may not be able to correctly determine whether this window is full-screen.", width, height);
            _warningWindowSize = true;
        }

        // This is an unknown display size. However we can guess whether it is full-screen
        // because most displays have a ratio that is an even multiple of 1/48th.

        int widthBy48 = width * 48;
        if (widthBy48 % height == 0) {
            // The ratio is an even multiple of 1/48th so assume it's a full-screen window
            return 0;
        } else {
            // Unknown size, so give up but assume the typical height of an OS X title bar: 22 pixels!
            // Though the native height on a Retina display is 44 pixels, our image is scaled so it is still effectively 22 pixels on a Retina display.
            return 22;
        }

    }


    /**
     * <p>Finds the main Hearthstone window ID for the given process ID.</p>
     * <p>Will only return the window that matches expected characteristics of the main Hearthstone window, namely:</p>
     * <ul>
     *     <li>kCGWindowIsOnscreen = 1</li>
     *     <li>kCGWindowLayer = 0</li>
     *     <li>kCGWindowOwnerPID = [pid]</li>
     * </ul>
     *
     * @param pid The process ID of Hearthstone.
     * @return the window ID if found, or zero if no suitable window was found. It is normal for the window ID to be zero briefly during startup of Hearthstone.
     */
    private int findWindow(int pid) {
        final NSAutoreleasePool pool = NSAutoreleasePool.new_();
        try {

            // Obtain a dictionary of all on-screen windows from Quartz Window Services, which will include all running applications.
            // Hearthstone typically has five or six windows, but only one or two are 'on screen' and it is those that we are interested in.
            final CFArrayRef originalArray = CoreGraphicsLibrary.INSTANCE.CGWindowListCopyWindowInfo(CGWindow.kCGWindowListExcludeDesktopElements | CGWindow.kCGWindowListOptionOnScreenOnly, 0);

            long count = CoreFoundationLibrary.INSTANCE.CFArrayGetCount(originalArray);
            for (long i = 0; i < count; i++) {

                // Obtain a CFDictionary containing this window's information dictionary
                Pointer pointer = CoreFoundationLibrary.INSTANCE.CFArrayGetValueAtIndex(originalArray, i);
                CFDictionaryRef dictionaryRef = new CFDictionaryRef(pointer);

                // Determine the process ID of this window
                NSString kCGWindowOwnerPID = CoreGraphicsLibrary.kCGWindowOwnerPID;
                Pointer pidPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowOwnerPID.id());
                NativeLongByReference longByReference = new NativeLongByReference();
                CoreFoundationLibrary.INSTANCE.CFNumberGetValue(pidPointer, CoreFoundationLibrary.CFNumberType.kCFNumberLongType, longByReference.getPointer());
                long pidLong = longByReference.getValue().longValue();

                if (pidLong == pid) {
                    // This window is a Hearthstone window

                    // When running in full-screen mode, Hearthstone has two windows: one for the game and one that appears to be a temporary desktop or space for the game to run in.
                    // The game window always has a kCGWindowLayer of zero, whereas the desktop has a non-zero kCGWindowLayer.
                    NSString kCGWindowLayer = CoreGraphicsLibrary.kCGWindowLayer;
                    Pointer windowLayerPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowLayer.id());
                    IntByReference windowLayerRef = new IntByReference();
                    CoreFoundationLibrary.INSTANCE.CFNumberGetValue(windowLayerPointer, CoreFoundationLibrary.CFNumberType.kCFNumberFloatType, windowLayerRef.getPointer());
                    int windowLayer = windowLayerRef.getValue();

                    if (windowLayer == 0) {
                        // This window has a zero kCGWindowLayer so it must be the main Hearthstone window

                        NSString kCGWindowNumber = CoreGraphicsLibrary.kCGWindowNumber;
                        Pointer windowNumberPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowNumber.id());
                        IntByReference windowIdRef = new IntByReference();
                        CoreFoundationLibrary.INSTANCE.CFNumberGetValue(windowNumberPointer, CoreFoundationLibrary.CFNumberType.kCFNumberIntType, windowIdRef.getPointer());
                        int windowId = windowIdRef.getValue();

                        return windowId;
                    }
                }
            }

            // No Hearthstone window was found
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
            debugLog.debug("    _pid has changed from {} to {}", _pid, newPid);
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

	@Override
	public Rectangle getHSWindowBounds() {

    final NSAutoreleasePool pool = NSAutoreleasePool.new_();
    try {

      // CGWindowListCreateDescriptionFromArray would be more efficient than the loop below,\
      // but isn't working... commented-out until can be fixed.
//      final Pointer[] values = { new IntByReference(_windowId).getPointer() };
//      CFArrayRef windowArray = CoreFoundationLibrary.INSTANCE.CFArrayCreate(null, values, 1, null);
//      final CFArrayRef descriptionArray = CoreGraphicsLibrary.INSTANCE.CGWindowListCreateDescriptionFromArray(windowArray);
//      long count = CoreFoundationLibrary.INSTANCE.CFArrayGetCount(descriptionArray);

      if (_pid == 0) {
        _pid = findProgramPid(_bundleIdentifier);
      }

      if (_windowId == 0) {
        _windowId = findWindow(_pid);
      }
      // Instead, obtain a dictionary of all on-screen windows from Quartz Window Services, which will include all running applications.
      CFArrayRef originalArray = CoreGraphicsLibrary.INSTANCE.CGWindowListCopyWindowInfo(CGWindow.kCGWindowListExcludeDesktopElements | CGWindow.kCGWindowListOptionOnScreenOnly, 0);

      long count = CoreFoundationLibrary.INSTANCE.CFArrayGetCount(originalArray);
      for (long i = 0; i < count; i++) {

        // Obtain a CFDictionary containing this window's information dictionary
        Pointer pointer = CoreFoundationLibrary.INSTANCE.CFArrayGetValueAtIndex(originalArray, i);
        CFDictionaryRef dictionaryRef = new CFDictionaryRef(pointer);

        // Determine the ID of this window
        NSString kCGWindowNumber = CoreGraphicsLibrary.kCGWindowNumber;
        Pointer windowNumberPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowNumber.id());
        IntByReference windowIdRef = new IntByReference();
        CoreFoundationLibrary.INSTANCE.CFNumberGetValue(windowNumberPointer, CoreFoundationLibrary.CFNumberType.kCFNumberIntType, windowIdRef.getPointer());
        int thisWindowId = windowIdRef.getValue();

        if (thisWindowId == _windowId) {

          // Determine the bounds of this window
          NSString kCGWindowBounds = CoreGraphicsLibrary.kCGWindowBounds;
          Pointer boundPointer = CoreFoundationLibrary.INSTANCE.CFDictionaryGetValue(dictionaryRef, kCGWindowBounds.id());

          CoreGraphicsLibrary.CGRectRef rect = new CoreGraphicsLibrary.CGRectRef();
          boolean result = CoreGraphicsLibrary.INSTANCE.CGRectMakeWithDictionaryRepresentation(boundPointer, rect);

          int x = (int) rect.origin.x;
          int y = (int) rect.origin.y;
          int width = (int) rect.size.width;
          int height = (int) rect.size.height;

          // Determine height of the title bar, if present
          int titleHeight = determineWindowTitleHeight(height, width);

          debugLog.debug("Found Hearthstone window at x={} y={} width={} height={} title={}", x, y, width, height, titleHeight);

//          x = x + titleHeight;
//          height = height - titleHeight;

          return new Rectangle(x, y, width, height);
        }
      }

    } finally {
      pool.drain();
    }

    // Couldn't find the Hearthstone window so return null... this will break the calling code.
    debugLog.warn("Unable to find position of Hearthstone window.");
    return null;
	}


  @Override
  public boolean bringWindowToForeground() {
    final NSAutoreleasePool pool;
    try {
      pool = NSAutoreleasePool.new_();
    } catch (Throwable ex) {
      ex.printStackTrace(System.err);
      throw new RuntimeException("Unable to find program " + _bundleIdentifier + " due to exception", ex);
    }
    try {
      final NSArray nsArray = NSRunningApplication.CLASS.runningApplicationsWithBundleIdentifier(_bundleIdentifier);
      final int size = nsArray.count();
      for (int i = 0; i < size; i++) {
        final NSRunningApplication nsRunningApplication = Rococoa.cast(nsArray.objectAtIndex(i), NSRunningApplication.class);

        // This double-check of the bundle identifier is probably unnecessary...
        if (_bundleIdentifier.equals(nsRunningApplication.bundleIdentifier())) {
          boolean result = nsRunningApplication.activateWithOptions(0);
          debugLog.debug("nsRunningApplication.activateWithOptions returned {}", result);
          return result;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
      throw new RuntimeException("Unable to find program " + _bundleIdentifier + " due to exception", ex);
    } finally {
      pool.drain();
    }
    return false;
  }

}
