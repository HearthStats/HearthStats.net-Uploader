package net.hearthstats.notification;

import java.util.ArrayList;

public class DialogNotificationQueue implements NotificationQueue {

	protected ArrayList<DialogNotification> _notifications = new ArrayList<DialogNotification>();

	public DialogNotificationQueue() {

	}

    @Override
    public void add(String header, String message, boolean allowFocus) {
        add(new DialogNotification(header, message, allowFocus));
    }

	public void add(DialogNotification notification) {
		_notifications.add(0, notification);
		_process();
	}

	protected void _process() {
		for (int i = 0; i < _notifications.size(); i++) {
			final DialogNotification notification = _notifications.get(i);
			if (!notification.wasShown()) {
				notification.offset(i);
				notification.show();
				new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(5000); // time after which pop up will
												// be disappeared.
							
							for (int x = 0; x < _notifications.size(); x++) {
								if(notification == _notifications.get(x)) {
									_notifications.remove(notification);
								}
							}
							notification.close();
							_process();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					};
				}.start();
			} else {
				notification.offset(i);
			}
		}
	}
}
