package net.sf.mbeans;

import java.util.Properties;
import java.util.Enumeration;
import javax.management.DynamicMBean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.AttributeNotFoundException;
import javax.management.ReflectionException;

/**
 * An open MBean for the {@link java.lang.System} class.
 * Exposes system properties as attributes.
 */
public class SystemMBean implements DynamicMBean {
	public MBeanInfo getMBeanInfo() {
		Properties properties = System.getProperties();
		OpenMBeanAttributeInfoSupport[] attributes = new OpenMBeanAttributeInfoSupport[properties.size()];
		int i=0;
		for(Enumeration iter = properties.propertyNames(); iter.hasMoreElements(); )
			attributes[i++] = new OpenMBeanAttributeInfoSupport(propertyNameToAttributeName((String) iter.nextElement()), "A system property", SimpleType.STRING, true, true, false);
		return new OpenMBeanInfoSupport(getClass().getName(), "System", attributes, null, null, null);
	}
	public Object getAttribute(String attribute) throws AttributeNotFoundException {
		String value = System.getProperty(attributeNameToPropertyName(attribute));
		if(value != null) {
			return value;
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
	public void setAttribute(Attribute attribute) {
		System.setProperty(attributeNameToPropertyName(attribute.getName()), (String) attribute.getValue());
	}
	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList list = new AttributeList(attributes.size());
		for(int i=0; i<attributes.size(); i++) {
			setAttribute((Attribute)attributes.get(i));
			list.add(attributes.get(i));
		}
		return list;
	}
	public Object invoke(String opName, Object[] params, String[] signature) throws ReflectionException {
		throw new ReflectionException(new NoSuchMethodException(opName), "No such operation "+opName);
	}

	private static String attributeNameToPropertyName(String name) {
		return name.replace('_', '.');
	}
	private static String propertyNameToAttributeName(String name) {
		return name.replace('.', '_');
	}
}
