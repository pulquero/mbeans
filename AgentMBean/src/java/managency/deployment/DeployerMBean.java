package managency.deployment;

import javax.management.JMException;
import managency.mbean.StartableMBean;

public interface DeployerMBean extends StartableMBean {
	void deploy() throws JMException;
	void undeploy() throws JMException;
}
