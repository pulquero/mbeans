package managency.mbean;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Boinc implements BoincMBean {
	private static final int DEFAULT_PORT = 31416;
	private static final int MAJOR_VERSION = 5;
	private static final int MINOR_VERSION = 0;
	private static final int RELEASE = 0;
	/** End-of-message */
	private static final char EOM = 3;
	private static final int UPDATE_INTERVAL = 10*60*1000;
	private final MessageDigest digester;
	private final Socket socket;
	private final BufferedWriter writer;
	private final BufferedReader reader;
	private final Map hostInfo = new HashMap();
	private final Map projects = new HashMap();
	private final Map results = new HashMap();
	private String platformName;
	private String version;
	private long lastUpdate = 0;

	public Boinc(String host, String password) throws GeneralSecurityException, IOException {
		digester = MessageDigest.getInstance("MD5");
		socket = new Socket(host, DEFAULT_PORT);
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "US-ASCII"));
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
		if(password != null) {
			authenticate(password);
		}
		getState();
	}

	public String getCPUVendor() {
		return (String) hostInfo.get("p_vendor");
	}
	public String getCPUModel() {
		return (String) hostInfo.get("p_model");
	}
	/** Whetstone */
	public double getFloatPerformance() {
		return Double.parseDouble((String) hostInfo.get("p_fpops"));
	}
	/** Dhrystone */
	public double getIntPerformance() {
		return Double.parseDouble((String) hostInfo.get("p_iops"));
	}
	public String getOSName() {
		return (String) hostInfo.get("os_name");
	}
	public String getOSVersion() {
		return (String) hostInfo.get("os_version");
	}

	public String getPlatformName() {
		return platformName;
	}
	public String getVersion() {
		return version;
	}

	public String[] getProjectURLs() {
		return (String[]) projects.keySet().toArray(new String[projects.size()]);
	}
	public String getProjectName(String url) {
		return getProperty(projects, url, "project_name");
	}
	public String getProjectCredit(String url) {
		return getProperty(projects, url, "user_total_credit");
	}
	public String getHostCredit(String url) {
		return getProperty(projects, url, "host_total_credit");
	}

	public String[] getResultNames() {
		try {
			update();
			return (String[]) results.keySet().toArray(new String[results.size()]);
		} catch(IOException ioe) {
			System.err.println(ioe);
			return null;
		}
	}
	public double getProgress(String name) {
		try {
			update();
			return Double.parseDouble(getProperty(results, name, "fraction_done"));
		} catch(IOException ioe) {
			System.err.println(ioe);
			return Double.NaN;
		}
	}
	public void setRunMode(String mode) throws IOException {
		sendRequest("<set_run_mode><"+mode+"/></set_run_mode>");
	}

	private void authenticate(String password) throws IOException, GeneralSecurityException {
		sendRequest("<auth1/>");
		String nonce = null;
		String line = reader.readLine();
		while(!isEOM(line)) {
			if(line.indexOf("<nonce>") != -1) {
				nonce = getTagValue(line);
			}
			line = reader.readLine();
		}
		if(nonce == null)
			throw new GeneralSecurityException("Reply not recognised");

		String hash = encode(nonce+password);
		sendRequest("<auth2/><nonce_hash>"+hash+"</nonce_hash>");
		boolean auth = false;
		line = reader.readLine();
		while(!isEOM(line)) {
			if(line.indexOf("<authorized/>") != -1) {
				auth = true;
			}
			line = reader.readLine();
		}
		if(!auth)
			throw new GeneralSecurityException("Unauthorized");
	}
	private void getState() throws IOException {
		hostInfo.clear();
		projects.clear();

		sendRequest("<get_state/>");
		String parentTag = null;
		String projectURL = null;
		Map project = null;
		String line = reader.readLine();
		while(!isEOM(line)) {
			String tag = getTag(line);
			if("<host_info>".equals(tag)) {
				parentTag = tag;
			} else if("</host_info>".equals(tag)) {
				parentTag = null;
			} else if("<host_info>".equals(parentTag)) {
				String name = getTagName(tag);
				String value = getTagValue(line);
				putProperty(hostInfo, name, value);
			} else if("<project>".equals(tag)) {
				parentTag = tag;
				project = new HashMap();
			} else if("</project>".equals(tag)) {
				projects.put(projectURL, project);
				parentTag = null;
			} else if("<project>".equals(parentTag)) {
				if("<master_url>".equals(tag)) {
					projectURL = getTagValue(line);
				} else if("<gui_urls>".equals(tag)) {
					parentTag = tag;
				} else if("</gui_urls>".equals(tag)) {
					parentTag = null;
				} else {
					String name = getTagName(tag);
					String value = getTagValue(line);
					putProperty(project, name, value);
				}
			} else if("<gui_urls>".equals(parentTag)) {
				// ignore
			} else if("<app_version>".equals(tag)) {
				parentTag = tag;
			} else if("</app_version>".equals(tag)) {
				parentTag = null;
			} else if("<app_version>".equals(parentTag)) {
				// ignore
			} else if("<platform_name>".equals(tag)) {
				platformName = getTagValue(line);
			} else if("<core_client_major_version>".equals(tag)) {
				version = getTagValue(line);
			} else if("<core_client_minor_version>".equals(tag)) {
				version = version+'.'+getTagValue(line);
			} else if("<core_client_release>".equals(tag)) {
				version = version+'.'+getTagValue(line);
			}
			line = reader.readLine();
		}
	}
	private void getResults() throws IOException {
		results.clear();

		sendRequest("<get_results/>");
		String resultName = null;
		Map result = null;
		String line = reader.readLine();
		while(!isEOM(line)) {
			String tag = getTag(line);
			if("<result>".equals(tag)) {
				result = new HashMap();
			} else if("</result>".equals(tag)) {
				results.put(resultName, result);
				result = null;
			} else if(result != null) {
				if("<result_name>".equals(tag)) {
					resultName = getTagValue(line);
				} else if(!"<active_task>".equals(tag) && !"</active_task>".equals(tag)) {
					String name = getTagName(tag);
					String value = getTagValue(line);
					putProperty(result, name, value);
				}
			}
			line = reader.readLine();
		}
	}
	private void update() throws IOException {
		final long now = System.currentTimeMillis();
		if(now - lastUpdate > UPDATE_INTERVAL) {
			getResults();
			lastUpdate = now;
		}
	}

	private void sendRequest(String request) throws IOException {
		writer.write("<boinc_gui_rpc_request>");
		writer.write("<major_version>"+MAJOR_VERSION+"</major_version>");
		writer.write("<minor_version>"+MINOR_VERSION+"</minor_version>");
		writer.write("<release>"+RELEASE+"</release>");
		writer.write(request);
		writer.write("</boinc_gui_rpc_request>");
		writer.write(EOM);
		writer.flush();
	}

	private static void putProperty(Map properties, String key, String value) {
		if(value != null) {
			if(value.length() > 0)
				properties.put(key, value);
		} else {
			properties.put(key, Boolean.TRUE);
		}
	}
	private static String getProperty(Map bundles, String name, String key) {
		Map properties = (Map) bundles.get(name);
		if(properties != null)
			return (String) properties.get(key);
		else
			return null;
	}

	private static String getTag(String line) {
		int startPos = line.indexOf('<');
		if(startPos == -1)
			return null;
		int endPos = line.indexOf('>', startPos) + 1;
		return line.substring(startPos, endPos);
	}
	private static String getTagName(String tag) {
		int startPos = 1;
		int endPos = tag.length()-1;
		if(tag.charAt(startPos) == '/')
			startPos++;
		else if(tag.charAt(endPos-1) == '/')
			endPos--;
		return tag.substring(startPos, endPos);
	}
	/** @return null if an empty-tag */
	private static String getTagValue(String line) {
		int startPos = line.indexOf('>') + 1;
		int endPos = line.indexOf('<', startPos);
		if(endPos != -1)
			return line.substring(startPos, endPos);
		else
			return null;
	}
	private boolean isEOM(String line) throws IOException {
		if(line != null && !line.equals("</boinc_gui_rpc_reply>")) {
			return false;
		} else {
			// read end-of-message
			reader.read();
			return true;
		}
	}

	private String encode(String v) throws UnsupportedEncodingException {
		return bytesToHexString(digester.digest(v.getBytes("US-ASCII")));
	}
        private static String bytesToHexString(byte b[]) {
                StringBuffer buffer = new StringBuffer(2*b.length);
                for(int i=0; i<b.length; i++) {
                        buffer.append(Character.forDigit(b[i]>>>4 & 0xF, 16));
                        buffer.append(Character.forDigit(b[i] & 0xF, 16));
                }
                return buffer.toString();
	}
}
