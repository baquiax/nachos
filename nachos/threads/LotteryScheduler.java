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

	protected class LotteryQueue extends PriorityQueue {						
		private Random r;
		private LotteryScheduler.ThreadState lockHolder;

		public LotteryQueue(boolean transferPriority) {
			super(transferPriority);
			this.r = new Random();
		}
		
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);									
		}

		public int getEffectivePriority() {
			int effectivePriority = priorityMinimum;
			for(KThread kt : this.waitQueue) {
				ThreadState ts = getThreadState(kt);
				effectivePriority += ts.getEffectivePriority();
			}
			
			return effectivePriority;
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);

			/*if (this.lockHolder != null && this.transferPriority) {								
				this.lockHolder.holdResources.remove(this); //I am no longer a slave!
			}
*/
			this.lockHolder = ts; //I am the Lord!
			ts.acquire(this);
		}
		
		/**
		 * Inspired on:
		 * http://pages.cs.wisc.edu/~remzi/OSTEP/cpu-sched-lottery.pdf		  
		 */
		 
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			if (this.waitQueue.isEmpty()) {                
                Lib.debug('a', "Size: " + this.waitQueue.size());
                return null;
            }

            if (this.lockHolder != null) {
            	this.lockHolder.holdResources.remove(this);
				this.lockHolder = null;	
            }
			

			//Aplicar la loteria y retornar el elegido!
			Lib.debug('a', "Size: " + this.waitQueue.size());
			int winner = r.nextInt(this.getEffectivePriority()) + 1;
			Lib.debug('a', "effectivePriority : " + this.getEffectivePriority());
			int counter = 0;				
			
			KThread nextThread = null;
			for(KThread kt : this.waitQueue) {
				ThreadState ts = getThreadState(kt);
				counter += ts.getEffectivePriority();
				Lib.debug('a', "counter : " + counter + " winner" + winner);
				if (counter >= winner) {
					nextThread = kt;
					break;
				}
			}

			if (nextThread != null) {
				this.waitQueue.remove(nextThread);
				this.acquire(nextThread);	
			}			
			return nextThread;
		}
				
    	public void print() {
			
		}
	}

	protected class ThreadState extends PriorityScheduler.ThreadState {
		protected LinkedList<LotteryQueue> holdResources = new LinkedList<LotteryQueue>();
		protected LotteryQueue waitingFor;

		public ThreadState(KThread thread) {
			super(thread);
			this.effectivePriority = priorityMinimum;
		}

		public int getPriority() {
			return this.priority;
		}

		public int getEffectivePriority() {
			for (LotteryQueue pq : holdResources) {				
				this.effectivePriority += pq.getEffectivePriority();
			}


			PriorityQueue queue = (PriorityQueue) this.thread.getJoinQueue();
			if (queue != null && queue.transferPriority) {
				for (KThread t : queue.waitQueue) {					
					int p = getThreadState(t).getEffectivePriority();					
					effectivePriority += p;						
				}
			}			
						return this.effectivePriority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public void waitForAccess(LotteryQueue waitQueue) {
		    Lib.assertTrue(Machine.interrupt().disabled());			
			waitQueue.waitQueue.add(this.thread);
			this.waitingFor = waitQueue;

			 if (holdResources.indexOf(waitQueue) != -1) {
            	holdResources.remove(waitQueue);
            	waitQueue.lockHolder = null;
        	}			
		}

		public void acquire(LotteryQueue waitQueue) {
		    Lib.assertTrue(Machine.interrupt().disabled());
			this.holdResources.add(waitQueue);
			if (waitQueue == this.waitingFor) {
				this.waitingFor = null;
			} 
		}
	}			
}
