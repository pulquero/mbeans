package managency.mbean;

import java.net.URL;
import javax.management.JMException;

public interface TaskSchedulerMBean extends StartableMBean {
	void schedule(URL url, long delay) throws JMException;
	void schedule(URL url, long delay, long period) throws JMException;
	void scheduleAtFixedRate(URL url, long delay, long period) throws JMException;
	void schedule(URL url, long delay, long period, long occurences, boolean fixedRate) throws JMException;
}
