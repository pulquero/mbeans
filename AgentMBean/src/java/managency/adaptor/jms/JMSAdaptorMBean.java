package managency.adaptor.jms;

import javax.jms.JMSException;
import managency.mbean.StartableMBean;

public interface JMSAdaptorMBean extends StartableMBean {
	void sendMessage(String text) throws JMSException;
}
