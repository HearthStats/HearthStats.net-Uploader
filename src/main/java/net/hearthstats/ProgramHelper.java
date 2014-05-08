package net.hearthstats;

import java.awt.image.BufferedImage;
import java.util.Observable;

/**
 * Abstract class that finds the Hearthstone program and takes screenshots of it.
 * A separate implementation of this class is needed for each operating system because the implementations rely on native system calls.
 */
public abstract class ProgramHelper extends Observable {

    /**
     * Is Hearthstone found?
     *
     * @return Whether or not the program is found
     */
    public abstract boolean foundProgram();

    /**
     * Takes a screenshot of the Hearthstone window.
     *
     * @return An image of the Hearthstone window, or null if not running or not available.
     */
    public abstract BufferedImage getScreenCapture();


    protected void _notifyObserversOfChangeTo(String property) {
        setChanged();
        notifyObservers(property);
    }
}

