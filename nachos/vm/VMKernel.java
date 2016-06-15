package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.LinkedList;

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
       swapFile = ThreadedKernel.fileSystem.open("swapFile", true);
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
       VMKernel.swapFile.close();
    }

    public static TranslationEntry getEntry(int pid, int vpn) {        
        IPTKey key = new VMKernel.IPTKey(pid, vpn);        
        TranslationEntry te = globalIPT.get(key.toString());
        Lib.debug(dbgVM, "Get entry: " + pid + ", " + vpn + " TE:" + te);
        if (te == null) {
            //Search in swapfile
	       te = getSwapTable(key.toString());
        }
        return te;
    }

    public static TranslationEntry setEntry(int pid, int vpn, int ppn) {
        mutex.acquire();
        IPTKey key = new VMKernel.IPTKey(pid, vpn);        
        TranslationEntry newTe = new TranslationEntry(vpn, ppn, true, false, true, false);
        newTe.ppn = ppn;
        Lib.debug(dbgVM, "Set entry: " + pid + ", " + vpn + " TE:" + newTe);
        globalIPT.put(key.toString(), newTe);
        mutex.release();
        return newTe;
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


    public static TranslationEntry loadPageFromSwap(int pid, int vpn) {
        mutex.acquire();              
        IPTKey key = new VMKernel.IPTKey(pid, vpn);
        TranslationEntry newTe = allocSwapTable(key);
        TranslationEntry te = getSwapTable(key.toString());
        Lib.debug(dbgVM, "Loaded page: " + pid + ", " + vpn + " TE:" + te);
        mutex.release();
        return te;
    }

    public static TranslationEntry loadPage(int pid, int vpn) {
        mutex.acquire();              
        IPTKey key = new VMKernel.IPTKey(pid, vpn);

        TranslationEntry newTe = null;
        if (UserKernel.getAvailablePages() > 0) {
            int ppn = UserKernel.allocPage();
            newTe = new TranslationEntry(vpn, ppn, true, false, true, false);
            globalIPT.put(key.toString(), newTe);
        } else {
            return null;
        }
        
        TranslationEntry te = globalIPT.get(key.toString());
        Lib.debug(dbgVM, "Loaded page: " + pid + ", " + vpn + " TE:" + te);

        mutex.release();
        return te;
    }

    public static TranslationEntry clockReplacement(String key, TranslationEntry value) {
        mutex.acquire();

        if (globalIPT.isEmpty()) {
            mutex.release();
            return null;
        }

        Enumeration e = globalIPT.keys();
        String keyValue;
        TranslationEntry teValue;
        int countClock=0;
        int dirtyClock=0;

        while(e.hasMoreElements()) {
            keyValue = (String) e.nextElement();
            teValue = (TranslationEntry) globalIPT.get(keyValue);

            if (teValue.used == true) {
                if (teValue.dirty == false) {
                    teValue.used = false;
                    globalIPT.put(keyValue,teValue);
                    countClock++;	   	
                } else {
                    dirtyClock++;
                }
            } else {
                value.ppn = teValue.ppn;
                globalIPT.remove(keyValue);
                globalIPT.put(key, value);
                mutex.release();
                return teValue;
            }
        }
        
        if (countClock >0 || dirtyClock>0) {
            Enumeration en = globalIPT.keys();
            if (en.hasMoreElements()) {
                String kv = (String) en.nextElement();
                teValue = (TranslationEntry) globalIPT.get(kv);
                value.ppn = teValue.ppn;                
                globalIPT.remove(kv);
                globalIPT.put(key,value);
                mutex.release();
                return teValue;
            }
        }
        
        mutex.release();
        return null;
    }
    
    public static TranslationEntry allocSwapTable(IPTKey key) {
        int ppn = 0;
        
        mutex.acquire();
        TranslationEntry newTe = new TranslationEntry(key.getPID(), -1, true, false, true, false);;
        if (swapPPN.size() == 0) {
            ppn++;
            swapPPN.addLast(ppn);
            mutex.release();
            newTe.ppn = ppn;
            return newTe;
        }
        
        if (swapPPN.indexOf(-1) != -1) {
            ppn = swapPPN.indexOf(-1);
            swapPPN.add(ppn, ppn);
            mutex.release();
            newTe.ppn = ppn;
            return newTe;
        }
        
        ppn = swapPPN.peekLast();
        swapPPN.addLast(ppn);
        mutex.release();

        TranslationEntry te = new TranslationEntry(key.getPID(), ppn, true, false, true, false);
        swapTable.put(key.toString(), te);

        return te;
    }
    
    private static void releaseSwapTable(int element) {
        mutex.acquire();
        swapPPN.add(element,-1);
        mutex.release();
    }
    
    private static TranslationEntry getSwapTable(String key) {
        TranslationEntry te = (TranslationEntry) swapTable.get(key);
        return te;
    }
    
    public static void writeSwapFile(TranslationEntry te, byte[] page) {        
        //UserProcess.readVirtualMemory(bufferToWritePointer, bytesToWrite, 0, numberOfBytes);
        swapFile.write(page,0,page.length);
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;
    private static final char dbgVM = 'v';    

    //My vars
    private static Lock mutex;
    private static OpenFile swapFile;
    private static Hashtable<String, TranslationEntry> globalIPT = new Hashtable<String, TranslationEntry> ();
    private static Hashtable<String, TranslationEntry>  swapTable = new Hashtable<String, TranslationEntry>();
    private static LinkedList<Integer> swapPPN = new LinkedList<Integer>();
}
