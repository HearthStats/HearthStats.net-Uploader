package net.hearthstats.osx.jna;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author gtch
 */
public class CFArrayRef extends PointerType {
    public CFArrayRef(Pointer address) {
        super(address);
    }
    public CFArrayRef() {
        super();
    }
};