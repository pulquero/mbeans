package net.sf.mbeans.agent;

import java.applet.Applet;
import java.net.URL;
import java.util.Set;
import java.util.Iterator;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import net.sf.mbeans.agent.AgentMBean;
import net.sf.mbeans.util.MBeanInfoUtilities;
import net.sf.mbeans.util.MBeanUtilities;
import net.sf.mbeans.util.ObjectUtilities;
import netscape.javascript.JSObject;

public class AgentMoz extends Applet {
	private static final String DEFAULT_DOMAIN = "AgentMoz";
	private static final String DOMAIN_PARAM = "domain";
	private static final String MBEAN_CLASSNAME_PARAM = "mbean.classname";
	private static final String MBEAN_PARAMETERS_PARAM = "mbean.parameters";
	private static final String MBEAN_SIGNATURE_PARAM = "mbean.signature";
	private static final MBeanServer server = AgentMBean.getPlatformMBeanServer();

	private ObjectName bootName;

	public void init() {
		String domain = getParameter(DOMAIN_PARAM);
		if(domain == null)
			domain = DEFAULT_DOMAIN;
		try {
			// add applet class loader to the MBean server class loader repository
			ObjectName name = ObjectName.getInstance(domain+':'+AgentMBean.SERVICE_KEY+"=ClassLoader");
			Object[] params = new Object[] {new URL[0], getClass().getClassLoader()};
			String[] signature = new String[] {URL[].class.getName(), ClassLoader.class.getName()};
			server.createMBean("javax.management.loading.MLet", name, params, signature);

			name = ObjectName.getInstance(domain+':'+AgentMBean.TYPE_KEY+"=navigator,"+AgentMBean.SERVICE_KEY+"=JavaScript");
			JSObject window = JSObject.getWindow(this);
			JSObject navigator = (JSObject) window.getMember("navigator");
			server.createMBean("managency.javascript.Navigator", name, new Object[] {navigator}, new String[] {JSObject.class.getName()});

			bootName = ObjectName.getInstance(domain+':'+AgentMBean.SERVICE_KEY+"=Boot");
			boot();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	private void boot() throws Exception {
		String className = getParameter(MBEAN_CLASSNAME_PARAM);
		if(className != null) {
			String[] params = ObjectUtilities.split(getParameter(MBEAN_PARAMETERS_PARAM), ',');
			String[] signature = ObjectUtilities.split(getParameter(MBEAN_SIGNATURE_PARAM), ',');
			Object[] parameters;
			if(params != null && signature != null) {
				parameters = new Object[params.length];
				for(int i=0; i<parameters.length; i++)
					parameters[i] = ObjectUtilities.valueOf(params[i], signature[i]);
			} else {
				parameters = null;
				signature = null;
			}
			server.createMBean(className, bootName, parameters, signature);
			if(MBeanInfoUtilities.hasStartOperation(server.getMBeanInfo(bootName)))
				MBeanUtilities.invokeStartOperation(bootName, server);
		}
	}
	public void destroy() {
		try {
			// check boot MBean is still registered at shutdown
			if(server.isRegistered(bootName) && MBeanInfoUtilities.hasStopOperation(server.getMBeanInfo(bootName))) {
				MBeanUtilities.invokeStopOperation(bootName, server);
				server.unregisterMBean(bootName);
			}
			unregisterAll();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	private void unregisterAll() throws Exception {
		Set names = server.queryNames(null, null);
		for(Iterator iter = names.iterator(); iter.hasNext(); ) {
			ObjectName name = (ObjectName) iter.next();
			if(!name.getDomain().equals("JMImplementation"))
				server.unregisterMBean(name);
		}
	}

	public String getAppletInfo() {
		return "Agent Moz";
	}
	public String[][] getParameterInfo() {
		return new String[][] {
			{DOMAIN_PARAM, "string", "domain to use (optional)"},
			{MBEAN_CLASSNAME_PARAM, "string", "boot MBean class name"},
			{MBEAN_PARAMETERS_PARAM, "comma separated list", "boot MBean parameters"},
			{MBEAN_SIGNATURE_PARAM, "comma separated list", "boot MBean signature"}
		};
	}
}
