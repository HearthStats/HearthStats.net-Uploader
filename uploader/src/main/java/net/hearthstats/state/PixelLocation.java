package net.hearthstats.state;

/**
 * The standard pixel locations that are checked to identify screens.
 * Any screen can be identified by checking only these pixels.
 */
public enum PixelLocation {

    /**
     * The screen size that was used to calculate the pixel positions shown below. If playing Hearthstone on a
     * different screen size, all the pixel locations have adjusted appropriately relative to this size.
     */
    REFERENCE_SIZE (1600, 1200),

    A ( 827,   35),
    B (1271,    6),
    C (1592,    6),
    D (1595,   66),
    E ( 552,  182),
    F ( 649,  242),
    G ( 772,  557),
    H ( 927,  675),
    I (1510,  529),
    J (1510,  556),
    K ( 104,  889),
    L ( 178, 1105),
    M ( 341, 1070),
    N ( 713, 1070),
    O ( 820,  931),
    P (1010,  896),
    Q (1255, 1117),
    R (1594,  904);


    public final int x;
    public final int y;

    PixelLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
