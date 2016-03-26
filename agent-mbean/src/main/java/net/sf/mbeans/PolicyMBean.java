package net.sf.mbeans;

import java.security.Policy;
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
 * An open MBean for the {@link java.security.Policy} class.
 * Exposes the policy refresh operation.
 */
public class PolicyMBean implements DynamicMBean {
	private static OpenMBeanAttributeInfo CLASS_NAME_ATTRIBUTE = new OpenMBeanAttributeInfoSupport("ClassName", "The class name of the policy object.", SimpleType.STRING, true, false, false);
	private static OpenMBeanOperationInfo REFRESH_OPERATION = new OpenMBeanOperationInfoSupport("refresh", "Refreshes the policy configuration.", null, SimpleType.VOID, OpenMBeanOperationInfoSupport.ACTION);

	public MBeanInfo getMBeanInfo() {
		OpenMBeanAttributeInfo[] attributes = {CLASS_NAME_ATTRIBUTE};
		OpenMBeanOperationInfo[] operations = {REFRESH_OPERATION};
		return new OpenMBeanInfoSupport(getClass().getName(), "Security policy", attributes, null, operations, null);
	}
	public Object getAttribute(String attribute) throws AttributeNotFoundException {
		if(CLASS_NAME_ATTRIBUTE.getName().equals(attribute)) {
			return Policy.getPolicy().getClass().getName();
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
		if(REFRESH_OPERATION.getName().equals(opName)) {
			Policy.getPolicy().refresh();
			return null;
		} else {
			throw new ReflectionException(new NoSuchMethodException(opName), "No such operation "+opName);
		}
	}
}
