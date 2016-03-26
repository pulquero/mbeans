package net.sf.mbeans.monitor;

import net.sf.mbeans.StartableMBean;

public interface ExternalMonitorMBean extends StartableMBean {
	long getGranularityPeriod();
	void setGranularityPeriod(long period);
	boolean isActive();
}
