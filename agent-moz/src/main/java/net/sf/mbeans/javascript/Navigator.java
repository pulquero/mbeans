package net.sf.mbeans.javascript;

import netscape.javascript.JSObject;

public class Navigator implements NavigatorMBean {
	private final JSObject navigator;

	public Navigator(JSObject navigator) {
		this.navigator = navigator;
	}

	public String getApplicationName() {
		return (String) navigator.getMember("appName");
	}
	public String getApplicationVersion() {
		return (String) navigator.getMember("appVersion");
	}
	public String getApplicationCodeName() {
		return (String) navigator.getMember("appCodeName");
	}
	public String getProductName() {
		return (String) navigator.getMember("product");
	}
	public String getProductVersion() {
		return (String) navigator.getMember("productSub");
	}
	public String getVendorName() {
		return (String) navigator.getMember("vendor");
	}
	public String getVendorVersion() {
		return (String) navigator.getMember("vendorSub");
	}

	public String getOperatingSystem() {
		return (String) navigator.getMember("oscpu");
	}
	public String getPlatform() {
		return (String) navigator.getMember("platform");
	}
	public String getUserAgent() {
		return (String) navigator.getMember("userAgent");
	}
	public String getLanguage() {
		return (String) navigator.getMember("language");
	}

	public Boolean isCookieEnabled() {
		return (Boolean) navigator.getMember("cookieEnabled");
	}

	public Object evaluate(String js) {
		return navigator.eval(js);
	}
}
