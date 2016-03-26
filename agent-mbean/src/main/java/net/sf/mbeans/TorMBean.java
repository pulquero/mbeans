package net.sf.mbeans;

import java.io.IOException;

public interface TorMBean {
	void exportRouterList(String xml) throws IOException;
}
