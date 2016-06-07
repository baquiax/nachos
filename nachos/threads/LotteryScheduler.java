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

	protected class LotteryQueue extends ThreadQueue {						
		private Random r;
		
		public LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.r = new Random();
		}
		
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);									
		}


		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);

			if (this.holder != null && this.transferPriority) {								
				this.lockHolder.holdResources.remove(this); //I am no longer a slave!
			}

			this.lockHolder = ts; //I am the Lord!
			ts.acquire(this);
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
				
    	public void print() {
			
		}
	}

	protected class ThreadState extends PriorityScheduler.ThreadState {
		private LinkedList<PriorityQueue> holdResources = new LinkedList<PriorityQueue>();
		private PriorityQueue waitingFor;

		public ThreadState(KThread thread) {
			super(thread);
			this.effectivePriority = priorityMinimum;
		}

		public int getPriority() {
			return this.priority;
		}

		public int getEffectivePriority() {
			for (PriorityQueue pq : holdResources) {
				this.effectivePriority += pq.getEffectivePriority();
			}
			return this.effectivePriority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public void waitForAccess(PriorityQueue waitQueue) {
		    Lib.assertTrue(Machine.interrupt().disabled());			
			waitQueue.waitQueue.add(this.thread);
			this.waitingFor = waitQueue;			
		}

		public void acquire(PriorityQueue waitQueue) {
		    Lib.assertTrue(Machine.interrupt().disabled());
			this.holdResources.add(waitQueue);
			if (waitQueue == this.waitingFor) {
				this.waitingFor = null;
			} 
		}
	}			
}
