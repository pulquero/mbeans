package managency.monitor;

import java.net.URL;
import java.net.MalformedURLException;

public interface URLMonitorMBean extends ExternalMonitorMBean {
	void addObservedURL(String url) throws MalformedURLException;
	void addObservedURL(URL url);
	boolean containsObservedURL(String url) throws MalformedURLException;
	boolean containsObservedURL(URL url);
	void removeObservedURL(String url) throws MalformedURLException;
	void removeObservedURL(URL url);
	URL[] getObservedURLs();
}
