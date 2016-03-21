package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	private Lock lock;
	private int word;

	private int speakers;
	private int listeners;	
	Condition speakerCondition;
	Condition listenerCondition;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		this.lock = new Lock();
		speakerCondition = new Condition(this.lock);
		listenerCondition = new Condition(this.lock);
		this.speakers = 0;
		this.listeners = 0;		
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param	word	the integer to transfer.
	 */
	public void speak(int word) {
		this.lock.acquire();
		while(this.listeners == 0) {
			this.listenerCondition.sleep();
		}
		this.listeners--;
		this.speakers++;
		this.word = word;
		this.speakerCondition.wake();
		this.lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	public int listen() {
		this.lock.acquire();
		this.listeners++;
		this.listenerCondition.wake();

		while(this.speakers == 0) {
			this.speakerCondition.sleep();
		}
		this.speakers--;
		Lib.debug(KThread.dbgCommunication, "Message receied: " + this.word);	
		this.lock.release();
		return this.word;
	}
}
