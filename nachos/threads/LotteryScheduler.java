package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}
	
	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer tickets from waiting threads
	 *					to the owning thread.
	 * @return	a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		// implement me		
		return new LotteryQueue(transferPriority);
	}
	
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}
	
	protected class LotteryQueue extends ThreadQueue {
		public booelan transferPriority;
		LinkedList<KThread> waitQueue = new LinkedList<KThread>();
		ThreadState lockHolder = null;
		
		public LotteryQueue(booelan transferPriority) {
			this.transferPriority = transferPriority;
		}
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread).waitForAccess(this);
			if (lockHolder != null) {
				lockHolder.effectivePriority += ts.effectivePriority; 
			}
			this.waitQueue.add(ts);						
		}
		
		public KThread nextThread() {
			if (this.waitQueue.size() > 0) {
				//Aplicar la loteria y retornar el elegido!
				
			}
			return null;
		}
    
    	public void acquire(KThread thread) {
			this.lockHolder = thread;			
			if (this.transferPriority) {
				int transferedPriority = 0;
				for (int i = 0; i < waitQueue.size(); i++) {
					transferedPriority = waitQueue.get(i).priority;					
				}
				this.lockHolder.effectivePriority = transferedPriority;
			}
		}

    	public void print() {
			
		}
	}			
}
