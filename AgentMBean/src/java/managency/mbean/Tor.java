package managency.mbean;

import java.io.*;
import java.net.URL;
import java.util.zip.InflaterInputStream;
import managency.util.ObjectUtilities;

public class Tor implements TorMBean {
	private final String host;
	private final int port;

	public Tor() {
		this("18.244.0.188", 9031);
	}
	public Tor(String host, int port) {
		this.host = host;
		this.port = port;
	}
	public void exportRouterList(String xml) throws IOException {
		URL torStatus = new URL("http", host, port, "/tor/status/authority.z");
		InputStream in = new InflaterInputStream(torStatus.openStream());
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "US-ASCII"));
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(xml), "UTF8"));
		try {
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<tor>");
			String line = reader.readLine();
			while(line != null) {
				if(line.startsWith("r ")) {
					line = line.substring(2);
					String[] desc = ObjectUtilities.split(line, ' ');
					line = reader.readLine();
					String status = line.startsWith("s ") ? line.substring(2) : "";
					writer.println("<router name=\""+desc[0]+"\" ip=\""+desc[5]+"\" or-port=\""+desc[6]+"\" status=\""+status+"\"/>");
				}
				line = reader.readLine();
			}
			writer.println("</tor>");
		} finally {
			writer.close();
		}
	}
}
