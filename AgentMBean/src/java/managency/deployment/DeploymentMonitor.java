package managency.deployment;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.timer.TimerNotification;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import managency.agent.AgentMBean;
import managency.monitor.URLMonitor;
import managency.util.MBeanUtilities;
import managency.util.ObjectUtilities;
import managency.util.RunnableQueue;

/**
 * MBean deployment server.
 * This class uses the {@link managency.agent.AgentMBean#getTimer(MBeanServer) AgentMBean timer}.
 */
public class DeploymentMonitor implements MBeanRegistration, NotificationListener, DeploymentMonitorMBean {
	private static final String DEPLOYER_MBEAN_CLASSNAME = "managency.deployment.Deployer";
	private static final String[] DEPLOYER_MBEAN_SIGNATURE = {
		String.class.getName(),
		ObjectName.class.getName(),
		URL[].class.getName(),
		Object[].class.getName(),
		String[].class.getName()
	};
	private static final String DEPLOY_OPERATION_NAME = "deploy";
	private static final String UNDEPLOY_OPERATION_NAME = "undeploy";

	private static final String TIMER_NOTIFICATION_TYPE = "managency.deployment.monitor.timer";
	private static final String TIMER_NOTIFICATION_MESSAGE = "Deployment monitor timer";

	private static final String URLMONITOR_MBEAN_CLASSNAME = "managency.monitor.URLMonitor";
	private static final String[] URLMONITOR_MBEAN_SIGNATURE = {
		URL.class.getName(),
		Long.TYPE.getName()
	};

	private final RunnableQueue runQueue = new RunnableQueue();
	private final Map deployments = new HashMap();
	private final SAXParserFactory parserFactory;
	private File deploymentDirectory;
	private List xmlFiles = Collections.EMPTY_LIST;
	private MBeanServer mbeanServer;
	private ObjectInstance timer;
	private Integer timerEventID;
	private long monitorInterval = 1000;
	private long timerSequenceNumber;

	/**
	 * Monitors the current directory.
	 */
	public DeploymentMonitor() {
		this(".");
	}
	/**
	 * @param directory the directory to monitor.
	 */
	public DeploymentMonitor(String directory) {
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
		deploymentDirectory = new File(directory);
	}

	public ObjectName preRegister(MBeanServer server, ObjectName name) throws JMException {
		mbeanServer = server;
		timer = AgentMBean.getTimer(mbeanServer);
		return name;
	}
	public void postRegister(Boolean registrationDone) {}
	public void preDeregister() throws Exception {
		stop();
	}
	public void postDeregister() {
		mbeanServer = null;
	}

	public String getDeploymentDirectory() {
		return deploymentDirectory.toString();
	}
	public void setDeploymentDirectory(String directory) {
		deploymentDirectory = new File(directory);
	}

	public long getMonitorInterval() {
		return monitorInterval;
	}
	public void setMonitorInterval(long interval) {
		if(interval <= 0)
			throw new IllegalArgumentException("Interval must be greater than zero");
		monitorInterval = interval;
	}

	public void start() throws JMException {
		checkRegistered();
		if(timerEventID == null) {
			runQueue.start();
			timerEventID = MBeanUtilities.addTimerNotification(timer.getObjectName(), TIMER_NOTIFICATION_TYPE, TIMER_NOTIFICATION_MESSAGE, this, new Date(), monitorInterval, mbeanServer);
			NotificationFilterSupport filter = new NotificationFilterSupport();
			filter.enableType(TIMER_NOTIFICATION_TYPE);
			mbeanServer.addNotificationListener(timer.getObjectName(), this, filter, this);
		}
	}
	/**
	 * Returns true if the deployment monitor has been started.
	 */
	public boolean isActive() {
		return (timerEventID != null);
	}
	public void stop() throws JMException {
		checkRegistered();
		if(timerEventID != null) {
			mbeanServer.removeNotificationListener(timer.getObjectName(), this);
			MBeanUtilities.removeTimerNotification(timer.getObjectName(), timerEventID, mbeanServer);
			runQueue.stop();
			timerEventID = null;
		}
	}
	private void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Adaptor not registered with an MBean server");
	}

	/**
	 * @param url URL of a deployment descriptor.
	 */
	public void deploy(URL url) throws IOException, ParserConfigurationException, SAXException, JMException {
		checkRegistered();
		SAXParser parser = parserFactory.newSAXParser();
		DescriptorHandler desc = new DescriptorHandler();
		parser.parse(new InputSource(url.toString()), desc);

		Object[] params = {desc.className, desc.name, desc.getClassPath(), desc.getParameters(), desc.getSignature()};
		ObjectInstance instance = mbeanServer.createMBean(DEPLOYER_MBEAN_CLASSNAME, desc.name, params, DEPLOYER_MBEAN_SIGNATURE);
		deployments.put(url, instance);
		mbeanServer.invoke(instance.getObjectName(), DEPLOY_OPERATION_NAME, null, null);

		if(desc.monitorURL != null) {
			Object[] monitorParams = {desc.monitorURL, new Long(desc.monitorInterval)};
			ObjectInstance monitorInstance = mbeanServer.createMBean(URLMONITOR_MBEAN_CLASSNAME, desc.name, monitorParams, URLMONITOR_MBEAN_SIGNATURE);
			NotificationFilterSupport filter = new NotificationFilterSupport();
			filter.enableType(URLMonitor.NOTIFICATION_TYPE);
			mbeanServer.addNotificationListener(monitorInstance.getObjectName(), instance.getObjectName(), filter, this);
			MBeanUtilities.invokeStartOperation(monitorInstance.getObjectName(), mbeanServer);
		}
	}
	/**
	 * @param url URL of a deployment descriptor.
	 */
	public void undeploy(URL url) throws JMException {
		checkRegistered();
		final ObjectInstance instance = (ObjectInstance) deployments.get(url);
		if(instance != null) {
			mbeanServer.invoke(instance.getObjectName(), UNDEPLOY_OPERATION_NAME, null, null);
			mbeanServer.unregisterMBean(instance.getObjectName());
		}
	}

	/**
	 * SAX handler.
	 */
	private static class DescriptorHandler extends DefaultHandler {
		ObjectName name;
		String className;
		private final List params = new ArrayList();
		private final List signature = new ArrayList();
		private final List classPath = new ArrayList();
		URL monitorURL;
		long monitorInterval;

		public Object[] getParameters() {
			return params.toArray();
		}
		public String[] getSignature() {
			return (String[]) signature.toArray(new String[0]);
		}
		public URL[] getClassPath() {
			return (URL[]) classPath.toArray(new URL[0]);
		}

		public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
			if(qName.equals("mbean")) {
				String nameAttr = attrs.getValue("name");
				try {
					name = ObjectName.getInstance(nameAttr);
				} catch(MalformedObjectNameException mone) {
					System.err.println("Invalid ObjectName in <"+qName+"/> name attribute: "+nameAttr);
				}
				className = attrs.getValue("classname");
			} else if(qName.equals("parameter")) {
				String type = attrs.getValue("type");
				String valueAttr = attrs.getValue("value");
				Object value;
				try {
					value = ObjectUtilities.valueOf(valueAttr, type);
				} catch(Exception e) {
					System.err.println("Invalid constructor parameter: "+valueAttr+" ("+e.toString()+")");
					value = null;
				}
				params.add(value);
				signature.add(type);
			} else if(qName.equals("monitor")) {
				String hrefAttr = attrs.getValue("xlink:href");
				try {
					monitorURL = new URL(hrefAttr);
					monitorInterval = Long.parseLong(attrs.getValue("interval"));
				} catch(MalformedURLException murle) {
					System.err.println("Invalid URL in <"+qName+"/> xlink:href attribute: "+hrefAttr);
				}
			} else if(qName.equals("classpath")) {
				String hrefAttr = attrs.getValue("xlink:href");
				try {
					classPath.add(new URL(hrefAttr));
				} catch(MalformedURLException murle) {
					System.err.println("Invalid URL in <"+qName+"/> xlink:href attribute: "+hrefAttr);
				}
			}
		}
	}

	/**
	 * Upon receiving a notification, checks the deployment directory.
	 */
	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof TimerNotification && handback == this) {
			TimerNotification timerNotification = (TimerNotification) notification;
			ObjectName srcName = (ObjectName) timerNotification.getSource();
			Integer srcID = timerNotification.getNotificationID();
			// if this is our timer notification
			if(srcName.equals(timer.getObjectName()) && srcID.equals(timerEventID)) {
				final long latestSeqNo = notification.getSequenceNumber();
				if(latestSeqNo > timerSequenceNumber) {
					timerSequenceNumber = latestSeqNo;
				} else {
					// ignore out-of-order timer notifications
					return;
				}
			}
		}

		// do not block the notification thread
		runQueue.execute(new Runnable() {
			public void run() {
			File[] fileList = deploymentDirectory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".mbean");
				}
			});
			final List currentXMLFiles = Collections.unmodifiableList(Arrays.asList(fileList));

			List removedXMLFiles = new ArrayList(xmlFiles);
			removedXMLFiles.removeAll(currentXMLFiles);
			undeployFiles(removedXMLFiles);

			List newXMLFiles = new ArrayList(currentXMLFiles);
			newXMLFiles.removeAll(xmlFiles);
			deployFiles(newXMLFiles);

			xmlFiles = currentXMLFiles;
			}
		});
	}
	private void undeployFiles(List files) {
		for(Iterator iter = files.iterator(); iter.hasNext(); ) {
			File file = (File) iter.next();
			try {
				undeploy(file.toURL());
			} catch(Exception e) {
				System.err.println("Could not undeploy "+file);
				e.printStackTrace();
			}
		}
	}
	private void deployFiles(List files) {
		for(Iterator iter = files.iterator(); iter.hasNext(); ) {
			File file = (File) iter.next();
			try {
				deploy(file.toURL());
			} catch(Exception e) {
				System.err.println("Could not deploy "+file);
				e.printStackTrace();
			}
		}
	}
}
