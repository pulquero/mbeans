package managency.adaptor.irc;

import managency.mbean.PircBotMBean;
import managency.mbean.StartableMBean;

public interface IRCAdaptorMBean extends PircBotMBean, StartableMBean {
	void setCommandPrefix(String prefix);
	String getCommandPrefix();
}
