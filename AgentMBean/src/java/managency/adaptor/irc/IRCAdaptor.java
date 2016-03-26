package managency.adaptor.irc;

import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import java.security.PrivilegedExceptionAction;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.AttributeNotFoundException;
import javax.management.ReflectionException;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.Colors;
import managency.util.MBeanInfoUtilities;
import managency.util.ObjectUtilities;

/**
 * An IRC adaptor.
 * This adaptor can listen for notifications.
 * When a notification is received, it is broadcast to any IRC channels the adaptor has joined.
 * Security checks are performed on adaptor commands by checking for a <code>JMXPrincipal</code> with the sender's nick.
 */
public class IRCAdaptor extends PircBot implements MBeanRegistration, IRCAdaptorMBean, NotificationListener {
	private static final String LIST_MBEANS_CMD = "list";
	private static final String CREATE_MBEAN_CMD = "create ";
	private static final String SHOW_MBEAN_CMD = "show ";
	private static final String SET_ATTRIBUTE_CMD = "set ";
	private static final String INVOKE_OPERATION_CMD = "invoke ";
	private static final String ADD_LISTENER_CMD = "+listener ";
	private static final String REMOVE_LISTENER_CMD = "-listener ";
	private static final String UNREGISTER_MBEAN_CMD = "unregister ";
	private static final String HELP_CMD = "help";

        private String commandPrefix = "!";
	private char paramSeparator = ',';
        private String ircServer;
        private String[] channels = new String[0];
        private MBeanServer mbeanServer;

        public IRCAdaptor(String server) {
                ircServer = server;
                setVerbose(true);
                setName("IRCAdaptor");
                setLogin("IRCAdaptor");
                setVersion("JMX IRC Adaptor");
        }

        public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
                mbeanServer = server;
                return name;
        }
        public void postRegister(Boolean registrationDone) {
        }
        public void preDeregister() throws Exception {
		quitServer("Adaptor deregistered");
	}
	public void postDeregister() {
		mbeanServer = null;
	}

	public void setCommandPrefix(String prefix) {
		commandPrefix = prefix;
	}
	public String getCommandPrefix() {
		return commandPrefix;
	}
	public void setParameterSeparator(char sep) {
		paramSeparator = sep;
	}
	public char getParameterSeparator() {
		return paramSeparator;
	}
	/**
	 * Starts this adaptor.
	 * Connects to the IRC server.
	 */
        public void start() throws Exception {
                connect(ircServer);
                for(int i=0; i<channels.length; i++)
                        joinChannel(channels[i]);
        }
	/**
	 * Stops this adaptor.
	 * Disconnects from the IRC server.
	 */
        public void stop() {
                channels = getChannels();
                quitServer("Adaptor stopped");
        }

        protected void onMessage(String channel, String sender, String login, String hostname, String message) {
                message = Colors.removeFormattingAndColors(message.trim());
		if(message.startsWith(commandPrefix)) {
                        String cmd = message.substring(commandPrefix.length());
			handleCommand(sender, cmd);
		}
	}
        protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		handleCommand(sender, message.trim());
	}
	private void handleCommand(final String sender, final String cmd) {
		Subject subject = new Subject();
		subject.getPrincipals().add(new JMXPrincipal(sender));
		subject.setReadOnly();

		if(cmd.startsWith(LIST_MBEANS_CMD)) {
			final int querySepPos = cmd.indexOf(' ', LIST_MBEANS_CMD.length());
			try {
				ObjectName query = (querySepPos != -1) ? ObjectName.getInstance(cmd.substring(querySepPos+1)) : null;
				Set mbeanSet = (Set) Subject.doAsPrivileged(subject, new QueryMBeansAction(query), null);
				if(!mbeanSet.isEmpty()) {
					for(Iterator iter = mbeanSet.iterator(); iter.hasNext(); ) {
						ObjectInstance mbean = (ObjectInstance) iter.next();
						sendNotice(sender, mbean.getObjectName().getCanonicalName()+" "+mbean.getClassName());
					}
				} else {
					sendNotice(sender, "No MBeans found");
				}
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(CREATE_MBEAN_CMD)) {
			final int clsSepPos = cmd.indexOf(' ', CREATE_MBEAN_CMD.length());
			if(clsSepPos == -1) {
				sendNotice(sender, "No class name specified");
				return;
			}
			final int paramsSepPos = cmd.indexOf(' ', clsSepPos+1);
			final String className = (paramsSepPos != -1) ? cmd.substring(clsSepPos+1, paramsSepPos) : cmd.substring(clsSepPos+1);
			final String paramsString = (paramsSepPos != -1) ? cmd.substring(paramsSepPos+1) : "";
			final String[] params = ObjectUtilities.split(paramsString, paramSeparator);
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(CREATE_MBEAN_CMD.length(), clsSepPos));
				Subject.doAsPrivileged(subject, new CreateMBeanAction(name, className, params), null);
				sendNotice(sender, "Created MBean "+name+" of class "+className);
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(SHOW_MBEAN_CMD)) {
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(SHOW_MBEAN_CMD.length()));
				AttributeList attrList = (AttributeList) Subject.doAsPrivileged(subject, new GetAllAttributesAction(name), null);
				MBeanInfo info = (MBeanInfo) Subject.doAsPrivileged(subject, new GetMBeanInfoAction(name), null);
				for(Iterator iter = attrList.iterator(); iter.hasNext(); ) {
					Attribute attr = (Attribute) iter.next();
					MBeanAttributeInfo attrInfo = MBeanInfoUtilities.getAttribute(info, attr.getName());
					String readMode = attrInfo.isReadable() ? "R" : "";
					String writeMode = attrInfo.isWritable() ? "W" : "";
					sendNotice(sender, attr.getName()+" = "+ObjectUtilities.toString(attr.getValue(), attrInfo.getType())+" ("+readMode+writeMode+")");
				}
				MBeanOperationInfo opInfos[] = info.getOperations();
				for(int i=0; i<opInfos.length; i++) {
					MBeanParameterInfo[] paramInfos = opInfos[i].getSignature();
					StringBuffer params = new StringBuffer();
					if(paramInfos.length > 0) {
						params.append(paramInfos[0].getType());
						for(int j=1; j<paramInfos.length; j++)
							params.append(',').append(paramInfos[j].getType());
					}
					sendNotice(sender, opInfos[i].getReturnType()+" "+opInfos[i].getName()+"("+params+")");
				}
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(SET_ATTRIBUTE_CMD)) {
			final int attrSepPos = cmd.indexOf(' ', SET_ATTRIBUTE_CMD.length());
			if(attrSepPos == -1) {
				sendNotice(sender, "No attribute name specified");
				return;
			}
			final int valueSepPos = cmd.indexOf(' ', attrSepPos+1);
			if(valueSepPos == -1) {
				sendNotice(sender, "No attribute value specified");
				return;
			}
			String attrName = cmd.substring(attrSepPos+1, valueSepPos);
			String valueString = cmd.substring(valueSepPos+1);
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(SET_ATTRIBUTE_CMD.length(), attrSepPos));
				Subject.doAsPrivileged(subject, new SetAttributeAction(name, attrName, valueString), null);
				sendNotice(sender, "Attribute "+attrName+" has been set to "+valueString);
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(INVOKE_OPERATION_CMD)) {
			final int opSepPos = cmd.indexOf(' ', INVOKE_OPERATION_CMD.length());
			if(opSepPos == -1) {
				sendNotice(sender, "No operation specified");
				return;
			}
			final int paramsSepPos = cmd.indexOf(' ', opSepPos+1);
			final String opName = (paramsSepPos != -1) ? cmd.substring(opSepPos+1, paramsSepPos) : cmd.substring(opSepPos+1);
			final String paramsString = (paramsSepPos != -1) ? cmd.substring(paramsSepPos+1) : "";
			final String[] params = ObjectUtilities.split(paramsString, paramSeparator);
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(INVOKE_OPERATION_CMD.length(), opSepPos));
				String value = (String) Subject.doAsPrivileged(subject, new InvokeOperationAction(name, opName, params), null);
				sendNotice(sender, "Operation returned "+value);
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(ADD_LISTENER_CMD)) {
			final int sepPos = cmd.indexOf(' ', ADD_LISTENER_CMD.length());
			if(sepPos == -1) {
				sendNotice(sender, "No listener specified");
				return;
			}
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(ADD_LISTENER_CMD.length(), sepPos));
				ObjectName listener = ObjectName.getInstance(cmd.substring(sepPos+1));
				Subject.doAsPrivileged(subject, new ListenerMBeanAction(name, listener, true), null);
				sendNotice(sender, listener+" added to "+name);
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(REMOVE_LISTENER_CMD)) {
			final int sepPos = cmd.indexOf(' ', REMOVE_LISTENER_CMD.length());
			if(sepPos == -1) {
				sendNotice(sender, "No listener specified");
				return;
			}
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(REMOVE_LISTENER_CMD.length(), sepPos));
				ObjectName listener = ObjectName.getInstance(cmd.substring(sepPos+1));
				Subject.doAsPrivileged(subject, new ListenerMBeanAction(name, listener, false), null);
				sendNotice(sender, listener+" removed from "+name);
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.startsWith(UNREGISTER_MBEAN_CMD)) {
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(UNREGISTER_MBEAN_CMD.length()));
				Subject.doAsPrivileged(subject, new UnregisterMBeanAction(name), null);
				sendNotice(sender, "Unregistered "+name);
			} catch(Exception e) {
				sendException(sender, e);
			}
		} else if(cmd.equals(HELP_CMD)) {
			sendNotice(sender, LIST_MBEANS_CMD + " [query] - lists all MBeans");
			sendNotice(sender, CREATE_MBEAN_CMD + "<MBean name> <class name> <param 1>"+paramSeparator+"..."+paramSeparator+"<param N> - creates an MBean");
			sendNotice(sender, SHOW_MBEAN_CMD + "<MBean name> - shows an MBean info");
			sendNotice(sender, SET_ATTRIBUTE_CMD + "<MBean name> <attribute name> <attribute value> - sets an MBean attribute");
			sendNotice(sender, INVOKE_OPERATION_CMD + "<MBean name> <operation name> <param 1>"+paramSeparator+"..."+paramSeparator+"<param N> - invokes an MBean operation");
			sendNotice(sender, ADD_LISTENER_CMD + "<MBean name> <MBean listener> - adds a listener to an MBean");
			sendNotice(sender, REMOVE_LISTENER_CMD + "<MBean name> <MBean listener> - removes a listener from an MBean");
			sendNotice(sender, UNREGISTER_MBEAN_CMD + "<MBean name> - unregisters an MBean");
			sendNotice(sender, HELP_CMD + " - displays this command list");
		} else {
			sendNotice(sender, "Unrecognised command: "+cmd);
		}
	}
	private void sendException(String sender, Throwable e) {
		sendMessage(sender, e.toString());
		if(e.getCause() != null)
			sendException(sender, e.getCause());
	}
	public void handleNotification(Notification notification, Object handback) {
		String msg = "#"+notification.getSequenceNumber()+" "+new Date(notification.getTimeStamp())+" type="+notification.getType()+" from "+notification.getSource()+": "+notification.getMessage()+" ("+notification.getUserData()+")";
		String[] channels = getChannels();
		for(int i=0; i<channels.length; i++) {
			sendNotice(channels[i], msg);
		}
	}
	private void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Adaptor not registered with an MBean server");
	}

	private class QueryMBeansAction implements PrivilegedExceptionAction {
		private final ObjectName query;
		private QueryMBeansAction(ObjectName query) {
			this.query = query;
		}
		public Object run() throws Exception {
			checkRegistered();
			return mbeanServer.queryMBeans(query, null);
		}
	}

	private class CreateMBeanAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private final String className;
		private final String[] params;
		private CreateMBeanAction(ObjectName name, String className, String[] params) {
			this.name = name;
			this.className = className;
			this.params = params;
		}
		public Object run() throws Exception {
			checkRegistered();
			String[] signature = new String[params.length];
			for(int i=0; i<signature.length; i++)
				signature[i] = String.class.getName();
			mbeanServer.createMBean(className, name, params, signature);
			return null;
		}
	}

	private class GetMBeanInfoAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private GetMBeanInfoAction(ObjectName name) {
			this.name = name;
		}
		public Object run() throws Exception {
			checkRegistered();
			return mbeanServer.getMBeanInfo(name);
		}
	}

	private class GetAllAttributesAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private GetAllAttributesAction(ObjectName name) {
			this.name = name;
		}
		public Object run() throws Exception {
			checkRegistered();
			final MBeanInfo info = mbeanServer.getMBeanInfo(name);
			MBeanAttributeInfo[] attrInfos = info.getAttributes();
			String[] attrNames = new String[attrInfos.length];
			for(int i=0; i<attrInfos.length; i++)
				attrNames[i] = attrInfos[i].getName();
			return mbeanServer.getAttributes(name, attrNames);
		}
	}

	private class SetAttributeAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private final String attrName;
		private final String valueString;
		private SetAttributeAction(ObjectName name, String attrName, String valueString) {
			this.name = name;
			this.attrName = attrName;
			this.valueString = valueString;
		}
		public Object run() throws Exception {
			checkRegistered();
			final MBeanInfo info = mbeanServer.getMBeanInfo(name);
			final MBeanAttributeInfo attrInfo = MBeanInfoUtilities.getAttribute(info, attrName);
			if(attrInfo == null)
				throw new AttributeNotFoundException(attrName);
			Object value = ObjectUtilities.valueOf(valueString, attrInfo.getType());
			mbeanServer.setAttribute(name, new Attribute(attrName, value));
			return null;
		}
	}

	private class InvokeOperationAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private final String opName;
		private final String[] opParams;
		private InvokeOperationAction(ObjectName name, String opName, String[] opParams) {
			this.name = name;
			this.opName = opName;
			this.opParams = opParams;
		}
		public Object run() throws Exception {
			checkRegistered();
			final MBeanInfo info = mbeanServer.getMBeanInfo(name);
			final MBeanOperationInfo opInfo = MBeanInfoUtilities.getOperation(info, opName, opParams.length);
			if(opInfo == null)
				throw new ReflectionException(new NoSuchMethodException(), "No such operation "+opName+" ("+opParams.length+")");
			MBeanParameterInfo[] paramInfos = opInfo.getSignature();
			Object[] params = new Object[paramInfos.length];
			String[] signature = new String[paramInfos.length];
			for(int i=0; i<paramInfos.length; i++) {
				signature[i] = paramInfos[i].getType();
				params[i] = ObjectUtilities.valueOf(opParams[i], signature[i]);
			}
			Object obj = mbeanServer.invoke(name, opName, params, signature);
			return ObjectUtilities.toString(obj, opInfo.getReturnType());
		}
	}

	private class ListenerMBeanAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private final ObjectName listener;
		private final boolean enable;
		private ListenerMBeanAction(ObjectName name, ObjectName listener, boolean enable) {
			this.name = name;
			this.listener = listener;
			this.enable = enable;
		}
		public Object run() throws Exception {
			checkRegistered();
			if(enable)
				mbeanServer.addNotificationListener(name, listener, null, null);
			else
				mbeanServer.removeNotificationListener(name, listener);
			return null;
		}
	}

	private class UnregisterMBeanAction implements PrivilegedExceptionAction {
		private final ObjectName name;
		private UnregisterMBeanAction(ObjectName name) {
			this.name = name;
		}
		public Object run() throws Exception {
			checkRegistered();
			mbeanServer.unregisterMBean(name);
			return null;
		}
	}
}
