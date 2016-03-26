package managency.mbean.win32;

import javax.management.Notification;

public class ActiveXNotification extends Notification {
	public ActiveXNotification(String type, Object source, long seq, Object[] params) {
		super(type, source, seq);
		setUserData(params);
	}
}
