package managency.agent;

import java.lang.reflect.Method;
import java.io.PrintStream;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.JMException;
import managency.util.MBeanUtilities;
import managency.util.MBeanInfoUtilities;

/**
 * Agent M Bean is the agency's top management agent!
 */
public final class AgentMBean {
	public static final String NAME_KEY = "name";
	public static final String TYPE_KEY = "type";
	public static final String SERVICE_KEY = "service";

	/** The name used for the boot MBean. */
	public static final String BOOT_MBEAN_NAME = getDomain()+':'+SERVICE_KEY+"=Boot";
	/** The name used for the agent's timer. */
	public static final String TIMER_MBEAN_NAME = getDomain()+':'+SERVICE_KEY+"=Timer";

	private static MBeanServer platformServer;

        private final MBeanServer server;

        public static void main(String[] args) throws Exception {
		if(args.length >= 1) {
			final int paramCount = args.length-1;
			Object[] params = new Object[paramCount];
			String[] signature = new String[paramCount];
			for(int i=0; i<paramCount; i++) {
				params[i] = args[i+1];
				signature[i] = String.class.getName();
			}
                        AgentMBean agent = new AgentMBean();
			agent.boot(args[0], params, signature);
                        agent.init();
		} else {
			printUsage(System.out);
		}
	}
        
        private AgentMBean() {
            server = getPlatformMBeanServer();
        }
	/**
	 * Boots the microkernel.
	 */
	private void boot(String className, Object[] params, String[] signature) throws Exception {
		final ObjectInstance mbean = server.createMBean(className, ObjectName.getInstance(BOOT_MBEAN_NAME), params, signature);
		final ObjectName name = mbean.getObjectName();

		if(MBeanInfoUtilities.hasStartOperation(server.getMBeanInfo(name)))
			MBeanUtilities.invokeStartOperation(name, server);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					// check boot MBean is still registered at shutdown
					if(server.isRegistered(name) && MBeanInfoUtilities.hasStopOperation(server.getMBeanInfo(name))) {
						MBeanUtilities.invokeStopOperation(name, server);
						server.unregisterMBean(name);
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}));
	}
        /** Netbeans management support */
        private void init() throws Exception {
        }
        /** Netbeans management support */
        public MBeanServer getMBeanServer() {
            return server;
        }
        
	/**
	 * Returns the platform MBeanServer.
	 */
	public static MBeanServer getPlatformMBeanServer() {
		if(platformServer == null) {
			try {
				// Java 5
				Class managementFactory = Class.forName("java.lang.management.ManagementFactory");
				Method method = managementFactory.getMethod("getPlatformMBeanServer", null);
				platformServer = (MBeanServer) method.invoke(null, null);
			} catch(Exception e) {
				// pre-Java 5
				platformServer = MBeanServerFactory.createMBeanServer();
			}
		}
                return platformServer;
	}
	/**
	 * Returns the agent's timer MBean.
	 * This provides a common timer that can be shared amongst MBeans.
	 * If the timer is not yet registered, it is registered and started.
	 */
	public static ObjectInstance getTimer(MBeanServer server) throws JMException {
		final ObjectName timerName = ObjectName.getInstance(TIMER_MBEAN_NAME);
		if(server.isRegistered(timerName)) {
			return server.getObjectInstance(timerName);
		} else {
			final ObjectInstance timer = MBeanUtilities.createTimerMBean(timerName, server);
			MBeanUtilities.invokeStartOperation(timer.getObjectName(), server);
			return timer;
		}
	}
	/**
	 * Returns the agent's domain.
	 * This can be modified with the <code>AgentMBean.domain</code> system property.
	 */
	public static String getDomain() {
		return System.getProperty("AgentMBean.domain", "AgentMBean");
	}
	public static void printUsage(PrintStream out) {
		out.println("Usage: java "+AgentMBean.class.getName()+" <bootMBeanClassName> [args...]");
	}
}
