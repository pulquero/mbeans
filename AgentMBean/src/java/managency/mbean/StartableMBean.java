package managency.mbean;

/**
 * Management interface for MBeans with a simple lifecycle.
 */
public interface StartableMBean {
	void start() throws Exception;
	void stop() throws Exception;
}
