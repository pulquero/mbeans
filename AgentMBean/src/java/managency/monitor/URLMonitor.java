package managency.monitor;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Iterator;
import javax.management.Notification;
import javax.management.MBeanNotificationInfo;
import javax.management.JMException;

/**
 * Monitors a set of URLs for changes and notifies any listening MBeans.
 * This class uses the {@link managency.agent.AgentMBean#getTimer(MBeanServer) AgentMBean timer}.
 */
public class URLMonitor extends ExternalMonitor implements URLMonitorMBean {
	/** The notification type broadcast by this MBean when a modification is detected. */
	public static final String NOTIFICATION_TYPE = "managency.monitor.url.modified";
	private static final String NOTIFICATION_MESSAGE_PREFIX = "URL has changed: ";

	private static final String TIMER_NOTIFICATION_TYPE = "managency.monitor.url.timer";
	private static final String TIMER_NOTIFICATION_MESSAGE = "URL monitor timer";

	private final Map urls = new Hashtable();
	private long monitorSequenceNumber;

	public URLMonitor() {
		super();
	}
	/**
	 * @param url the URL to monitor.
	 * @param period how often (in milliseconds) to monitor the URL for changes.
	 */
	public URLMonitor(URL url, long period) {
		super(period);
		addObservedURL(url);
	}
	/**
	 * @param url the URL to monitor.
	 * @param period how often (in milliseconds) to monitor the URL for changes.
	 */
	public URLMonitor(String url, long period) throws MalformedURLException {
		this(new URL(url), period);
	}

	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] {new MBeanNotificationInfo(new String[] {NOTIFICATION_TYPE}, Notification.class.getName(), "URLMonitor notifications")};
	}
	public void addObservedURL(String url) throws MalformedURLException {
		addObservedURL(new URL(url));
	}
	public void addObservedURL(URL url) {
		if(!containsObservedURL(url))
			urls.put(url, new long[] {-1L, -1L});
	}
	public boolean containsObservedURL(String url) throws MalformedURLException {
		return containsObservedURL(new URL(url));
	}
	public boolean containsObservedURL(URL url) {
		return urls.containsKey(url);
	}
	public void removeObservedURL(String url) throws MalformedURLException {
		removeObservedURL(new URL(url));
	}
	public void removeObservedURL(URL url) {
		urls.remove(url);
	}
	public URL[] getObservedURLs() {
		return (URL[]) urls.keySet().toArray(new URL[urls.size()]);
	}

	/**
	 * Starts monitoring.
	 */
	public void start() throws JMException {
		start(TIMER_NOTIFICATION_TYPE, TIMER_NOTIFICATION_MESSAGE);
	}

	protected void checkResources() {
		synchronized(urls) {
			for(Iterator iter = urls.entrySet().iterator(); iter.hasNext(); ) {
				Map.Entry entry = (Map.Entry) iter.next();
				URL url = (URL) entry.getKey();
				long[] header = (long[]) entry.getValue();
				long oldModified = header[0];
				long oldLength = header[1];
				long newModified;
				long newLength;
				try {
					URLConnection conn = url.openConnection();
					conn.connect();
					newModified = conn.getLastModified();
					newLength = conn.getContentLength();
				} catch(IOException ioe) {
					System.err.println(name+" could not connect to "+url+": "+ioe.toString());
					newModified = -1L;
					newLength = -1L;
				}
				if(newModified != oldModified || newLength != oldLength) {
					header[0] = newModified;
					header[1] = newLength;
					entry.setValue(header);
					sendNotification(new Notification(NOTIFICATION_TYPE, name, ++monitorSequenceNumber, NOTIFICATION_MESSAGE_PREFIX+url+"="+header[0]+","+header[1]));
				}
			}
		}
	}
}
