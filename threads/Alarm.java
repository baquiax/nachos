package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	public PriorityQueue alarmWaitList = new PriorityQueue<AlarmWait>();

	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() { timerInterrupt(); }
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();
		long currentTime = Machine.timer().getTime();

		AlarmWait waitingThread = (AlarmWait)alarmWaitList.peek();
		while (waitingThread != null && currentTime >= waitingThread.getWakeTime()) {
			alarmWaitList.poll();
			waitingThread.thread.ready();
			waitingThread = (AlarmWait)alarmWaitList.peek();
		}
		Machine.interrupt().restore(intStatus);
		KThread.currentThread().yield();    
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param   x   the minimum number of clock ticks to wait.
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
		if (x > 0) {			
			boolean intStatus = Machine.interrupt().disable();
			long time = Machine.timer().getTime() + x;
			AlarmWait aw = new AlarmWait(time , KThread.currentThread());
			alarmWaitList.offer(aw);
			KThread.sleep();
			Machine.interrupt().restore(intStatus);
		} else return;
	}
}

class AlarmWait implements Comparable<AlarmWait>{
	long wakeTime;
	KThread thread;

	public AlarmWait(long wakeTime, KThread thread) {
		this.wakeTime=wakeTime;
		this.thread=thread;
	}

	public long getWakeTime() {
		return wakeTime;
	}

	public KThread getThread() {
		return thread;
	}

	@Override
	public int compareTo(AlarmWait toCompare) {
		if (this.wakeTime < toCompare.getWakeTime()) {
			return -1;
		} else if (this.wakeTime > toCompare.getWakeTime()) {
			return 1;
		}
		return 0;
	}
}
