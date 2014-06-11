package net.hearthstats.osx.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.rococoa.ID;


/**
 * @author gtch
 */
public interface CoreFoundationLibrary extends Library {

  public static final CoreFoundationLibrary INSTANCE = (CoreFoundationLibrary) Native.loadLibrary("CoreFoundation", CoreFoundationLibrary.class);


  /**
   * @param theArray The array to be queried. If this parameter is not a valid<br>
   *                 CFArray, the behavior is undefined.<br>
   * @function CFArrayGetCount<br>
   * Returns the number of values currently in the array.<br>
   * @result The number of values in the array.<br>
   * Original signature : <code>CFIndex CFArrayGetCount(CFArrayRef)</code>
   */
  long CFArrayGetCount(CFArrayRef theArray);

  /**
   * @param theArray The array to be queried. If this parameter is not a<br>
   *                 valid CFArray, the behavior is undefined.<br>
   * @param idx      The index of the value to retrieve. If the index is<br>
   *                 outside the index space of the array (0 to N-1 inclusive,<br>
   *                 where N is the count of the array), the behavior is<br>
   *                 undefined.<br>
   * @function CFArrayGetValueAtIndex<br>
   * Retrieves the value at the given index.<br>
   * @result The value with the given index in the array.<br>
   * Original signature : <code>void* CFArrayGetValueAtIndex(CFArrayRef, CFIndex)</code><br>
   * <i>native declaration : /System/Library/Frameworks/CoreFoundation.framework/Headers/CFArray.h:291</i>
   */
  Pointer CFArrayGetValueAtIndex(CFArrayRef theArray, long idx);


  NativeLong CFNumberGetByteSize(Pointer number);

  int CFNumberGetType(Pointer number);

  boolean CFNumberGetValue(Pointer number, int theType, Pointer valuePtr);

  public static interface CFNumberType {
    /// Fixed-width types
    public static final int kCFNumberSInt8Type = 1;
    public static final int kCFNumberSInt16Type = 2;
    public static final int kCFNumberSInt32Type = 3;
    public static final int kCFNumberSInt64Type = 4;
    public static final int kCFNumberFloat32Type = 5;
    /// 64-bit IEEE 754
    public static final int kCFNumberFloat64Type = 6;
    /// Basic C types
    public static final int kCFNumberCharType = 7;
    public static final int kCFNumberShortType = 8;
    public static final int kCFNumberIntType = 9;
    public static final int kCFNumberLongType = 10;
    public static final int kCFNumberLongLongType = 11;
    public static final int kCFNumberFloatType = 12;
    public static final int kCFNumberDoubleType = 13;
    /// Other
    public static final int kCFNumberCFIndexType = 14;
    public static final int kCFNumberNSIntegerType = 15;
    public static final int kCFNumberCGFloatType = 16;
    public static final int kCFNumberMaxType = 16;
  }


  NativeLong CFDictionaryGetCount(CFDictionaryRef cfDictionaryRef);

  NativeLong CFDictionaryGetCountOfKey(CFDictionaryRef theDict, Pointer key);

  NativeLong CFDictionaryGetCountOfValue(CFDictionaryRef theDict, Pointer value);

  boolean CFDictionaryContainsKey(CFDictionaryRef theDict, Pointer key);

  boolean CFDictionaryContainsValue(CFDictionaryRef theDict, Pointer value);

  Pointer CFDictionaryGetValue(CFDictionaryRef theDict, ID key);

  boolean CFDictionaryGetValueIfPresent(CFDictionaryRef theDict, Pointer key, PointerByReference value);

}
