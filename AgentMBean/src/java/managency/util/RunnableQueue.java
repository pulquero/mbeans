package managency.util;

import java.util.LinkedList;

/**
 * Executor.
 */
public class RunnableQueue implements Runnable {
	private final LinkedList queue = new LinkedList();
	private volatile Thread workerThread;

	public void execute(Runnable task) {
		synchronized(queue) {
			queue.addLast(task);
			queue.notifyAll();
		}
	}
	public synchronized void start() {
		if(workerThread == null) {
			workerThread = new Thread(this, "Runnable queue thread");
			workerThread.start();
		}
	}
	public boolean isActive() {
		return workerThread != null && workerThread.isAlive();
	}
	public void run() {
		final Thread currentThread = Thread.currentThread();
		while(currentThread == workerThread) {
			Runnable task = null;
			synchronized(queue) {
				if(!queue.isEmpty()) {
					task = (Runnable) queue.removeFirst();
				} else {
					try {
						queue.wait();
					} catch(InterruptedException ie) {}
					}
				}
			}
			if(task != null) {
				try {
					task.run();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	public synchronized void stop() {
		if(workerThread != null) {
			Thread thr = workerThread;
			workerThread = null;
			thr.interrupt();
		}
	}
}
