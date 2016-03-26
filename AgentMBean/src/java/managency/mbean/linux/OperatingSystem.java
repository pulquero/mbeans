package managency.mbean.linux;

/**
 * Requires <code>linuxmbean</code> native library.
 */
public class OperatingSystem implements OperatingSystemMBean {
	static {
		System.loadLibrary("linuxmbean");
	}

	/**
	 * Returns the system uptime in milliseconds.
	 */
	public native long getUptime();

	public native long getTotalPhysicalMemory();
	public native long getAvailablePhysicalMemory();
	public native long getTotalVirtualMemory();
	public native long getAvailableVirtualMemory();
	/**
	 * Returns the amount of shared memory (in bytes).
	 */
	public native long getSharedMemory();
	/**
	 * Returns the amount of memory used by buffers (in bytes).
	 */
	public native long getBufferMemory();
	/**
	 * Returns the number of current processes.
	 */
	public native short getProcessCount();
}
