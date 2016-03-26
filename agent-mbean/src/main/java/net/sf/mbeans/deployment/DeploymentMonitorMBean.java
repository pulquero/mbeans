package net.sf.mbeans.deployment;

import java.io.IOException;
import java.net.URL;
import javax.management.JMException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import net.sf.mbeans.StartableMBean;

public interface DeploymentMonitorMBean extends StartableMBean {
	String getDeploymentDirectory();
	void setDeploymentDirectory(String dir);
	long getMonitorInterval();
	void setMonitorInterval(long interval);
	boolean isActive();
	void deploy(URL url) throws IOException, ParserConfigurationException, SAXException, JMException;
	void undeploy(URL url) throws JMException;
}
