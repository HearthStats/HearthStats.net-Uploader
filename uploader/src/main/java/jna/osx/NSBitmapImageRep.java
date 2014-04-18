package jna.osx;

import com.sun.jna.Pointer;
import org.rococoa.ID;
import org.rococoa.NSClass;
import org.rococoa.Rococoa;

/**
 * @author gtch
 */
public interface NSBitmapImageRep extends NSImageRep {

    public static final _Class CLASS = Rococoa.createClass("NSBitmapImageRep", _Class.class);

    public interface _Class extends NSClass {
        NSBitmapImageRep alloc();
    }

    NSBitmapImageRep initWithCGImage(ID cgImage);

    Pointer bitmapData();

    int samplesPerPixel();
    int bitsPerPixel();
    int bytesPerRow();
    int bytesPerPlane();
    int numberOfPlanes();
    int bitmapFormat();

    public static interface NSBitmapFormat {
        /// 0 means is alpha last (RGBA, CMYKA, etc.)
        public static final int NSAlphaFirstBitmapFormat = 1 << 0;
        /// 0 means is premultiplied
        public static final int NSAlphaNonpremultipliedBitmapFormat = 1 << 1;
        /// 0 is integer
        public static final int NSFloatingPointSamplesBitmapFormat = 1 << 2;
    }


}
