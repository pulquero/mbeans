package managency.mbean;

import java.io.IOException;

public interface BoincMBean {
	String getCPUVendor();
	String getCPUModel();
	double getFloatPerformance();
	double getIntPerformance();
	String getOSName();
	String getOSVersion();

	String getPlatformName();
	String getVersion();

	String[] getProjectURLs();
	String getProjectName(String url);

	String[] getResultNames();
	double getProgress(String name);
	void setRunMode(String mode) throws IOException;
}
