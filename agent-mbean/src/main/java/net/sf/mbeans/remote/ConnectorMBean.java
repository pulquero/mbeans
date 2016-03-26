package net.sf.mbeans.remote;

import java.io.IOException;
import java.util.Map;
import javax.management.remote.JMXServiceURL;

import net.sf.mbeans.StartableMBean;

public interface ConnectorMBean extends StartableMBean {
	String getConnectionId() throws IOException;
	JMXServiceURL getAddress();
	Map getAttributes();
	boolean isConnected();
}
