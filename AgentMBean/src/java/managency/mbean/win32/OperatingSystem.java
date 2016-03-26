package managency.mbean.win32;

/**
 * Requires <code>win32mbean</code> native library.
 */
public class OperatingSystem implements OperatingSystemMBean {
	private static final short UNKNOWN_ARCH = (short) 0xFFFF;
	private static final short INTEL_ARCH = 0;
	private static final short MIPS_ARCH = 1;
	private static final short ALPHA_ARCH = 2;
	private static final short PPC_ARCH = 3;
	private static final short SHX_ARCH = 4;
	private static final short ARM_ARCH = 5;
	private static final short IA64_ARCH = 6;
	private static final short ALPHA64_ARCH = 7;
	private static final short MSIL_ARCH = 8;
	private static final short AMD64_ARCH = 9;

	static {
		System.loadLibrary("win32mbean");
	}

	/**
	 * Returns the system uptime in milliseconds.
	 */
	public native long getUptime();

	public native long getTotalPhysicalMemory();
	public native long getAvailablePhysicalMemory();
	public native long getTotalVirtualMemory();
	public native long getAvailableVirtualMemory();

	public String getProcessorName() {
		byte[] ansiBytes = new byte[256];
		int rc = getProcessorNameBytes(ansiBytes);
		if(rc == 0) {
			int endPos = 0;
			while(endPos < ansiBytes.length && ansiBytes[endPos] != 0)
				endPos++;
			return new String(ansiBytes, 0, endPos);
		} else {
			return null;
		}
	}
	private native int getProcessorNameBytes(byte[] ansiBytes);
	public String getProcessorVendor() {
		byte[] ansiBytes = new byte[256];
		int rc = getProcessorVendorBytes(ansiBytes);
		if(rc == 0) {
			int endPos = 0;
			while(endPos < ansiBytes.length && ansiBytes[endPos] != 0)
				endPos++;
			return new String(ansiBytes, 0, endPos);
		} else {
			return null;
		}
	}
	private native int getProcessorVendorBytes(byte[] ansiBytes);
	public String getProcessorArchitecture() {
		switch(getProcessorArch()) {
			case INTEL_ARCH: return "Intel";
			case IA64_ARCH: return "IA64";
			case AMD64_ARCH: return "AMD64";
			default: return "Unknown";
		}
	}
	private native short getProcessorArch();
	/**
	 * Returns the processor speed in MHz.
	 */
	public native int getProcessorSpeed();
	public native short getProcessorFamily();
	public native short getProcessorModel();
	public native short getProcessorStepping();
	public native boolean isMMXProcessor();
	public native boolean isSSEProcessor();
	public native boolean isSSE2Processor();
	public native boolean is3DNowProcessor();
}
