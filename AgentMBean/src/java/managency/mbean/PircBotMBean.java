package managency.mbean;

import java.net.InetAddress;

/**
 * Management interface for IRC bots based on the <a href="http://www.jibble.org/pircbot.php">PircBot</a> API.
 */
public interface PircBotMBean {
        String getNick();
        String getLogin();
        String getVersion();
        String getFinger();
        String getServer();
        int getPort();
	InetAddress getInetAddress();
        String[] getChannels();
	void setDccInetAddress(InetAddress addr);
	InetAddress getDccInetAddress();
	void setDccPorts(int[] ports);
	int[] getDccPorts();
        void setMessageDelay(long delay);
        long getMessageDelay();
        int getOutgoingQueueSize();
        boolean isConnected();
        void setVerbose(boolean verbose);
        void changeNick(String nick);
        void joinChannel(String channel);
        void partChannel(String channel);
	void sendMessage(String target, String message);
	void sendNotice(String target, String notice);
	void sendAction(String target, String action);
}
