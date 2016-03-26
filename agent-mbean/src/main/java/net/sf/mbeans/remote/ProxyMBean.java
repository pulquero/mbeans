package net.sf.mbeans.remote;

import java.io.IOException;
import javax.management.DynamicMBean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.JMException;
import javax.management.JMRuntimeException;

public class ProxyMBean implements DynamicMBean {
	private final MBeanServerConnection connection;
	private final ObjectName remoteName;

	public ProxyMBean(ObjectName remoteName, MBeanServerConnection connection) {
		this.remoteName = remoteName;
		this.connection = connection;
	}
	public Object getAttribute(String attribute) {
		try {
			return connection.getAttribute(remoteName, attribute);
		} catch(JMException jme) {
			throw new JMRuntimeException(jme.getMessage());
		} catch(IOException ioe) {
			throw new JMRuntimeException(ioe.getMessage());
		}
	}
	public void setAttribute(Attribute attribute) {
		try {
			connection.setAttribute(remoteName, attribute);
		} catch(JMException jme) {
			throw new JMRuntimeException(jme.getMessage());
		} catch(IOException ioe) {
			throw new JMRuntimeException(ioe.getMessage());
		}
	}
	public AttributeList getAttributes(String[] attributes) {
		try {
			return connection.getAttributes(remoteName, attributes);
		} catch(JMException jme) {
			throw new JMRuntimeException(jme.getMessage());
		} catch(IOException ioe) {
			throw new JMRuntimeException(ioe.getMessage());
		}
	}
	public AttributeList setAttributes(AttributeList attributes) {
		try {
			return connection.setAttributes(remoteName, attributes);
		} catch(JMException jme) {
			throw new JMRuntimeException(jme.getMessage());
		} catch(IOException ioe) {
			throw new JMRuntimeException(ioe.getMessage());
		}
	}
	public Object invoke(String actionName, Object[] params, String[] signature) {
		try {
			return connection.invoke(remoteName, actionName, params, signature);
		} catch(JMException jme) {
			throw new JMRuntimeException(jme.getMessage());
		} catch(IOException ioe) {
			throw new JMRuntimeException(ioe.getMessage());
		}
	}
	public MBeanInfo getMBeanInfo() {
		try {
			return connection.getMBeanInfo(remoteName);
		} catch(JMException jme) {
			throw new JMRuntimeException(jme.getMessage());
		} catch(IOException ioe) {
			throw new JMRuntimeException(ioe.getMessage());
		}
	}
}
