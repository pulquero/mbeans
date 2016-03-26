package net.sf.mbeans.adaptor.jms;

import javax.jms.JMSException;

import net.sf.mbeans.StartableMBean;

public interface JMSAdaptorMBean extends StartableMBean {
	void sendMessage(String text) throws JMSException;
}
