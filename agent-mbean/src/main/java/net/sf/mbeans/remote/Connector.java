package net.sf.mbeans.remote;

import java.io.Serializable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanRegistration;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.ListenerNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXServiceURL;

/**
 * A JMX connector.
 * This MBean can be used to establish a bridge between two MBean servers.
 * MBeans in the remote server will be proxied in the local server.
 */
public class Connector extends NotificationBroadcasterSupport implements JMXConnector, MBeanRegistration, NotificationListener, ConnectorMBean {
	private static final String SERVICE_URL_KEY = "serviceURL";
	private static final String CONNECTION_ID_KEY = "connectionID";
	private static final String PROXY_MBEAN_CLASSNAME = "managency.remote.ProxyMBean";
	private static final String[] PROXY_MBEAN_SIGNATURE = {ObjectName.class.getName(), MBeanServerConnection.class.getName()};
	private final JMXServiceURL url;
	private Map environment;
	private MBeanServer mbeanServer;
	private JMXConnector connector;

	public Connector(String url) throws MalformedURLException {
		this(url, Collections.EMPTY_MAP);
	}
	public Connector(String url, Map env) throws MalformedURLException {
		this(new JMXServiceURL(url), env);
	}
	public Connector(JMXServiceURL url) {
		this(url, Collections.EMPTY_MAP);
	}
	public Connector(JMXServiceURL url, Map env) {
		this.url = url;
		this.environment = (env != null) ? env : Collections.EMPTY_MAP;
	}

	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] {new MBeanNotificationInfo(new String[] {JMXConnectionNotification.OPENED, JMXConnectionNotification.CLOSED, JMXConnectionNotification.FAILED, JMXConnectionNotification.NOTIFS_LOST}, JMXConnectionNotification.class.getName(), "Connection notifications")};
	}

	public ObjectName preRegister(MBeanServer server, ObjectName name) {
		mbeanServer = server;
		return name;
	}
	public void postRegister(Boolean registrationDone) {}
	public void preDeregister() throws Exception {
		if(connector != null)
			stop();
	}
	public void postDeregister() {
		mbeanServer = null;
	}

	public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		addNotificationListener(listener, filter, handback);
	}
	public void removeConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		removeNotificationListener(listener, filter, handback);
	}
	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		removeNotificationListener(listener);
	}
	public String getConnectionId() throws IOException {
		if(connector == null)
			throw new IOException("Not connected");
		return connector.getConnectionId();
	}
	public JMXServiceURL getAddress() {
		return url;
	}
	public Map getAttributes() {
		Map attrs = new HashMap(environment.size());
		for(Iterator iter = environment.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry entry = (Map.Entry) iter.next();
			if(entry.getValue() instanceof Serializable)
				attrs.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(attrs);
	}
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		if(connector == null)
			throw new IOException("Not connected");
		return connector.getMBeanServerConnection();
	}
	public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
		if(connector == null)
			throw new IOException("Not connected");
		return connector.getMBeanServerConnection(delegationSubject);
	}
	public void connect() throws IOException {
		connector = JMXConnectorFactory.newJMXConnector(url, environment);
		connector.addConnectionNotificationListener(this, null, this);
		connector.connect();
	}
	public void connect(Map env) throws IOException {
		this.environment = (env != null) ? env : Collections.EMPTY_MAP;
		connect();
	}
	public boolean isConnected() {
		return (connector != null);
	}
	public void close() throws IOException {
		if(connector == null)
			return;
		connector.close();
		try {
			connector.removeConnectionNotificationListener(this);
		} catch(ListenerNotFoundException lnfe) {}
		connector = null;
	}

	/**
	 * Opens a connection and creates local proxy MBeans for all the remote MBeans.
	 * The proxy MBeans will be named after their remote MBeans and the identifiers for this connection.
	 */
	public void start() throws IOException {
		checkRegistered();
		connect();
		final MBeanServerConnection connection = connector.getMBeanServerConnection();
		Set mbeans = connection.queryNames(null, null);
		for(Iterator iter = mbeans.iterator(); iter.hasNext(); ) {
			ObjectName name = (ObjectName) iter.next();
			String domain = name.getDomain();
			if(domain.equals("JMImplementation"))
				domain = "JMImplementationProxy";
			Hashtable keyProperties = new Hashtable(name.getKeyPropertyList());
			putProxyKeyProperties(keyProperties);
			try {
				ObjectName proxyName = ObjectName.getInstance(domain, keyProperties);
				mbeanServer.createMBean(PROXY_MBEAN_CLASSNAME, proxyName, new Object[] {name, connection}, PROXY_MBEAN_SIGNATURE);
			} catch(Exception jme) {
				System.err.println(jme.toString());
			}
		}
	}
	/**
	 * Unregisters all the proxy MBeans using this connector and closes the connection.
	 */
	public void stop() throws IOException {
		checkRegistered();
		try {
			Hashtable keyProperties = new Hashtable();
			putProxyKeyProperties(keyProperties);
			ObjectName pattern = ObjectName.getInstance("*", keyProperties);
			pattern = ObjectName.getInstance(pattern.getCanonicalName()+",*");
			Set mbeans = mbeanServer.queryMBeans(pattern, null);
			for(Iterator iter = mbeans.iterator(); iter.hasNext(); ) {
				ObjectInstance mbean = (ObjectInstance) iter.next();
				try {
					mbeanServer.unregisterMBean(mbean.getObjectName());
				} catch(Exception jme) {
					System.err.println(jme.toString());
				}
			}
		} catch(MalformedObjectNameException mone) {
			System.err.println(mone.toString());
		}
		close();
	}
	private void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Adaptor not registered with an MBean server");
	}
	private void putProxyKeyProperties(Hashtable keyProperties) throws IOException {
		keyProperties.put(SERVICE_URL_KEY, ObjectName.quote(url.toString()));
		keyProperties.put(CONNECTION_ID_KEY, ObjectName.quote(connector.getConnectionId()));
	}
	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof JMXConnectionNotification && handback == this) {
			sendNotification(notification);
		}
	}
}
