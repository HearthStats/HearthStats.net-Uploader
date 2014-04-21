package net.hearthstats.util;

/**
 * A simple class to represent an immutable X & Y coordinate on the screen.
 */
public class Coordinate {

    public final int x;
    public final int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "[x=" + x + ", y=" + y + "]";
    }
}
