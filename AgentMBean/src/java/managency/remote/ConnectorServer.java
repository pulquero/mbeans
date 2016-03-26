package managency.remote;

import java.io.Serializable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;

/**
 * A JMX connector server.
 */
public class ConnectorServer extends JMXConnectorServer implements ConnectorServerMBean {
	private final JMXServiceURL url;
	private final Map environment;
	private JMXConnectorServer connectorServer;

	public ConnectorServer(String url) throws MalformedURLException {
		this(url, Collections.EMPTY_MAP);
	}
	public ConnectorServer(String url, Map env) throws MalformedURLException {
		this(new JMXServiceURL(url), env);
	}
	public ConnectorServer(JMXServiceURL url) {
		this(url, Collections.EMPTY_MAP);
	}
	public ConnectorServer(JMXServiceURL url, Map env) {
		this.url = url;
		this.environment = (env != null) ? env : Collections.EMPTY_MAP;
	}

	public JMXServiceURL getAddress() {
		return url;
	}
	public Map getAttributes() {
		Map attrs = new HashMap(environment.size());
		for(Iterator iter = environment.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry entry = (Map.Entry) iter.next();
			if(entry.getValue() instanceof Serializable)
				attrs.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(attrs);
	}
	/**
	 * Returns true if the connector server has been started.
	 */
	public boolean isActive() {
		return (connectorServer != null) && connectorServer.isActive();
	}
	public void start() throws IOException {
		connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, environment, getMBeanServer());
		connectorServer.start();
	}
	public void stop() throws IOException {
		connectorServer.stop();
	}
}
