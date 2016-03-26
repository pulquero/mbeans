package managency.util;

public abstract class Task implements Runnable {
	private boolean hasResult = false;
	private Object result;
	private Exception exception;

	protected abstract Object call();
	public final void run() {
		try {
			set(call());
		} catch(Exception e) {
			exception = e;
			set(null);
		}
	}
	private synchronized void set(Object o) {
		result = o;
		hasResult = true;
		notifyAll();
	}
	public synchronized Object get() {
		while(!hasResult) {
			try {
				wait();
			} catch(InterruptedException ie) {}
		}
		return result;
	}
	public Exception getException() {
		return exception;
	}
}
