package jna.osx;

import org.rococoa.ObjCClass;
import org.rococoa.ObjCObject;
import org.rococoa.Rococoa;

/**
 * @author gtch
 */
public interface NSUserNotificationCenter extends ObjCObject {

	public static final _Class CLASS = Rococoa.createClass(
			"NSUserNotificationCenter", _Class.class);

	public interface _Class extends ObjCClass {
		NSUserNotificationCenter defaultUserNotificationCenter();
	}

	void scheduleNotification(NSUserNotification notification);

	void deliverNotification(NSUserNotification notification);

}
