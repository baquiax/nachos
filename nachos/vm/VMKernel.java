package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Hashtable;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    public static class IPTKey {
        private int PID;
        private int vpn;

        public IPTKey(int pid, int vpn) {
            this.PID = pid;
            this.vpn = vpn;
        }

        public int getVPN() {
            return this.vpn;
        }

        public int getPID() {
            return this.PID;
        }
    }
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	   super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	   super.initialize(args);
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	   super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	   super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	   super.terminate();
    }

    public static TranslationEntry getEntry(int pid, int vpn) {
        mutex.acquire();
        IPTKey key = new VMKernel.IPTKey(pid, vpn);
        return globalIPT.get(key);
        mutex.release();
    }

    public static void addEntry(int pid, int vpn, TranslationEntry te) {
        mutex.acquire();
        IPTKey key = new VMKernel.IPTKey(pid, vpn);
        return globalIPT.put(key, te);
        mutex.release();  
    }

    public static TranslationEntry loadPage(int pid, int vpn) {
        mutex.acquire();
        int ppn = UserKernel.allocPage();
        TranslationEntry newTe = new TranslationEntry(vpn, ppn, true, false, false, false);        
        VMKernel.addEntry(pid, vpn, newTe);
        return getEntry(pid, vpn);
        mutex.release();
    }
    

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    //My vars
    private Lock mutex;
    private static Hashtable<IPTKey, TranslationEntry> globalIPT = new Hashtable<IPTKey
    , TranslationEntry> ();
    
}
