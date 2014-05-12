package jna.osx;

import org.rococoa.NSClass;
import org.rococoa.Rococoa;
import org.rococoa.NSObject;

/**
 * @author gtch
 */
public interface NSImageRep extends NSObject {

    public static final _Class CLASS = Rococoa.createClass("NSImageRep", _Class.class);

    public interface _Class extends NSClass {
    }

    /// Original signature : <code>-(NSSize)size</code>
    public abstract org.rococoa.cocoa.foundation.NSSize.ByValue size();
    /// Original signature : <code>-(BOOL)hasAlpha</code>
    public abstract boolean hasAlpha();
    /// Original signature : <code>-(BOOL)isOpaque</code>
    public abstract boolean isOpaque();
    /// Original signature : <code>-(void)setColorSpaceName:(NSString*)</code>
    public abstract String colorSpaceName();
    /// Original signature : <code>-(void)setBitsPerSample:(NSInteger)</code>
    public abstract int bitsPerSample();
    /// Original signature : <code>-(NSInteger)pixelsWide</code>
    public abstract int pixelsWide();
    /// Original signature : <code>-(NSInteger)pixelsHigh</code>
    public abstract int pixelsHigh();


}
