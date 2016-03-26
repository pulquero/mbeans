package managency.mbean;

import javax.management.DynamicMBean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.AttributeNotFoundException;
import javax.management.ReflectionException;

/**
 * An open MBean for the {@link java.lang.Runtime} class.
 * Exposes memory usage and management (garbage collector).
 */
public class RuntimeMBean implements DynamicMBean {
	private static final OpenMBeanAttributeInfo AVAILABLE_PROCESSORS_ATTRIBUTE = new OpenMBeanAttributeInfoSupport("AvailableProcessors", "Number of processors available to the JVM.", SimpleType.INTEGER, true, false, false);
	private static final OpenMBeanAttributeInfo FREE_MEMORY_ATTRIBUTE = new OpenMBeanAttributeInfoSupport("FreeMemory", "Amount of free memory in the JVM.", SimpleType.LONG, true, false, false);
	private static final OpenMBeanAttributeInfo TOTAL_MEMORY_ATTRIBUTE = new OpenMBeanAttributeInfoSupport("TotalMemory", "Total amount of memory in the JVM.", SimpleType.LONG, true, false, false);
	private static final OpenMBeanAttributeInfo MAX_MEMORY_ATTRIBUTE = new OpenMBeanAttributeInfoSupport("MaxMemory", "Maximum amount of memory that the JVM will attempt to use.", SimpleType.LONG, true, false, false);
	private static final OpenMBeanOperationInfo GC_OPERATION = new OpenMBeanOperationInfoSupport("gc", "Runs the garbage collector.", null, SimpleType.VOID, OpenMBeanOperationInfoSupport.ACTION);

	public MBeanInfo getMBeanInfo() {
		OpenMBeanAttributeInfo[] attributes = {
			AVAILABLE_PROCESSORS_ATTRIBUTE,
			FREE_MEMORY_ATTRIBUTE,
			TOTAL_MEMORY_ATTRIBUTE,
			MAX_MEMORY_ATTRIBUTE
		};
		OpenMBeanOperationInfo[] operations = {GC_OPERATION};
		return new OpenMBeanInfoSupport(getClass().getName(), "Runtime environment", attributes, null, operations, null);
	}
	public Object getAttribute(String attribute) throws AttributeNotFoundException {
		if(AVAILABLE_PROCESSORS_ATTRIBUTE.getName().equals(attribute)) {
			return new Integer(Runtime.getRuntime().availableProcessors());
		} else if(FREE_MEMORY_ATTRIBUTE.getName().equals(attribute)) {
			return new Long(Runtime.getRuntime().freeMemory());
		} else if(TOTAL_MEMORY_ATTRIBUTE.getName().equals(attribute)) {
			return new Long(Runtime.getRuntime().totalMemory());
		} else if(MAX_MEMORY_ATTRIBUTE.getName().equals(attribute)) {
			return new Long(Runtime.getRuntime().maxMemory());
		} else {
			throw new AttributeNotFoundException("No such attribute "+attribute);
		}
	}
	public AttributeList getAttributes(String[] attributes) {
		AttributeList list = new AttributeList(attributes.length);
		for(int i=0; i<attributes.length; i++) {
			try {
				list.add(new Attribute(attributes[i], getAttribute(attributes[i])));
			} catch(AttributeNotFoundException anfe) {}
		}
		return list;
	}
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException {
		throw new AttributeNotFoundException("No such attribute "+attribute.getName());
	}
	public AttributeList setAttributes(AttributeList attributes) {
		return new AttributeList();
	}
	public Object invoke(String opName, Object[] params, String[] signature) throws ReflectionException {
		if(GC_OPERATION.getName().equals(opName)) {
			Runtime.getRuntime().gc();
			return null;
		} else {
			throw new ReflectionException(new NoSuchMethodException(opName), "No such operation "+opName);
		}
	}
}
