package managency.mbean;

import java.io.File;
import org.apache.tools.ant.*;

public class Ant implements AntMBean {
	private String buildListenerClassName;

	public String getVersion() {
		return Main.getAntVersion();
	}
	/**
	 * org.apache.tools.ant.DefaultLogger
	 * org.apache.tools.ant.listener.Log4jListener
	 */
	public void setBuildListenerClassName(String className) {
		buildListenerClassName = className;
	}
	public String getBuildListenerClassName() {
		return buildListenerClassName;
	}

	public void execute(String buildFileName) {
		execute(buildFileName, null);
	}
	public void execute(String buildFileName, String target) {
		Project project = new Project();

		if(buildListenerClassName != null) {
			try {
				BuildListener listener = (BuildListener) Class.forName(buildListenerClassName).newInstance();
				project.addBuildListener(listener);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		try {
			project.fireBuildStarted();
			project.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			project.addReference("ant.projectHelper", helper);
			helper.parse(project, new File(buildFileName));
			project.executeTarget(target != null ? target : project.getDefaultTarget());
			project.fireBuildFinished(null);
		} catch(BuildException be) {
			project.fireBuildFinished(be);
			throw be;
		}
	}
}
