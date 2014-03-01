package jna.osx;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author gtch
 */
public class CFDictionaryRef extends PointerType {
    public CFDictionaryRef(Pointer pointer) {
        super(pointer);
    }
    public CFDictionaryRef() {
        super();
    }
}
