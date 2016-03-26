package managency.mbean.win32;

public interface OperatingSystemMBean {
	long getUptime();

	long getTotalPhysicalMemory();
	long getAvailablePhysicalMemory();
	long getTotalVirtualMemory();
	long getAvailableVirtualMemory();

	String getProcessorName();
	String getProcessorVendor();
	String getProcessorArchitecture();
	int getProcessorSpeed();
	short getProcessorFamily();
	short getProcessorModel();
	short getProcessorStepping();
	boolean isMMXProcessor();
	boolean isSSEProcessor();
	boolean isSSE2Processor();
	boolean is3DNowProcessor();
}
