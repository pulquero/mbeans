package net.sf.mbeans.util;

import java.util.Date;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.JMException;

public final class MBeanUtilities {
	private MBeanUtilities() {}

	public static final String START_OPERATION_NAME = "start";
	public static final String STOP_OPERATION_NAME = "stop";

	/**
	 * Invokes the <code>start()</code> method of an MBean.
	 */
	public static void invokeStartOperation(ObjectName name, MBeanServer server) throws JMException {
		server.invoke(name, START_OPERATION_NAME, null, null);
	}
	/**
	 * Invokes the <code>stop()</code> method of an MBean.
	 */
	public static void invokeStopOperation(ObjectName name, MBeanServer server) throws JMException {
		server.invoke(name, STOP_OPERATION_NAME, null, null);
	}

	private static final String TIMER_MBEAN_CLASSNAME = "javax.management.timer.Timer";
	private static final String ADD_NOTIFICATION_OPERATION_NAME = "addNotification";
	private static final String[] ADD_NOTIFICATION_4_SIGNATURE = {
		String.class.getName(),
		String.class.getName(),
		Object.class.getName(),
		Date.class.getName()
	};
	private static final String[] ADD_NOTIFICATION_5_SIGNATURE = {
		String.class.getName(),
		String.class.getName(),
		Object.class.getName(),
		Date.class.getName(),
		Long.TYPE.getName()
	};
	private static final String[] ADD_NOTIFICATION_7_SIGNATURE = {
		String.class.getName(),
		String.class.getName(),
		Object.class.getName(),
		Date.class.getName(),
		Long.TYPE.getName(),
		Long.TYPE.getName(),
		Boolean.TYPE.getName()
	};
	private static final String REMOVE_NOTIFICATION_OPERATION_NAME = "removeNotification";
	private static final String[] REMOVE_NOTIFICATION_SIGNATURE = {
		Integer.class.getName()
	};

	public static ObjectInstance createTimerMBean(ObjectName name, MBeanServer server) throws JMException {
		return server.createMBean(TIMER_MBEAN_CLASSNAME, name, null, null);
	}
	public static Integer addTimerNotification(ObjectName timer, String type, String message, Object userData, Date date, MBeanServer server) throws JMException {
		return (Integer) server.invoke(timer, ADD_NOTIFICATION_OPERATION_NAME, new Object[] {type, message, userData, date}, ADD_NOTIFICATION_4_SIGNATURE);
	}
	public static Integer addTimerNotification(ObjectName timer, String type, String message, Object userData, Date date, long period, MBeanServer server) throws JMException {
		return (Integer) server.invoke(timer, ADD_NOTIFICATION_OPERATION_NAME, new Object[] {type, message, userData, date, new Long(period)}, ADD_NOTIFICATION_5_SIGNATURE);
	}
	public static Integer addTimerNotification(ObjectName timer, String type, String message, Object userData, Date date, long period, long occurences, boolean fixedRate, MBeanServer server) throws JMException {
		return (Integer) server.invoke(timer, ADD_NOTIFICATION_OPERATION_NAME, new Object[] {type, message, userData, date, new Long(period), new Long(occurences), Boolean.valueOf(fixedRate)}, ADD_NOTIFICATION_7_SIGNATURE);
	}
	public static void removeTimerNotification(ObjectName timer, Integer id, MBeanServer server) throws JMException {
		server.invoke(timer, REMOVE_NOTIFICATION_OPERATION_NAME, new Object[] {id}, REMOVE_NOTIFICATION_SIGNATURE);
	}
}
