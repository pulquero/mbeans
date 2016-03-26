package net.sf.mbeans;

public interface AntMBean {
	String getVersion();
	void setBuildListenerClassName(String className);
	String getBuildListenerClassName();

	void execute(String buildFileName);
	void execute(String buildFileName, String target);
}
