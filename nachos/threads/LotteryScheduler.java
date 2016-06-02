package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
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
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE;
	
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

	@Override
	public boolean increasePriority() {
		boolean status = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(status);
		return true;
	}
	
	@Override
	public boolean decreasePriority() {
		boolean status = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(status);
		return true;
	}

	protected class LotteryQueue extends ThreadQueue {
		public boolean transferPriority;
		LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();
		ThreadState lockHolder = null;		
		Random r;
		
		public LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.r = new Random();
		}
		
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);			
			if (lockHolder != null) {
				lockHolder.effectivePriority += ts.priority; 
			}
			this.waitQueue.add(ts);						
		}
		
		/**
		 * Inspired on:
		 * http://pages.cs.wisc.edu/~remzi/OSTEP/cpu-sched-lottery.pdf		  
		 */
		 
		public KThread nextThread() {
			if (this.waitQueue.size() > 0) {
				//Aplicar la loteria y retornar el elegido!								
				int winner = r.nextInt(lockHolder.effectivePriority);
				int counter = 0;				
				for(ThreadState ts : this.waitQueue) {
					counter += ts.effectivePriority;
					if (counter > winner)
						return ts.thread; 	
				}				
			}
			return null;
		}
    
    	public void acquire(KThread thread) {
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

	protected class ThreadState extends PriorityScheduler.ThreadState {
		public ThreadState(KThread thread) {
			super(thread);
			this.effectivePriority = 0;
		}

		public int getPriority() {
			return this.priority;
		}

		public int getEffectivePriority() {
			return this.effectivePriority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public void waitForAccess(PriorityQueue waitQueue) {
			this.waitQueue = waitQueue;
		}

		public void acquire(PriorityQueue waitQueue) {
			this.waitQueue = waitQueue;
		}
	}			
}
