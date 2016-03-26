package managency.mbean;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import javax.management.Attribute;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.JMException;
import javax.management.timer.TimerNotification;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import managency.agent.AgentMBean;
import managency.util.MBeanUtilities;
import managency.util.MBeanInfoUtilities;
import managency.util.ObjectUtilities;
import managency.util.RunnableQueue;

public class TaskScheduler implements MBeanRegistration, NotificationListener, TaskSchedulerMBean {
	private static final String TIMER_NOTIFICATION_TYPE = "managency.taskScheduler.timer";
	private static final String TIMER_NOTIFICATION_MESSAGE = "Task scheduler timer";

	private final RunnableQueue runQueue = new RunnableQueue();
	private final SAXParserFactory parserFactory;
	private MBeanServer mbeanServer;
	private ObjectInstance timer;
	private final URL startTask;
	private final URL stopTask;
	private boolean isStarted = false;

	public TaskScheduler(URL startTask, URL stopTask) {
		parserFactory = SAXParserFactory.newInstance();
		this.startTask = startTask;
		this.stopTask = stopTask;
	}
	public TaskScheduler() {
		this((URL)null, (URL)null);
	}
	public TaskScheduler(String startTaskURL) throws MalformedURLException {
		this(new URL(startTaskURL), null);
	}
	public TaskScheduler(String startTaskURL, String stopTaskURL) throws MalformedURLException {
		this(new URL(startTaskURL), new URL(stopTaskURL));
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

	/**
	 * Executes the start task (if any).
	 */
	public void start() throws Exception {
                checkRegistered();
		if(!isStarted) {
			isStarted = true;

			// register as a listener for timer notifications
			NotificationFilterSupport filter = new NotificationFilterSupport();
			filter.enableType(TIMER_NOTIFICATION_TYPE);
			mbeanServer.addNotificationListener(timer.getObjectName(), this, filter, this);

			if(startTask != null)
				executeTask(startTask);
		}
	}
	/**
	 * Executes the stop task (if any).
	 */
	public void stop() throws Exception {
                checkRegistered();
		if(isStarted) {
			isStarted = false;

			// deregister as a listener for timer notifications
			mbeanServer.removeNotificationListener(timer.getObjectName(), this);

			if(stopTask != null)
				executeTask(stopTask);
		}
	}

	/**
	 * Schedules a task descriptor for execution.
	 */
	public void schedule(URL url, long delay) throws JMException {
		schedule(url, delay, 0L);
	}
	public void schedule(URL url, long delay, long period) throws JMException {
		schedule(url, delay, period, 0L, false);
	}
	public void scheduleAtFixedRate(URL url, long delay, long period) throws JMException {
		schedule(url, delay, period, 0L, true);
	}
	public void schedule(URL url, long delay, long period, long occurences, boolean fixedRate) throws JMException {
		checkRegistered();
		// add a timer notification
		Integer timerEventID = MBeanUtilities.addTimerNotification(timer.getObjectName(), TIMER_NOTIFICATION_TYPE, TIMER_NOTIFICATION_MESSAGE, url, new Date(System.currentTimeMillis()+delay), period, occurences, fixedRate, mbeanServer);
	}
	private void executeTask(URL url) throws ParserConfigurationException, IOException, SAXException {
		checkRegistered();
		SAXParser parser = parserFactory.newSAXParser();
		DescriptorHandler desc = new DescriptorHandler();
		parser.parse(new InputSource(url.toString()), desc);
	}
	private void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Adaptor not registered with an MBean server");
	}

	public void handleNotification(Notification notification, Object handback) {
		if(notification instanceof TimerNotification) {
			final URL url = (URL) notification.getUserData();

			// do not block the notification thread
			runQueue.start(); // make sure the queue is running
			runQueue.execute(new Runnable() {
				public void run() {
					try {
						executeTask(url);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	private class DescriptorHandler extends DefaultHandler {
		private final List params = new ArrayList();
		private final List signature = new ArrayList();
		private ObjectName mbean;
		private String name;

		public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
			if(qName.equals("create")) {
				mbean = getMBeanName(attrs.getValue("mbean"));
				name = attrs.getValue("name");
			} else if(qName.equals("attribute")) {
				mbean = getMBeanName(attrs.getValue("mbean"));
				name = attrs.getValue("name");

				try {
					MBeanInfo info = mbeanServer.getMBeanInfo(mbean);
					MBeanAttributeInfo attrInfo = MBeanInfoUtilities.getAttribute(info, name);
					String type = attrInfo.getType();
					Object value = getValue(attrs.getValue("value"), type);
					params.add(value);
					signature.add(type);
				} catch(JMException e) {
					System.err.println(e.toString());
					mbean = null;
				}
			} else if(qName.equals("invoke")) {
				mbean = getMBeanName(attrs.getValue("mbean"));
				name = attrs.getValue("name");
			} else if(qName.equals("parameter")) {
				String type = attrs.getValue("type");
				Object value = getValue(attrs.getValue("value"), type);
				params.add(value);
				signature.add(type);
			} else if(qName.equals("addListener") || qName.equals("removeListener")) {
				mbean = getMBeanName(attrs.getValue("mbean"));
				name = attrs.getValue("name");
				String filter = attrs.getValue("filter");
				if(filter != null) {
					NotificationFilterSupport notificationFilter = new NotificationFilterSupport();
					notificationFilter.enableType(filter);
					params.add(notificationFilter);
				} else {
					params.add(null);
				}
				signature.add("javax.management.NotificationFilter");
				params.add(attrs.getValue("handback"));
				signature.add("java.lang.Object");
			} else if(qName.equals("unregister")) {
				mbean = getMBeanName(attrs.getValue("mbean"));
			}
		}
		private ObjectName getMBeanName(String name) {
			try {
				return ObjectName.getInstance(name);
			} catch(MalformedObjectNameException mone) {
				System.err.println("Invalid ObjectName in mbean attribute: "+name);
				return null;
			}
		}
		private Object getValue(String value, String type) {
			try {
				return ObjectUtilities.valueOf(value, type);
			} catch(Exception e) {
				System.err.println("Invalid parameter: "+e.toString());
				return null;
			}
		}
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(qName.equals("create")) {
				try {
					mbeanServer.createMBean(name, mbean, params.toArray(), (String[]) signature.toArray(new String[0]));
				} catch(JMException jme) {
					System.err.println("Failed to create "+mbean+": "+jme.toString());
				}
				params.clear();
				signature.clear();
			} else if(qName.equals("attribute")) {
				try {
					mbeanServer.setAttribute(mbean, new Attribute(name, params.get(0)));
				} catch(JMException jme) {
					System.err.println("Failed to set attribute "+name+" of "+mbean+": "+jme.toString());
				}
				params.clear();
				signature.clear();
			} else if(qName.equals("invoke")) {
				try {
					mbeanServer.invoke(mbean, name, params.toArray(), (String[]) signature.toArray(new String[0]));
				} catch(JMException jme) {
					System.err.println("Failed to invoke operation "+name+" of "+mbean+": "+jme.toString());
				}
				params.clear();
				signature.clear();
			} else if(qName.equals("addListener")) {
				try {
					mbeanServer.addNotificationListener(mbean, ObjectName.getInstance(name), (NotificationFilter) params.get(0), params.get(1));
				} catch(JMException jme) {
					System.err.println("Failed to add listener to "+mbean+": "+jme.toString());
				}
				params.clear();
				signature.clear();
			} else if(qName.equals("removeListener")) {
				try {
					NotificationFilter filter = (NotificationFilter) params.get(0);
					Object handback = params.get(1);
					if(filter != null || handback != null)
						mbeanServer.removeNotificationListener(mbean, ObjectName.getInstance(name), filter, handback);
					else
						mbeanServer.removeNotificationListener(mbean, ObjectName.getInstance(name));
				} catch(JMException jme) {
					System.err.println("Failed to remove listener from "+mbean+": "+jme.toString());
				}
				params.clear();
				signature.clear();
			} else if(qName.equals("unregister")) {
				try {
					mbeanServer.unregisterMBean(mbean);
				} catch(JMException jme) {
					System.err.println("Failed to unregister "+mbean+": "+jme.toString());
				}
				params.clear();
				signature.clear();
			}
		}
	}
}
