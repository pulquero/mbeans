package managency.mbean.linux;

public interface OperatingSystemMBean {
	long getUptime();

	long getTotalPhysicalMemory();
	long getAvailablePhysicalMemory();
	long getTotalVirtualMemory();
	long getAvailableVirtualMemory();
	long getSharedMemory();
	long getBufferMemory();
	short getProcessCount();
}
