package managency.mbean.os2;

/**
 * Requires <code>os2mbean</code> native library.
 */
public class OperatingSystem implements OperatingSystemMBean {
	private static final short QSV_MS_COUNT = 14;
	private static final short QSV_TOTPHYSMEM = 17;
	private static final short QSV_TOTRESMEM = 18;
	private static final short QSV_TOTAVAILMEM = 19;
	private static final short QSV_MAXSHMEM = 21;

	private static final long systemStartTime;

	static {
		System.loadLibrary("os2mbean");
		systemStartTime = System.currentTimeMillis() - getSysInfo(QSV_MS_COUNT);
	}

	private native static long getSysInfo(short index);

	/**
	 * Returns the system uptime in milliseconds.
	 */
	public long getUptime() {
		return System.currentTimeMillis() - systemStartTime;
	}

	public long getTotalPhysicalMemory() {
		return getSysInfo(QSV_TOTPHYSMEM);
	}
	public long getResidentMemory() {
		return getSysInfo(QSV_TOTRESMEM);
	}
	public long getAvailableMemory() {
		return getSysInfo(QSV_TOTAVAILMEM);
	}
	public long getAvailableSharedMemory() {
		return getSysInfo(QSV_MAXSHMEM);
	}
}

