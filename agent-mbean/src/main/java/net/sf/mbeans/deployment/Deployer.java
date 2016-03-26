package net.sf.mbeans.deployment;

import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MBeanInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import net.sf.mbeans.agent.AgentMBean;
import net.sf.mbeans.util.MBeanInfoUtilities;
import net.sf.mbeans.util.MBeanUtilities;

import javax.management.JMException;

/**
 * Manages the deployment of another MBean.
 * It respects the support of a basic start/stop lifecycle.
 */
public class Deployer implements MBeanRegistration, NotificationListener, DeployerMBean {
	private static final String CLASSLOADER_MBEAN_CLASSNAME = "javax.management.loading.PrivateMLet";
	private static final String[] CLASSLOADER_MBEAN_SIGNATURE = {URL[].class.getName(), Boolean.TYPE.getName()};

	private final ExecutorService runQueue = Executors.newSingleThreadExecutor();
	private final URL[] classPath;
	private final Object[] createParams;
	private final String[] createSignature;

	private MBeanServer mbeanServer;
	private ObjectInstance instance;
	private ObjectInstance classLoaderInstance;
	private boolean isStartable;
	private boolean isStoppable;

	/**
	 * Constructs an MBean deployer.
	 * @param className the class name of the MBean to be deployed.
	 * @param name the name to give the MBean when it is deployed.
	 * @param classPath the classpath URLs required to deploy the MBean.
	 * @param params the constructor parameters to pass to the MBean when it is deployed.
	 * @param signature the constructor's signature.
	 */
	public Deployer(String className, ObjectName name, URL[] classPath, Object[] params, String[] signature) {
		this.classPath = classPath;
		this.createParams = params;
		this.createSignature = signature;
		this.instance = new ObjectInstance(name, className);
	}

	/**
	 * Modifies this MBean's name with the key <code>type=Deployer</code>.
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		mbeanServer = server;
		Hashtable properties = new Hashtable(name.getKeyPropertyList());
		properties.put(AgentMBean.TYPE_KEY, "Deployer");
		return ObjectName.getInstance(name.getDomain(), properties);
	}
	public void postRegister(Boolean registrationDone) {}
	public void preDeregister() throws Exception {
		undeploy();
	}
	public void postDeregister() {
		mbeanServer = null;
	}

	/**
	 * Upon receiving a notification, redeploys the MBean.
	 */
	public void handleNotification(Notification notification, Object handback) {
		// do not block the notification thread
		runQueue.execute(new Runnable() {
			public void run() {
				try {
					undeploy();
					deploy();
				} catch(Exception e) {
					System.err.println("Could not redeploy "+instance.getObjectName());
				}
			}
		});
	}

	/**
	 * Deploys the MBean to be deployed.
	 * Creates and starts the MBean to be deployed.
	 */
	public void deploy() throws JMException {
		checkRegistered();
		final ObjectName name = instance.getObjectName();
		if(!mbeanServer.isRegistered(name)) {
			Hashtable properties = new Hashtable(name.getKeyPropertyList());
			properties.put(AgentMBean.TYPE_KEY, "ClassLoader");
			classLoaderInstance = mbeanServer.createMBean(CLASSLOADER_MBEAN_CLASSNAME, ObjectName.getInstance(name.getDomain(), properties), new Object[] {classPath, Boolean.FALSE}, CLASSLOADER_MBEAN_SIGNATURE);
			instance = mbeanServer.createMBean(instance.getClassName(), name, classLoaderInstance.getObjectName(), createParams, createSignature);
			final MBeanInfo info = mbeanServer.getMBeanInfo(instance.getObjectName());
			isStartable = MBeanInfoUtilities.hasStartOperation(info);
			isStoppable = MBeanInfoUtilities.hasStopOperation(info);
		}
		start();
	}
	/**
	 * Starts the deployed MBean.
	 */
	public void start() throws JMException {
		checkRegistered();
		final ObjectName name = instance.getObjectName();
		if(isStartable && mbeanServer.isRegistered(name))
			MBeanUtilities.invokeStartOperation(name, mbeanServer);
	}
	/**
	 * Stops the deployed MBean.
	 */
	public void stop() throws JMException {
		checkRegistered();
		final ObjectName name = instance.getObjectName();
		if(isStoppable && mbeanServer.isRegistered(name))
			MBeanUtilities.invokeStopOperation(name, mbeanServer);
	}
	/**
	 * Undeploys the deployed MBean.
	 * Stops and unregisters the deployed MBean.
	 */
	public void undeploy() throws JMException {
		stop();
		final ObjectName name = instance.getObjectName();
		if(mbeanServer.isRegistered(name)) {
			mbeanServer.unregisterMBean(name);
			mbeanServer.unregisterMBean(classLoaderInstance.getObjectName());
		}
	}
	private void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Adaptor not registered with an MBean server");
	}
}
