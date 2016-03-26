package managency.mbean.os2;

public interface OperatingSystemMBean {
	long getUptime();

	long getTotalPhysicalMemory();
	long getResidentMemory();
	long getAvailableMemory();
	long getAvailableSharedMemory();
}

