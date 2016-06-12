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

        public String toString() {
            return this.PID + "-" + this.vpn;
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
       mutex = new Lock();
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
        IPTKey key = new VMKernel.IPTKey(pid, vpn);        
        TranslationEntry te = globalIPT.get(key.toString());
        Lib.debug('a', "Get entry: " + pid + ", " + vpn + " TE:" + te);        
        return te;
    }

    public static void unloadPage(int pid, int vpn) {
        mutex.acquire();        
        IPTKey key = new VMKernel.IPTKey(pid, vpn);        
        TranslationEntry te = globalIPT.get(key.toString());
        if (te != null) {
            UserKernel.releasePage(te.ppn);
            globalIPT.remove(key);    
        }        
        
        mutex.release();
    }

    public static TranslationEntry loadPage(int pid, int vpn) {
        mutex.acquire();      
        int ppn = UserKernel.allocPage();     
        IPTKey key = new VMKernel.IPTKey(pid, vpn);

        TranslationEntry newTe = new TranslationEntry(vpn, ppn, true, false, false, false);        
        newTe.used = true;     
        newTe.valid = true;
        globalIPT.put(key.toString(), newTe);
        
        TranslationEntry te = globalIPT.get(key.toString());
        Lib.debug('a', "Loaded page: " + pid + ", " + vpn + " TE:" + te);

        mutex.release();
        return te;
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    //My vars
    private static Lock mutex;
    private static Hashtable<String, TranslationEntry> globalIPT = new Hashtable<String
    , TranslationEntry> ();
    
}
