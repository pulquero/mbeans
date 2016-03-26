var err = initInstall("Agent Moz (JMX agent for Mozilla)", "Agent Moz", "0.4");
logComment("initInstall: "+err);

addFile("Agent Moz Chrome", "chrome/AgentMoz.jar", getFolder("Chrome"), "AgentMoz.jar");
addDirectory("Agent Moz Applet", "components", getFolder("Components"), "");

registerChrome(CONTENT | DELAYED_CHROME, getFolder("Chrome", "AgentMoz.jar"), "content/agentmoz/");

if(err == SUCCESS)
   performInstall();
else
   cancelInstall(err);
