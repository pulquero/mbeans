package net.sf.mbeans.deployment;

import javax.management.JMException;

import net.sf.mbeans.StartableMBean;

public interface DeployerMBean extends StartableMBean {
	void deploy() throws JMException;
	void undeploy() throws JMException;
}
