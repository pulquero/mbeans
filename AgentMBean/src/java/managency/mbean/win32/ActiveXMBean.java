package managency.mbean.win32;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import javax.management.DynamicMBean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.NotificationBroadcasterSupport;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.DispatchEvents;
import com.jacob.com.Variant;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import managency.mbean.StartableMBean;
import managency.util.MBeanUtilities;
import managency.util.MBeanInfoUtilities;
import managency.util.RunnableQueue;
import managency.util.Task;
import managency.util.ByteArrayClassLoader;

public class ActiveXMBean extends NotificationBroadcasterSupport implements MBeanRegistration, DynamicMBean, StartableMBean, InvocationHandler {
	private static final String NOTIFICATION_TYPE_PREFIX = "managency.activeX.";
	private static final String ACTIVE_ATTRIBUTE_NAME = "Active";
	private final MBeanInfo info;
	private ObjectName name;
	private final String componentID;
	private ActiveXComponent component;
	private final String eventsID;
	private DispatchEvents eventDispatcher;
	private final Object activeXListener;
	private final RunnableQueue comWorker = new RunnableQueue();
	private long sequenceNumber;

	public ActiveXMBean(String descriptorURL) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		DescriptorHandler desc = new DescriptorHandler();
		parser.parse(new InputSource(descriptorURL), desc);
		info = desc.getMBeanInfo();
		componentID = desc.getComponentID();
		eventsID = desc.getEventsID();
		if(eventsID != null) {
			Class listenerClass = desc.getActiveXListenerClass();
			activeXListener = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[] {listenerClass}, this);
		} else {
			activeXListener = null;
		}
	}

	public ObjectName preRegister(MBeanServer server, ObjectName name) {
		this.name = name;
		return name;
	}
	public void postRegister(Boolean registrationDone) {}
	public void preDeregister() {}
	public void postDeregister() {}

	public MBeanInfo getMBeanInfo() {
		return info;
	}
	public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException {
		if(ACTIVE_ATTRIBUTE_NAME.equals(attribute)) {
			return Boolean.valueOf(isActive());
		} else if(MBeanInfoUtilities.hasAttribute(info, attribute)) {
			if(!isActive())
				throw new MBeanException(new IllegalStateException("STA not started"));

			Task getTask = new Task() {
				protected Object call() {
					return getActiveXProperty(component, attribute);
				}
			};
			Object value;
			// guard against start/stop
			synchronized(this) {
				comWorker.execute(getTask);
				value = getTask.get();
			}
			if(getTask.getException() != null)
				throw new MBeanException(getTask.getException());
			return value;
		} else {
			throw new AttributeNotFoundException("No such attribute "+attribute);
		}
	}
	public AttributeList getAttributes(final String[] attributes) {
		if(!isActive()) {
			AttributeList list = new AttributeList();
			for(int i=0; i<attributes.length; i++) {
				String attribute = attributes[i];
				if(ACTIVE_ATTRIBUTE_NAME.equals(attribute))
					list.add(new Attribute(attribute, Boolean.FALSE));
			}
			return list;
		} else {
			Task getTask = new Task() {
				protected Object call() {
					AttributeList list = new AttributeList(attributes.length);
					for(int i=0; i<attributes.length; i++) {
						String attribute = attributes[i];
						if(ACTIVE_ATTRIBUTE_NAME.equals(attribute)) {
							list.add(new Attribute(attribute, Boolean.TRUE));
						} else if(MBeanInfoUtilities.hasAttribute(info, attribute)) {
							try {
								Object value = getActiveXProperty(component, attribute);
								list.add(new Attribute(attribute, value));
							} catch(Exception e) {
								// do not add to list
								System.err.println(e.toString());
							}
						}
					}
					return list;
				}
			};
			AttributeList list;
			// guard against start/stop
			synchronized(this) {
				comWorker.execute(getTask);
				list = (AttributeList) getTask.get();
			}
			return list;
		}
	}
	public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, MBeanException {
		if(!MBeanInfoUtilities.hasAttribute(info, attribute.getName()))
			throw new AttributeNotFoundException("No such attribute "+attribute);
		if(!isActive())
			throw new MBeanException(new IllegalStateException("STA not started"));

		Task setTask = new Task() {
			protected Object call() {
				setActiveXProperty(component, attribute.getName(), attribute.getValue());
				return null;
			}
		};
		// guard against start/stop
		synchronized(this) {
			comWorker.execute(setTask);
			setTask.get();
		}
		if(setTask.getException() != null)
			throw new MBeanException(setTask.getException());
	}
	public AttributeList setAttributes(final AttributeList attributes) {
		if(!isActive())
			return new AttributeList();

		Task setTask = new Task() {
			protected Object call() {
				AttributeList list = new AttributeList(attributes.size());
				for(int i=0; i<attributes.size(); i++) {
					Attribute attr = (Attribute) attributes.get(i);
					try {
						setActiveXProperty(component, attr.getName(), attr.getValue());
						list.add(attr);
					} catch(Exception e) {
						// do not add to list
						System.err.println(e.toString());
					}
				}
				return list;
			}
		};
		AttributeList list;
		// guard against start/stop
		synchronized(this) {
			comWorker.execute(setTask);
			list = (AttributeList) setTask.get();
		}
		return list;
	}
	public Object invoke(final String opName, final Object[] params, final String[] signature) throws ReflectionException, MBeanException {
		if(MBeanUtilities.START_OPERATION_NAME.equals(opName) && (signature == null || signature.length == 0)) {
			start();
			return null;
		} else if(MBeanUtilities.STOP_OPERATION_NAME.equals(opName) && (signature == null || signature.length == 0)) {
			stop();
			return null;
		} else if(MBeanInfoUtilities.hasOperation(info, opName, signature)) {
			if(!isActive())
				throw new MBeanException(new IllegalStateException("STA not started"));

			Task invokeTask = new Task() {
				protected Object call() {
					return invokeActiveXMethod(component, opName, params);
				}
			};
			Object result;
			// guard against start/stop
			synchronized(this) {
				comWorker.execute(invokeTask);
				result = invokeTask.get();
			}
			if(invokeTask.getException() != null)
				throw new MBeanException(invokeTask.getException());
			return result;
		} else {
			throw new ReflectionException(new NoSuchMethodException(opName), "No such operation "+opName);
		}
	}

	private static final char SEPARATOR = '_';
	private static Object getActiveXProperty(ActiveXComponent component, String qualifiedProperty) {
		String property;
		int pos = qualifiedProperty.lastIndexOf(SEPARATOR);
		if(pos != -1) {
			property = qualifiedProperty.substring(pos+1);
			component = getComponent(component, qualifiedProperty.substring(0, pos));
		} else {
			property = qualifiedProperty;
		}
		Variant var = component.getProperty(property);
		return variantToObject(var);
	}
	private static void setActiveXProperty(ActiveXComponent component, String qualifiedProperty, Object value) {
		String property;
		int pos = qualifiedProperty.lastIndexOf(SEPARATOR);
		if(pos != -1) {
			property = qualifiedProperty.substring(pos+1);
			component = getComponent(component, qualifiedProperty.substring(0, pos));
		} else {
			property = qualifiedProperty;
		}
		Variant var = objectToVariant(value);
		component.setProperty(property, var);
	}
	private static Object invokeActiveXMethod(ActiveXComponent component, String qualifiedMethod, Object[] params) {
		String method;
		int pos = qualifiedMethod.lastIndexOf(SEPARATOR);
		if(pos != -1) {
			method = qualifiedMethod.substring(pos+1);
			component = getComponent(component, qualifiedMethod.substring(0, pos));
		} else {
			method = qualifiedMethod;
		}
		Variant[] vars = objectsToVariants(params);
		Variant var = component.invoke(method, vars);
		return variantToObject(var);
	}
	private static ActiveXComponent getComponent(ActiveXComponent component, String qualifiedProperty) {
		if(qualifiedProperty == null || qualifiedProperty.length() == 0)
			return component;

		int pos = qualifiedProperty.indexOf(SEPARATOR);
		while(pos != -1) {
			String property = qualifiedProperty.substring(0, pos);
			component = component.getPropertyAsComponent(property);
			qualifiedProperty = qualifiedProperty.substring(pos+1);
			pos = qualifiedProperty.indexOf(SEPARATOR);
		}
		return component.getPropertyAsComponent(qualifiedProperty);
	}
	private static Object variantToObject(Variant var) {
		if(var == null)
			return null;

		switch(var.getvt()) {
			case Variant.VariantBoolean: return Boolean.valueOf(var.getBoolean());
			case Variant.VariantInt: return new Integer(var.getInt());
			case Variant.VariantString: return var.getString();
			case Variant.VariantDouble: return new Double(var.getDouble());
			case Variant.VariantFloat: return new Float(var.getFloat());
			case Variant.VariantShort: return new Short(var.getShort());
			case Variant.VariantByte: return new Byte(var.getByte());
			default: return null;
		}
	}
	private static Object[] variantsToObjects(Variant[] vars) {
		if(vars == null)
			return new Object[0];

		Object[] objs = new Object[vars.length];
		for(int i=0; i<vars.length; i++)
			objs[i] = variantToObject(vars[i]);
		return objs;
	}
	private static Variant objectToVariant(Object obj) {
		return new Variant(obj);
	}
	private static Variant[] objectsToVariants(Object[] objs) {
		if(objs == null)
			return new Variant[0];

		Variant[] vars = new Variant[objs.length];
		for(int i=0; i<objs.length; i++)
			vars[i] = objectToVariant(objs[i]);
		return vars;
	}

	public boolean isActive() {
		return comWorker.isActive() && component != null;
	}
	public synchronized void start() {
		if(isActive())
			return;

		comWorker.start();
		Task startTask = new Task() {
			protected Object call() {
				ComThread.InitMTA();
				component = new ActiveXComponent(componentID);
				if(eventsID != null)
					eventDispatcher = new DispatchEvents(component, activeXListener, eventsID);
				return null;
			}
		};
		comWorker.execute(startTask);
		startTask.get();
	}
	public synchronized void stop() {
		if(!isActive())
			return;

		Task stopTask = new Task() {
			protected Object call() {
				if(eventDispatcher != null) {
					// we want to unregister the event listener
					eventDispatcher.safeRelease();
					eventDispatcher = null;
				}
				if(component != null) {
					component.safeRelease();
					component = null;
				}
				ComThread.Release();
				return null;
			}
		};
		comWorker.execute(stopTask);
		stopTask.get();
		comWorker.stop();
	}

	public Object invoke(Object proxy, Method method, Object[] args) {
		if(proxy != activeXListener)
			throw new IllegalStateException("Illegal proxy object");

		String event = method.getName();
		Variant[] vars = (Variant[]) args[0];
		System.err.println("ActiveX event: "+event+" "+Arrays.asList(vars));
		sendNotification(new ActiveXNotification(NOTIFICATION_TYPE_PREFIX+event, name, ++sequenceNumber, variantsToObjects(vars)));
		return null;
	}

	/**
	 * SAX handler.
	 */
	private static class DescriptorHandler extends DefaultHandler {
		private String componentID;
		private final List attrInfos = new ArrayList();
		private final List opInfos = new ArrayList();
		private String eventsID;
		private final List eventTypes = new ArrayList();
		private String name;
		private OpenType type;
		private boolean readOnly;
		private final List paramInfos = new ArrayList();

		public DescriptorHandler() {
			attrInfos.add(new OpenMBeanAttributeInfoSupport(ACTIVE_ATTRIBUTE_NAME, "STA active", SimpleType.BOOLEAN, true, false, true));
			opInfos.add(new OpenMBeanOperationInfoSupport(MBeanUtilities.START_OPERATION_NAME, "Start STA", null, SimpleType.VOID, MBeanOperationInfo.ACTION));
			opInfos.add(new OpenMBeanOperationInfoSupport(MBeanUtilities.STOP_OPERATION_NAME, "Stop STA", null, SimpleType.VOID, MBeanOperationInfo.ACTION));
		}
		public MBeanInfo getMBeanInfo() {
			OpenMBeanAttributeInfoSupport[] attributes = (OpenMBeanAttributeInfoSupport[]) attrInfos.toArray(new OpenMBeanAttributeInfoSupport[0]);
			OpenMBeanOperationInfoSupport[] operations = (OpenMBeanOperationInfoSupport[]) opInfos.toArray(new OpenMBeanOperationInfoSupport[0]);
			MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[1];
			notifications[0] = new MBeanNotificationInfo((String[]) eventTypes.toArray(new String[0]), ActiveXNotification.class.getName(), eventsID);
			return new OpenMBeanInfoSupport(ActiveXMBean.class.getName(), componentID, attributes, null, operations, notifications);
		}
		public String getComponentID() {
			return componentID;
		}
		public String getEventsID() {
			return eventsID;
		}
		public Class getActiveXListenerClass() {
			ByteArrayClassLoader cl = new ByteArrayClassLoader(ActiveXMBean.class.getClassLoader());
			byte[] classData = generateActiveXListenerClass("managency/mbean/win32/ActiveXEventListener");
			return cl.loadClass("managency.mbean.win32.ActiveXEventListener", classData);
		}
		private byte[] generateActiveXListenerClass(String className) {
			ClassWriter cw = new ClassWriter(false);
			cw.visit(Opcodes.V1_3, Opcodes.ACC_ABSTRACT+Opcodes.ACC_INTERFACE, className, null, "java/lang/Object", null);
			for(int i=0; i<eventTypes.size(); i++) {
				String methodName = ((String) eventTypes.get(i)).substring(NOTIFICATION_TYPE_PREFIX.length());
				MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_ABSTRACT, methodName, "([Lcom/jacob/com/Variant;)V", null, null);
				mv.visitEnd();
			}
			cw.visitEnd();
			return cw.toByteArray();
		}

		public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
			if(qName.equals("activeX")) {
				componentID = attrs.getValue("id");
			} else if(qName.equals("property")) {
				name = qualifiedNameToMemberName(attrs.getValue("name"));
				type = variantTypeToOpenType(attrs.getValue("type"));
				String value = attrs.getValue("readOnly");
				if(value != null)
					readOnly = Boolean.valueOf(value).booleanValue();
				else
					readOnly = false;
			} else if(qName.equals("method")) {
				name = qualifiedNameToMemberName(attrs.getValue("name"));
				type = variantTypeToOpenType(attrs.getValue("type"));
			} else if(qName.equals("parameter")) {
				String name = attrs.getValue("name");
				OpenType type = variantTypeToOpenType(attrs.getValue("type"));
				paramInfos.add(new OpenMBeanParameterInfoSupport(name, name, type));
			} else if(qName.equals("events")) {
				eventsID = attrs.getValue("id");
			} else if(qName.equals("event")) {
				name = attrs.getValue("name");
			}
		}
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(qName.equals("property")) {
				attrInfos.add(new OpenMBeanAttributeInfoSupport(name, "Property", type, true, !readOnly, false));
			} else if(qName.equals("method")) {
				OpenMBeanParameterInfoSupport[] signature = (OpenMBeanParameterInfoSupport[]) paramInfos.toArray(new OpenMBeanParameterInfoSupport[0]);
				opInfos.add(new OpenMBeanOperationInfoSupport(name, "Method", signature, type, MBeanOperationInfo.ACTION));
				paramInfos.clear();
			} else if(qName.equals("event")) {
				eventTypes.add(NOTIFICATION_TYPE_PREFIX+name);
			}
		}

		private static String qualifiedNameToMemberName(String name) {
			return name.replace('.', SEPARATOR);
		}
		private static OpenType variantTypeToOpenType(String type) {
			if(type.equals("void"))
				return SimpleType.VOID;
			else if(type.equals("boolean"))
				return SimpleType.BOOLEAN;
			else if(type.equals("int"))
				return SimpleType.INTEGER;
			else if(type.equals("String"))
				return SimpleType.STRING;
			else if(type.equals("double"))
				return SimpleType.DOUBLE;
			else if(type.equals("float"))
				return SimpleType.FLOAT;
			else if(type.equals("short"))
				return SimpleType.SHORT;
			else if(type.equals("byte"))
				return SimpleType.BYTE;
			else
				return null;
		}
	}
}
