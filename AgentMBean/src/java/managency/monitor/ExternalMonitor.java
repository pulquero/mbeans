package managency.monitor;

import java.util.Date;
import java.util.Hashtable;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.NotificationBroadcasterSupport;
import javax.management.JMException;
import javax.management.timer.TimerNotification;
import managency.agent.AgentMBean;
import managency.util.MBeanUtilities;
import managency.util.RunnableQueue;

/**
 * Monitors a set of external resources for changes and notifies any listening MBeans.
 * This class uses the {@link managency.agent.AgentMBean#getTimer(MBeanServer) AgentMBean timer}.
 */
public abstract class ExternalMonitor extends NotificationBroadcasterSupport implements MBeanRegistration, NotificationListener, ExternalMonitorMBean {
	protected MBeanServer mbeanServer;
	protected long period;
	protected ObjectName name;
	protected ObjectInstance timer;
	// checkResource worker
	private final RunnableQueue runQueue = new RunnableQueue();
	private long timerSequenceNumber;
	private Integer timerEventID;

	/**
	 * Default granularity period is 2.5 minutes.
	 */
	public ExternalMonitor() {
		this(180000L);
	}
	public ExternalMonitor(long period) {
		setGranularityPeriod(period);
	}

	/**
	 * Modifies this MBean's name with the key <code>type=Monitor</code>.
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		mbeanServer = server;
		Hashtable properties = new Hashtable(name.getKeyPropertyList());
		properties.put(AgentMBean.TYPE_KEY, "Monitor");
		this.name = ObjectName.getInstance(name.getDomain(), properties);
		timer = AgentMBean.getTimer(mbeanServer);
		return this.name;
	}
	public void postRegister(Boolean registrationDone) {}
	public void preDeregister() throws Exception {
		stop();
	}
	public void postDeregister() {}

	/**
	 * Returns how often the resources are monitored (in milliseconds).
	 */
	public final long getGranularityPeriod() {
		return period;
	}
	/**
	 * Sets how often the resources are monitored (in milliseconds).
	 */
	public final void setGranularityPeriod(long period) {
		if(period <= 0)
			throw new IllegalArgumentException("Granularity must be greater than zero");
		this.period = period;
	}

	protected final void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Monitor not registered with an MBean server");
	}

	/**
	 * Starts monitoring.
	 */
	protected final void start(String timerNotifType, String timerNotifMsg) throws JMException {
		checkRegistered();
		if(timerEventID == null) {
			runQueue.start();
			// add a timer notification
			timerEventID = MBeanUtilities.addTimerNotification(timer.getObjectName(), timerNotifType, timerNotifMsg, this, new Date(), period, mbeanServer);

			// register as a listener for timer notifications
			NotificationFilterSupport filter = new NotificationFilterSupport();
			filter.enableType(timerNotifType);
			mbeanServer.addNotificationListener(timer.getObjectName(), this, filter, this);
		}
	}
	/**
	 * Returns true if the monitor has been started.
	 */
	public final boolean isActive() {
		return (timerEventID != null);
	}
	/**
	 * Stops monitoring.
	 */
	public final void stop() throws JMException {
		checkRegistered();
		if(timerEventID != null) {
			mbeanServer.removeNotificationListener(timer.getObjectName(), this);
			MBeanUtilities.removeTimerNotification(timer.getObjectName(), timerEventID, mbeanServer);
			runQueue.stop();
			timerEventID = null;
		}
	}

	/**
	 * Upon receiving a notification, checks the resources for changes.
	 */
	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof TimerNotification && handback == this) {
			TimerNotification timerNotification = (TimerNotification) notification;
			ObjectName srcName = (ObjectName) timerNotification.getSource();
			Integer srcID = timerNotification.getNotificationID();
			// if this is our timer notification
			if(srcName.equals(timer.getObjectName()) && srcID.equals(timerEventID)) {
				final long notifSeqNo = notification.getSequenceNumber();
				if(notifSeqNo > timerSequenceNumber) {
					timerSequenceNumber = notifSeqNo;
				} else {
					// ignore out-of-order timer notifications
					return;
				}
			}
		}

		// do not block the notification thread
		runQueue.execute(new Runnable() {
			public void run() {
				checkResources();
			}
		});
	}
	protected abstract void checkResources();
}
