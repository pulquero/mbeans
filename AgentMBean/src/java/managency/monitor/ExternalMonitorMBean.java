package managency.monitor;

import managency.mbean.StartableMBean;

public interface ExternalMonitorMBean extends StartableMBean {
	long getGranularityPeriod();
	void setGranularityPeriod(long period);
	boolean isActive();
}
