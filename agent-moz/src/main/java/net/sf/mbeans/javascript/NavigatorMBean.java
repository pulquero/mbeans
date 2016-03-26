package net.sf.mbeans.javascript;

public interface NavigatorMBean extends JavaScriptMBean {
	String getPlatform();
	String getOperatingSystem();
	String getUserAgent();
	String getLanguage();
	String getApplicationName();
	String getApplicationVersion();
	String getApplicationCodeName();
	String getProductName();
	String getProductVersion();
	String getVendorName();
	String getVendorVersion();
	Boolean isCookieEnabled();
}
