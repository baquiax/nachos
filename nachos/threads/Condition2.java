package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param	conditionLock	the lock associated with this condition
	 *				variable. The current thread must hold this
	 *				lock whenever it uses <tt>sleep()</tt>,
	 *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		this.waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The
	 * current thread must hold the associated lock. The thread will
	 * automatically reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		conditionLock.release();
		boolean lastInterruptState = Machine.interrupt().disable();
		this.waitQueue.waitForAccess(KThread.currentThread());		
		KThread.currentThread().sleep();
		Machine.interrupt().restore(lastInterruptState);
		conditionLock.acquire();		
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean lastInterruptState = Machine.interrupt().disable();		
		KThread waitingThread = this.waitQueue.nextThread();
		if (waitingThread!= null)
			waitingThread.ready();
		
		Machine.interrupt().restore(lastInterruptState);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean lastInterruptState = Machine.interrupt().disable();		
		KThread waitingThread;
		while((waitingThread = this.waitQueue.nextThread()) != null) {
			waitingThread.ready();
		}		
		Machine.interrupt().restore(lastInterruptState);
	}

	private Lock conditionLock;
	private ThreadQueue waitQueue;
}
