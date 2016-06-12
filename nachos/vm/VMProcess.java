package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        //super.restoreState();
        //Invalidate GIPT
        int size = Machine.processor().getTLBSize();        
        for (int i = 0; i < size ; i++) {
            TranslationEntry te = Machine.processor().readTLBEntry(i);
            te.valid = false;
            Machine.processor().writeTLBEntry(i, te);
        }        
    }
    
    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     * 
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        //Now there are infinite pages using swap
        /*if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }*/

        for(int i = 0; i < numPages; i++) {
            //Now there are infinite pages using swap
            /*if (UserKernel.getAvailablePages() == 0) {
                coff.close();
                Lib.debug(dbgProcess, "\tinsufficient pages");
                return false;
            }*/
            TranslationEntry te = VMKernel.loadPage(this.getPID(), i);
            pageTable[i] = te;
        }
        
        //Load pages and load to GIPT        
        Lib.debug(dbgProcess, "Load sections.");
        for (int i = 0; i < coff.getNumSections(); i++) {
            CoffSection section = coff.getSection(i);
            Lib.debug(dbgProcess, "Loading section:" + section.getName());
            for (int s = 0; s < section.getLength(); s++) {
                int vpn = section.getFirstVPN() + s;
                TranslationEntry te = VMKernel.getEntry(this.getPID(), vpn);
                Lib.debug(dbgProcess, "Loading page with PID:" + this.getPID() + " and VPN: " + vpn + " = " + te);
                te.readOnly = section.isReadOnly();
                section.loadPage(s, te.ppn);                
            }        
        }
        
        //Carga por demanda
        /*int numberOfPages = Machine.processor().getNumPhysPages();
        int vaddr, vpn;
        for (int i = 0; i < numberOfPages; i++) {
            pageTable[i].valid = false;
            vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
            vpn = vaddr/pageSize;
            TranslationEntry entryIPT = Machine.processor().readTLBEntry(i);
            VMKernel.addEntry(this.getPID(), vpn,entryIPT);
            Lib.debug(dbgProcess, String.valueOf(i));
        }*/
        
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        //super.unloadSections();
        //Remove from TLB
        for (int i = 0; i < coff.getNumSections(); i++) {
            TranslationEntry te = pageTable[i];
            VMKernel.unloadPage(this.getPID(), te.vpn);
        }
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	   Processor processor = Machine.processor();
       
	   switch (cause) {
           case Processor.exceptionTLBMiss:
                Lib.debug(dbgProcess, "TLB Miss.");
                //Search for value...                
                int vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
                int vpn = vaddr/pageSize;
                if (vpn < 0 || vpn > pageTable.length) {
                    Lib.debug(dbgProcess, "Invalid page.");
                    this.handleExit(-1); //Exit of the process
                }

                TranslationEntry page = VMKernel.getEntry(this.getPID(), vpn); //PID... Remember the inheritance                

                if (page == null || page.valid == false) {
                    //Is necessary load from GIPT
                    Lib.debug(dbgProcess, "PAGE FAULT with PID:" + this.getPID() + " and VPN: " + vpn + " = " + page);
                    page = VMKernel.loadPage(this.getPID(), vpn); 
                }
                
                int randomNumber = (int) Math.floor(Math.random()*(Machine.processor().getTLBSize()));                  
                Lib.debug(dbgProcess, "RandomIndex: " + randomNumber + ", Size: " + Machine.processor().getTLBSize());

                Machine.processor().writeTLBEntry(randomNumber, page);
                break;
	       default:
	           super.handleException(cause);
	           break;
	   }
    }	    

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';    
}
