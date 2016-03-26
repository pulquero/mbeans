package managency.remote;

import javax.management.remote.JMXConnectorServerMBean;
import managency.mbean.StartableMBean;

public interface ConnectorServerMBean extends JMXConnectorServerMBean, StartableMBean {}
