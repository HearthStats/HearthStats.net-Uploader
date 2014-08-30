package net.hearthstats.notification;

public interface NotificationQueue {

  /**
   * Displays a notification on the screen.
   *
   * @param header     The header of the notification
   * @param message    Extra information for the notification, optional
   * @param allowFocus
   */
  void add(String header, String message, boolean allowFocus);

  /**
   * Clears all notifications, removing all displayed and undisplayed notifications (if any).
   */
  void clearAllNotifications();

}
