package net.sf.mbeans.monitor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface InetAddressMonitorMBean extends ExternalMonitorMBean {
	void addObservedInetAddress(String address) throws UnknownHostException;
	void addObservedInetAddress(InetAddress address);
	boolean containsObservedInetAddress(String address) throws UnknownHostException;
	boolean containsObservedInetAddress(InetAddress address);
	void removeObservedInetAddress(String address) throws UnknownHostException;
	void removeObservedInetAddress(InetAddress address);
	InetAddress[] getObservedInetAddresses();
}
