package net.sf.mbeans.monitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Iterator;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.JMException;

/**
 * Monitors a set of InetAddresses for changes in reachability and notifies any listening MBeans.
 * This class uses the {@link net.sf.mbeans.agent.AgentMBean#getTimer(MBeanServer) AgentMBean timer}.
 */
public class InetAddressMonitor extends ExternalMonitor implements InetAddressMonitorMBean {
	/** The notification type broadcast by this MBean when a change is detected. */
	public static final String NOTIFICATION_TYPE = "managency.monitor.inetAddress.changed";
	private static final String NOTIFICATION_MESSAGE_PREFIX = "InetAddress reachability has changed: ";

	private static final String TIMER_NOTIFICATION_TYPE = "managency.monitor.inetAddress.timer";
	private static final String TIMER_NOTIFICATION_MESSAGE = "InetAddress monitor timer";

	private final Map addresses = new Hashtable();
	private long monitorSequenceNumber;

	public InetAddressMonitor() {
		super();
	}
	/**
	 * @param address the InetAddress to monitor.
	 * @param period how often (in milliseconds) to monitor the InetAddress for changes.
	 */
	public InetAddressMonitor(InetAddress address, long period) {
		super(period);
		addObservedInetAddress(address);
	}
	/**
	 * @param address the InetAddress to monitor.
	 * @param period how often (in milliseconds) to monitor the URL for changes.
	 */
	public InetAddressMonitor(String address, long period) throws UnknownHostException {
		this(InetAddress.getByName(address), period);
	}

	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] {new MBeanNotificationInfo(new String[] {NOTIFICATION_TYPE}, Notification.class.getName(), "InetAddressMonitor notifications")};
	}
	public void addObservedInetAddress(String address) throws UnknownHostException {
		addObservedInetAddress(InetAddress.getByName(address));
	}
	public void addObservedInetAddress(InetAddress address) {
		if(!containsObservedInetAddress(address))
			addresses.put(address, Boolean.FALSE);
	}
	public boolean containsObservedInetAddress(String address) throws UnknownHostException {
		return containsObservedInetAddress(InetAddress.getByName(address));
	}
	public boolean containsObservedInetAddress(InetAddress address) {
		return addresses.containsKey(address);
	}
	public void removeObservedInetAddress(String address) throws UnknownHostException {
		removeObservedInetAddress(InetAddress.getByName(address));
	}
	public void removeObservedInetAddress(InetAddress address) {
		addresses.remove(address);
	}
	public InetAddress[] getObservedInetAddresses() {
		return (InetAddress[]) addresses.keySet().toArray(new InetAddress[addresses.size()]);
	}

	/**
	 * Starts monitoring.
	 */
	public void start() throws JMException {
		start(TIMER_NOTIFICATION_TYPE, TIMER_NOTIFICATION_MESSAGE);
	}

	protected void checkResources() {
		synchronized(addresses) {
			for(Iterator iter = addresses.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry entry = (Map.Entry) iter.next();
				InetAddress address = (InetAddress) entry.getKey();
				Boolean oldReachable = (Boolean) entry.getValue();
				Boolean newReachable;
				try {
					newReachable = Boolean.valueOf(address.isReachable(1000));
				} catch(IOException ioe) {
					System.err.println(name+" could not connect to "+address+": "+ioe.toString());
					newReachable = Boolean.FALSE;
				}
				if(newReachable != oldReachable) {
					entry.setValue(newReachable);
					sendNotification(new Notification(NOTIFICATION_TYPE, name, ++monitorSequenceNumber, NOTIFICATION_MESSAGE_PREFIX+entry));
				}
			}
		}
	}
}
