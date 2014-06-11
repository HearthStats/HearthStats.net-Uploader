package net.hearthstats.osx.jna;

import org.rococoa.NSClass;
import org.rococoa.NSObject;
import org.rococoa.Rococoa;


/**
 * @author gtch
 */
public interface NSUserNotificationCenter extends NSObject {

  public static final _Class CLASS = Rococoa.createClass(
    "NSUserNotificationCenter", _Class.class);

  public interface _Class extends NSClass {
    NSUserNotificationCenter defaultUserNotificationCenter();
  }

  void scheduleNotification(NSUserNotification notification);

  void deliverNotification(NSUserNotification notification);

}
