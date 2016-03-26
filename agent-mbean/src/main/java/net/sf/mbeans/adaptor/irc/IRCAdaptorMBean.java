package net.sf.mbeans.adaptor.irc;

import net.sf.mbeans.PircBotMBean;
import net.sf.mbeans.StartableMBean;

public interface IRCAdaptorMBean extends PircBotMBean, StartableMBean {
	void setCommandPrefix(String prefix);
	String getCommandPrefix();
}
