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
        super.restoreState();
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
        //return super.loadSections();

        //Carga por demanda
        int numberOfPages = Machine.processor().getNumPhysPages();
        for (int i = 0; i < numberOfPages; i++) {
            pageTable[i].valid = false;
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        super.unloadSections();
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
                //Search for value...                
                int vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
                int vpn = vaddr/pageSize;
                if (vpn < 0 || vpn > pageTable.length) {                
                    Lib.debug(dbgProcess, "Invalid page.");
                    handleExit(-1); //Exit of the process
                }

                TranslationEntry page = VMKernel.getEntry(this.getPID(), vpn); //PID... Remember the inheritance
                if (page == null) {
                    //Is necessary load from GIPT
                    Lib.debug(dbgProcess, "Page fault");
                    page = VMKernel.loadPage(this.getPID(), vpn);
                }
                
                //Load coff
                for(int s=0;(s<coff.getNumSections());s++){
                    CoffSection section = coff.getSection(s);
                    for(int i = 0; i<section.getLength(); i++){
                        if(section.getFirstVPN() + i == vpn) {
                            Lib.debug(dbgProcess, "\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");
                            pageTable[vpn].readOnly = section.isReadOnly();
                            section.loadPage(i, pageTable[vpn].ppn);
				        }
			        }
		        }

                int randomNumber = (int) Math.floor(Math.random()*(Machine.processor().getTLBSize() + 1));  
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
