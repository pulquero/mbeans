package managency.adaptor.jms;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.ReflectionException;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicPublisher;
import javax.jms.TopicSubscriber;
import javax.jms.JMSException;
import managency.util.MBeanInfoUtilities;
import managency.util.ObjectUtilities;

/**
 * A JMS adaptor.
 * This adaptor can listen for notifications.
 * When a notification is received, it is published under the current topic.
 */
public class JMSAdaptor implements MBeanRegistration, JMSAdaptorMBean, NotificationListener {
	private static final String LIST_MBEANS_CMD = "list";
	private static final String CREATE_MBEAN_CMD = "create ";
	private static final String SHOW_MBEAN_CMD = "show ";
	private static final String SET_ATTRIBUTE_CMD = "set ";
	private static final String INVOKE_OPERATION_CMD = "invoke ";
	private static final String ADD_LISTENER_CMD = "+listener ";
	private static final String REMOVE_LISTENER_CMD = "-listener ";
	private static final String UNREGISTER_MBEAN_CMD = "unregister ";
	private static final String HELP_CMD = "help";
	private static final char PARAM_SEPARATOR = ',';

	private final TopicConnectionFactory factory;
	private final Topic topic;
	private final TopicConnection connection;
	private MBeanServer mbeanServer;
	private TopicSession session;
	private TopicPublisher publisher;
	private TopicSubscriber subscriber;

	public JMSAdaptor(String topicConnectionFactoryName, String topicName) throws NamingException, JMSException {
		Context ctx = new InitialContext();
		factory = (TopicConnectionFactory) ctx.lookup(topicConnectionFactoryName);
		topic = (Topic) ctx.lookup(topicName);
		connection = factory.createTopicConnection();
	}

        public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
                mbeanServer = server;
                return name;
        }
        public void postRegister(Boolean registrationDone) {
        }
        public void preDeregister() throws Exception {
		stop();
        }
	public void postDeregister() {
		mbeanServer = null;
	}

	/**
	 * Starts this adaptor.
	 */
	public void start() throws JMSException {
		connection.start();
		session = connection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
		publisher = session.createPublisher(topic);
		subscriber = session.createSubscriber(topic, null, true);
		subscriber.setMessageListener(new Listener());
	}
	/**
	 * Stops this adaptor.
	 */
	public void stop() throws JMSException {
		subscriber.close();
		publisher.close();
		session.close();
		connection.stop();
	}
	/**
	 * Sends a JMS text message.
	 */
	public void sendMessage(String text) throws JMSException {
		TextMessage message = session.createTextMessage(text);
		publisher.publish(message);
	}
	private void handleCommand(String cmd) throws JMSException {
		if(cmd.startsWith(LIST_MBEANS_CMD)) {
			final int querySepPos = cmd.indexOf(' ', LIST_MBEANS_CMD.length());
			try {
				ObjectName query = (querySepPos != -1) ? ObjectName.getInstance(cmd.substring(querySepPos+1)) : null;
				checkRegistered();
				Set mbeanSet = mbeanServer.queryMBeans(query, null);
				if(!mbeanSet.isEmpty()) {
					for(Iterator iter = mbeanSet.iterator(); iter.hasNext(); ) {
						ObjectInstance mbean = (ObjectInstance) iter.next();
						sendMessage(mbean.getObjectName().getCanonicalName()+" "+mbean.getClassName());
					}
				} else {
					sendMessage("No MBeans found");
				}
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(CREATE_MBEAN_CMD)) {
			final int clsSepPos = cmd.indexOf(' ', CREATE_MBEAN_CMD.length());
			if(clsSepPos == -1) {
				sendMessage("No class name specified");
				return;
			}
			final int paramsSepPos = cmd.indexOf(' ', clsSepPos+1);
			final String className = (paramsSepPos != -1) ? cmd.substring(clsSepPos+1, paramsSepPos) : cmd.substring(clsSepPos+1);
			final String paramsString = (paramsSepPos != -1) ? cmd.substring(paramsSepPos+1) : "";
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(CREATE_MBEAN_CMD.length(), clsSepPos));
				checkRegistered();
				String[] params = ObjectUtilities.split(paramsString, PARAM_SEPARATOR);
				String[] signature = new String[params.length];
				for(int i=0; i<signature.length; i++)
					signature[i] = String.class.getName();
				mbeanServer.createMBean(className, name, params, signature);
				sendMessage("Created MBean "+name+" of class "+className);
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(SHOW_MBEAN_CMD)) {
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(SHOW_MBEAN_CMD.length()));
				checkRegistered();
				final MBeanInfo info = mbeanServer.getMBeanInfo(name);
				MBeanAttributeInfo[] attrInfos = info.getAttributes();
				String[] attrNames = new String[attrInfos.length];
				for(int i=0; i<attrInfos.length; i++)
					attrNames[i] = attrInfos[i].getName();
				AttributeList attrList = mbeanServer.getAttributes(name, attrNames);
				for(Iterator iter = attrList.iterator(); iter.hasNext(); ) {
					Attribute attr = (Attribute) iter.next();
					MBeanAttributeInfo attrInfo = MBeanInfoUtilities.getAttribute(info, attr.getName());
					String readMode = attrInfo.isReadable() ? "R" : "";
					String writeMode = attrInfo.isWritable() ? "W" : "";
					sendMessage(attr.getName()+" = "+ObjectUtilities.toString(attr.getValue(), attrInfo.getType())+" ("+readMode+writeMode+")");
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
					sendMessage(opInfos[i].getReturnType()+" "+opInfos[i].getName()+"("+params+")");
				}
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(SET_ATTRIBUTE_CMD)) {
			final int attrSepPos = cmd.indexOf(' ', SET_ATTRIBUTE_CMD.length());
			if(attrSepPos == -1) {
				sendMessage("No attribute name specified");
				return;
			}
			final int valueSepPos = cmd.indexOf(' ', attrSepPos+1);
			if(valueSepPos == -1) {
				sendMessage("No attribute value specified");
				return;
			}
			String attrName = cmd.substring(attrSepPos+1, valueSepPos);
			String valueString = cmd.substring(valueSepPos+1);
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(SET_ATTRIBUTE_CMD.length(), attrSepPos));
				checkRegistered();
				final MBeanInfo info = mbeanServer.getMBeanInfo(name);
				final MBeanAttributeInfo attrInfo = MBeanInfoUtilities.getAttribute(info, attrName);
				if(attrInfo == null)
					throw new AttributeNotFoundException(attrName);
				Object value = ObjectUtilities.valueOf(valueString, attrInfo.getType());
				mbeanServer.setAttribute(name, new Attribute(attrName, value));
				sendMessage("Attribute "+attrName+" has been set to "+valueString);
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(INVOKE_OPERATION_CMD)) {
			final int opSepPos = cmd.indexOf(' ', INVOKE_OPERATION_CMD.length());
			if(opSepPos == -1) {
				sendMessage("No operation specified");
				return;
			}
			final int paramsSepPos = cmd.indexOf(' ', opSepPos+1);
			final String opName = (paramsSepPos != -1) ? cmd.substring(opSepPos+1, paramsSepPos) : cmd.substring(opSepPos+1);
			final String paramsString = (paramsSepPos != -1) ? cmd.substring(paramsSepPos+1) : "";
			final String[] opParams = ObjectUtilities.split(paramsString, PARAM_SEPARATOR);
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(INVOKE_OPERATION_CMD.length(), opSepPos));
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
				sendMessage("Operation returned "+ObjectUtilities.toString(obj, opInfo.getReturnType()));
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(ADD_LISTENER_CMD)) {
			final int sepPos = cmd.indexOf(' ', ADD_LISTENER_CMD.length());
			if(sepPos == -1) {
				sendMessage("No listener specified");
				return;
			}
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(ADD_LISTENER_CMD.length(), sepPos));
				ObjectName listener = ObjectName.getInstance(cmd.substring(sepPos+1));
				checkRegistered();
				mbeanServer.addNotificationListener(name, listener, null, null);
				sendMessage(listener+" added to "+name);
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(REMOVE_LISTENER_CMD)) {
			final int sepPos = cmd.indexOf(' ', REMOVE_LISTENER_CMD.length());
			if(sepPos == -1) {
				sendMessage("No listener specified");
				return;
			}
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(REMOVE_LISTENER_CMD.length(), sepPos));
				ObjectName listener = ObjectName.getInstance(cmd.substring(sepPos+1));
				checkRegistered();
				mbeanServer.removeNotificationListener(name, listener);
				sendMessage(listener+" removed from "+name);
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.startsWith(UNREGISTER_MBEAN_CMD)) {
			try {
				ObjectName name = ObjectName.getInstance(cmd.substring(UNREGISTER_MBEAN_CMD.length()));
				checkRegistered();
				mbeanServer.unregisterMBean(name);
				sendMessage("Unregistered "+name);
			} catch(Exception e) {
				sendException(e);
			}
		} else if(cmd.equals(HELP_CMD)) {
			sendMessage(LIST_MBEANS_CMD + " [query] - lists all MBeans");
			sendMessage(CREATE_MBEAN_CMD + "<MBean name> <class name> <param 1>"+PARAM_SEPARATOR+"..."+PARAM_SEPARATOR+"<param N> - creates an MBean");
			sendMessage(SHOW_MBEAN_CMD + "<MBean name> - shows an MBean info");
			sendMessage(SET_ATTRIBUTE_CMD + "<MBean name> <attribute name> <attribute value> - sets an MBean attribute");
			sendMessage(INVOKE_OPERATION_CMD + "<MBean name> <operation name> <param 1>"+PARAM_SEPARATOR+"..."+PARAM_SEPARATOR+"<param N> - invokes an MBean operation");
			sendMessage(ADD_LISTENER_CMD + "<MBean name> <MBean listener> - adds a listener to an MBean");
			sendMessage(REMOVE_LISTENER_CMD + "<MBean name> <MBean listener> - removes a listener from an MBean");
			sendMessage(UNREGISTER_MBEAN_CMD + "<MBean name> - unregisters an MBean");
			sendMessage(HELP_CMD + " - displays this command list");
		} else {
			sendMessage("Unrecognised command: "+cmd);
		}
	}
	private void sendException(Throwable e) throws JMSException {
		sendMessage(e.toString());
		if(e.getCause() != null)
			sendException(e.getCause());
	}
	private void checkRegistered() throws IllegalStateException {
		if(mbeanServer == null)
			throw new IllegalStateException("Adaptor not registered with an MBean server");
	}
	public void handleNotification(Notification notification, Object handback) {
		try {
			sendMessage("#"+notification.getSequenceNumber()+" "+new Date(notification.getTimeStamp())+" type="+notification.getType()+" from "+notification.getSource()+": "+notification.getMessage()+" ("+notification.getUserData()+")");
		} catch(JMSException jmse) {
			System.err.println(jmse.getMessage());
		}
	}

	private class Listener implements MessageListener {
		public void onMessage(Message message) {
			if(message instanceof TextMessage) {
				try {
					handleCommand(((TextMessage)message).getText());
				} catch(JMSException jmse) {
					System.err.println(jmse.getMessage());
				}
			}
		}
	}
}
