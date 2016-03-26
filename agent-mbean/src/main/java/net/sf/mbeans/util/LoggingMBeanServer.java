package net.sf.mbeans.util;

import java.io.ObjectInputStream;
import java.util.Set;
import java.util.logging.Logger;
import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.MBeanServerForwarder;

public class LoggingMBeanServer implements MBeanServerForwarder {
	private static final Logger logger = Logger.getLogger(LoggingMBeanServer.class.getName());
	private MBeanServer mbeanServer;

	private static void logEntering(String method) {
		logger.entering(LoggingMBeanServer.class.getName(), method);
	}

	public MBeanServer getMBeanServer() {
		logEntering("getMBeanServer");
		return mbeanServer;
	}
	public void setMBeanServer(MBeanServer mbs) {
		logEntering("setMBeanServer");
		if(mbeanServer != null || mbs == null)
			throw new IllegalArgumentException();
		mbeanServer = mbs;
	}

	public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
		logEntering("createMBean");
		return mbeanServer.createMBean(className, name);
	}
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
		logEntering("createMBean");
		return mbeanServer.createMBean(className, name, loaderName);
	}
	public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
		logEntering("createMBean");
		return mbeanServer.createMBean(className, name, params, signature);
	}
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
		logEntering("createMBean");
		return mbeanServer.createMBean(className, name, loaderName, params, signature);
	}

	public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
		logEntering("registerMBean");
		return mbeanServer.registerMBean(object, name);
	}
	public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
		logEntering("unregisterMBean");
		mbeanServer.unregisterMBean(name);
	}

	public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
		logEntering("getObjectInstance");
		return mbeanServer.getObjectInstance(name);
	}
	public Set queryMBeans(ObjectName name, QueryExp query) {
		logEntering("queryMBeans");
		return mbeanServer.queryMBeans(name, query);
	}
	public Set queryNames(ObjectName name, QueryExp query) {
		logEntering("queryNames");
		return mbeanServer.queryNames(name, query);
	}
	public boolean isRegistered(ObjectName name) {
		logEntering("isRegistered");
		return mbeanServer.isRegistered(name);
	}
	public Integer getMBeanCount() {
		logEntering("getMBeanCount");
		return mbeanServer.getMBeanCount();
	}

	public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
		logEntering("getAttribute");
		return mbeanServer.getAttribute(name, attribute);
	}
	public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
		logEntering("getAttributes");
		return mbeanServer.getAttributes(name, attributes);
	}
	public void setAttribute(ObjectName name, Attribute attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, InvalidAttributeValueException {
		logEntering("setAttribute");
		mbeanServer.setAttribute(name, attribute);
	}
	public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
		logEntering("setAttributes");
		return mbeanServer.setAttributes(name, attributes);
	}
	public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
		logEntering("invoke");
		return mbeanServer.invoke(name, operationName, params, signature);
	}

	public String getDefaultDomain() {
		logEntering("getDefaultDomain");
		return mbeanServer.getDefaultDomain();
	}
	public String[] getDomains() {
		logEntering("getDomains");
		return mbeanServer.getDomains();
	}

	public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
		logEntering("addNotificationListener");
		mbeanServer.addNotificationListener(name, listener, filter, handback);
	}
	public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
		logEntering("addNotificationListener");
		mbeanServer.addNotificationListener(name, listener, filter, handback);
	}
	public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
		logEntering("removeNotificationListener");
		mbeanServer.removeNotificationListener(name, listener);
	}
	public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
		logEntering("removeNotificationListener");
		mbeanServer.removeNotificationListener(name, listener, filter, handback);
	}
	public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
		logEntering("removeNotificationListener");
		mbeanServer.removeNotificationListener(name, listener);
	}
	public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
		logEntering("removeNotificationListener");
		mbeanServer.removeNotificationListener(name, listener, filter, handback);
	}

	public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
		logEntering("getMBeanInfo");
		return mbeanServer.getMBeanInfo(name);
	}
	public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
		logEntering("isInstanceOf");
		return mbeanServer.isInstanceOf(name, className);
	}

	public Object instantiate(String className) throws ReflectionException, MBeanException {
		logEntering("instantiate");
		return mbeanServer.instantiate(className);
	}
	public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
		logEntering("instantiate");
		return mbeanServer.instantiate(className, loaderName);
	}
	public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
		logEntering("instantiate");
		return mbeanServer.instantiate(className, params, signature);
	}
	public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
		logEntering("instantiate");
		return mbeanServer.instantiate(className, loaderName, params, signature);
	}

	public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
		logEntering("getClassLoaderFor");
		return mbeanServer.getClassLoaderFor(mbeanName);
	}
	public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
		logEntering("getClassLoader");
		return mbeanServer.getClassLoader(loaderName);
	}
	public ClassLoaderRepository getClassLoaderRepository() {
		logEntering("getClassLoaderRepository");
		return mbeanServer.getClassLoaderRepository();
	}

	public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
		logEntering("deserialize");
		return mbeanServer.deserialize(name, data);
	}
	public ObjectInputStream deserialize(String className, byte[] data) throws ReflectionException, OperationsException {
		logEntering("deserialize");
		return mbeanServer.deserialize(className, data);
	}
	public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, ReflectionException, OperationsException {
		logEntering("deserialize");
		return mbeanServer.deserialize(className, loaderName, data);
	}
}
