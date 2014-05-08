package jna.osx;

import com.sun.jna.Pointer;
import org.rococoa.ID;
import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;

/**
 * @author gtch
 */
public abstract class NSBitmapImageRep extends NSImageRep {

	public static final _Class CLASS = Rococoa.createClass("NSBitmapImageRep",
			_Class.class);

	public interface _Class extends ObjCClass {
		NSBitmapImageRep alloc();
	}

	public abstract NSBitmapImageRep initWithCGImage(ID cgImage);
	public abstract Pointer bitmapData();
	public abstract int samplesPerPixel();
	public abstract int bitsPerPixel();
	public abstract int bytesPerRow();
	public abstract int bytesPerPlane();
	public abstract int numberOfPlanes();
	public abstract int bitmapFormat();

	public static interface NSBitmapFormat {
		// / 0 means is alpha last (RGBA, CMYKA, etc.)
		public static final int NSAlphaFirstBitmapFormat = 1 << 0;
		// / 0 means is premultiplied
		public static final int NSAlphaNonpremultipliedBitmapFormat = 1 << 1;
		// / 0 is integer
		public static final int NSFloatingPointSamplesBitmapFormat = 1 << 2;
	}

}
