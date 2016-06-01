package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
		this.fileDescriptorTable = new HashMap<Integer, OpenFile>();            
        this.prepareFileDescriptors(16);
		this.childProcesses = new HashMap<Integer, UserProcess>();            
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
  
        lock.acquire();          
        this.PID = UserProcess.CURRENT_PID++;
        UserProcess.remainingProcesses++;
        lock.release();
    }		
	
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        this.userThread = new UThread(this);
        this.userThread.setName(name).fork();
        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {}

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
        int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;
        
        //int amount = Math.min(length, memory.length - vaddr);
        ///System.arraycopy(memory, vaddr, data, offset, amount);
        
        //Here there is the magic code!        
        int basePage = vaddr/this.pageSize; //e.g (1100/1000) = page 1
        int endPage =  (vaddr + length) / pageSize; //e.g (1100 + 1600)/1000 = page 2            
        int physicalOffset = vaddr % pageSize; // e.g 1100 % 1000 = 100
        
        int physicalPage = 0;
        int bytesToCopy = 0;
        int physicalPageAddress = 0;
        int bytesCopied = 0;
        
        for (int i = basePage; i <= basePage; i++) {            
            //Read all a page or a part of this.
            bytesToCopy = Math.min(length, pageSize);
            physicalPage = pageTable[i].ppn; //Get the real address for a virtual address.
            pageTable[i].used = true; //According TranslationEntry class, whe need put to used.            
            physicalPageAddress = (physicalPage * this.pageSize) + ((i == basePage) ? physicalOffset : 0); // + offset only for the first Page
            
            //Ref: public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length)            
            System.arraycopy(memory, physicalPageAddress, data, offset, bytesToCopy); //Copy chunk by chunk of memory.
            
            offset += bytesToCopy;
            length -= bytesToCopy;                   
            bytesCopied += bytesToCopy;
        }
        return bytesCopied;  
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
        int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        //int amount = Math.min(length, memory.length - vaddr);
        //System.arraycopy(data, offset, memory, vaddr, amount);
        
        //Here there is the magic code!        
        int basePage = (int) vaddr/this.pageSize; //e.g (1100/1000) = page 1
        int endPage =  (vaddr + length) / pageSize; //e.g (1100 + 1600)/1000 = page 2            
        int physicalOffset = vaddr % pageSize; // e.g 1100 % 1000 = 100
        
        int physicalPage;
        int bytesToCopy;
        int physicalPageAddress;
        int bytesCopied = 0;
        
        for (int i = basePage; i <= basePage; i++) {            
            //Read all a page or a part of this.
            bytesToCopy = Math.min(length, pageSize);
            physicalPage = pageTable[i].ppn; //Get the real address for a virtual address.
            pageTable[i].used = true; //According TranslationEntry class, whe need put to used.            
            physicalPageAddress = (physicalPage * this.pageSize) + ((i == basePage) ? physicalOffset : 0); // + offset only for the first Page
            
            //Ref: public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length)            
            System.arraycopy(data, offset, memory, physicalPageAddress, bytesToCopy); //It's same readVirtualMemory
            
            offset += bytesToCopy;
            length -= bytesToCopy;
            bytesCopied += bytesToCopy;
        }
        return bytesCopied;        
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] {
                0
            }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }
        
        //Load pages
        for(int i = 0; i < numPages; i++) {
            if (UserKernel.getAvailablePages() == 0) {
                coff.close();
                Lib.debug(dbgProcess, "\tinsufficient pages");
                return false;                
            }                        
            pageTable[i].ppn = UserKernel.allocPage();
            pageTable[i].used = true;
        }

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName() +
                " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;

                // for now, just assume virtual addresses=physical addresses
                //section.loadPage(i, vpn);
                
                //Here there is the magical code!!!
                pageTable[vpn].readOnly = section.isReadOnly();//According the instructions
                section.loadPage(i, vpn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {}

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
		//TODO: Solo el root puede hacer halt, de lo contrario ignorar.
        if (this.PID != 0) return -1;
        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    private void prepareFileDescriptors(int maxDescriptors) {
        if (this.availableFileDescriptors != null) return;

        this.availableFileDescriptors = new LinkedList<Integer>();        
        for (int i = 0; i < maxDescriptors; i++) {
            this.availableFileDescriptors.add(i);
        }

        //When any process is started, its file descriptors 0 and 1 must refer to standard input and standard output.
        this.addFileDescriptor(UserKernel.console.openForReading());
        this.addFileDescriptor(UserKernel.console.openForWriting());        
    }

    //This method returns -1 when reached the limit of opened files, otherwise returns <int>.
    private int addFileDescriptor(OpenFile of) {
        if (this.availableFileDescriptors.peek() == null) return -1;
        int fileDescriptor = this.availableFileDescriptors.poll();
        this.fileDescriptorTable.put(fileDescriptor, of);
        return fileDescriptor;
    }
    
    private boolean removeFileDescriptor(int fileDescriptor) {        
        if (fileDescriptor < 0 && fileDescriptor >= this.fileDescriptorTable.size()) return false;
        this.fileDescriptorTable.remove(fileDescriptor);
        this.availableFileDescriptors.addLast(fileDescriptor);
        return true;
    }

	/**
	 * Handle the create() system call.
	 * Returns -1 when happened an error or returns a fileDescriptor otherwise.
	 */
	private int handleCreate(int fileNamePointer) {
		Lib.debug(dbgProcess, "syscall create with addr: " + fileNamePointer); //It's the virtual string address
		String fileName = readVirtualMemoryString(fileNamePointer, 255); //Read 256 bytes (maxLength + 1) from <fileNamePointer>
		if (fileName == null) { //Invalid fileName
			Lib.debug(dbgProcess, "create error, invalid name.");
			return -1;
		}
		OpenFile newFile = ThreadedKernel.fileSystem.open(fileName, true);
		
		if (newFile == null) { //Creation error
			Lib.debug(dbgProcess, "create error, file non created.");
			return -1;
		}
		
		/**
		 * Read it about File Descriptors: http://stackoverflow.com/a/5256705/1736167
		 */
		 return addFileDescriptor(newFile);
	}

    /**
     * Handle the open() system call.
     * Returns -1 when happened an error or returns a fileDescriptor otherwise.
     */
    private int handleOpen(int fileNamePointer) {
        Lib.debug(dbgProcess, "syscall open with fileNamePointer: " + fileNamePointer);
        String fileName = readVirtualMemoryString(fileNamePointer, 255);
        if (fileName == null) {
            Lib.debug(dbgProcess, "Invalid file name.");
            return -1;
        }

        OpenFile openedFile = ThreadedKernel.fileSystem.open(fileName, false);
        
        if (openedFile == null) {
            Lib.debug(dbgProcess, "File doesn't exists.");
            return -1;
        }

        return this.addFileDescriptor(openedFile);
    }

    private int handleRead(int fileDescriptor, int destBufferPointer, int numberOfBytes) {
        Lib.debug(dbgProcess, "syscall read with descriptor: " + fileDescriptor);
        OpenFile of = this.fileDescriptorTable.get(fileDescriptor);
        if (of == null) {
            Lib.debug(dbgProcess, "File doesn't exists.");
            return -1;
        }
        numberOfBytes = Math.min(Math.max(0, numberOfBytes), of.length());
        byte[] readedBytes = new byte[numberOfBytes];
        of.read(readedBytes, 0, numberOfBytes);
        return this.writeVirtualMemory(destBufferPointer, readedBytes);   
    }

    private int handleWrite(int fileDescriptor, int bufferToWritePointer, int numberOfBytes) {
        Lib.debug(dbgProcess, "syscall write with descriptor: " + fileDescriptor);
        OpenFile of = this.fileDescriptorTable.get(fileDescriptor);
        if (of == null) {
            Lib.debug(dbgProcess, "File isn't opened.");
            return -1;
        }
        //numberOfBytes = Math.min(Math.max(0, numberOfBytes), of.length());
        Lib.debug(dbgProcess, "" + numberOfBytes);
        byte[] bytesToWrite = new byte[numberOfBytes];
        this.readVirtualMemory(bufferToWritePointer, bytesToWrite, 0, numberOfBytes);
        return of.write(bytesToWrite,0,numberOfBytes);
    }

    private int handleClose(int fileDescriptor) {
        Lib.debug(dbgProcess, "syscall close with descriptor: " + fileDescriptor);
        OpenFile of = this.fileDescriptorTable.get(fileDescriptor);
        if (of == null) {
            Lib.debug(dbgProcess, "The filedescriptor provided ins't exists.");
            return -1;
        }        
        of.close();
        return (this.removeFileDescriptor(fileDescriptor)) ? 0 : -1;
    }


    private int handleUnlink(int fileNamePointer) {
        Lib.debug(dbgProcess, "syscall unlink with fileNamePointer: " + fileNamePointer);
        String fileName = readVirtualMemoryString(fileNamePointer, 255);
        if (fileName == null) {
            Lib.debug(dbgProcess, "Invalid file name.");
            return -1;
        }
        return (ThreadedKernel.fileSystem.remove(fileName)) ? 0 : -1;
    }
    
    private int handleExec(int fileNamePointer, int argc, int argv) {        
        Lib.debug(dbgProcess, "syscall exec with fileNamePointer: " + fileNamePointer);
        String fileName = readVirtualMemoryString(fileNamePointer, 255);
        UserProcess newChild = UserProcess.newUserProcess();
        if (newChild == null) {
            Lib.debug(dbgProcess, "Child doesn't created.");
            return -1;
        }
        if (fileName == null) {
            Lib.debug(dbgProcess, "Invalid file name.");
            return -1;
        }
        
        if (!fileName.trim().endsWith(".coff")) {
            Lib.debug(dbgProcess, "Invalid COFF file.");
            return -1;
        }
        
        if (argc < 0) {
            Lib.debug(dbgProcess, "Number of args should be positive.");
            return 1;
        }
        
        String args[] = new String[argc];
        Lib.debug(dbgProcess, "syscall exec received paramNumber: " + argc);
        for (int i = 0; i < argc; i++) {                        
            String parameter = readVirtualMemoryString(argv, 255);
            Lib.debug(dbgProcess, "syscall exec received param: " + parameter);
            args[i] = parameter;            
            argv += 256;
        }

        this.childProcesses.put(newChild.getPID(), newChild);
        newChild.setParent(this);                
        
        Lib.debug(dbgProcess, "syscall exec with filename: " + fileName);
        if (!newChild.execute(fileName, args)) {
            Lib.debug(dbgProcess, "An error in exec.");
            return 1;
        }
        
        Lib.debug(dbgProcess, "syscall exec end with PID: " + newChild.getPID());
        return newChild.getPID();
    }        
    
    private void handleExit(int status) {
        Lib.debug(dbgProcess, "syscall exit with status: " + status);
        for (OpenFile of : fileDescriptorTable.values()) { //Close all opened files
            of.close();
        }
        fileDescriptorTable.clear();
        for (UserProcess up : this.childProcesses.values()) {
            up.setParent(null);
        }
        
        if (this.parent != null) {
            this.parent.setLastChildStatus(status);
        }
                
        this.unloadSections();//Free memory
        
        lock.acquire();
        UserProcess.remainingProcesses--;
        lock.release();
        
        if (UserProcess.remainingProcesses == 0) {
            Kernel.kernel.terminate();     
        }        
        KThread.finish();
    }    

    private int handleJoin(int childPID, int statusPointer) {
        Lib.debug(dbgProcess, "syscall join with child PID: " + childPID);
        UserProcess child = this.childProcesses.get(childPID);
        if (child == null) {
            Lib.debug(dbgProcess, "The child with PID " + childPID + " isn't recognized.");
            return -1;
        }
        child.getUserThread().join(); //Join to child
        /*Check it: http://stackoverflow.com/questions/2183240/java-integer-to-byte-array*/
        byte[] status = ByteBuffer.allocate(4).putInt(this.lastChildStatus).array();//Save the child status
        writeVirtualMemory(statusPointer, status);
        this.childProcesses.remove(childPID); //Not allowed join again        
        if (this.lastChildStatus == 0) {
            Lib.debug(dbgProcess, "syscall join with errors");
            return 0;
        }
        return 1;
    }                
    
    public int getPID() {
        return this.PID;
    }
    
    public void setParent(UserProcess up) {
        this.parent = up;
    }
    
    public UserProcess getParent() {
        return this.parent;    
    }
    
    public UThread getUserThread() {
        return this.userThread;
    }
    
    public void setLastChildStatus(int status) {
        this.lastChildStatus = status;
    }
    private static final int
    syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreate = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        Lib.debug(dbgProcess, "HANDLING SYSCALL " + syscall);
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallCreate:
                return handleCreate(a0);
            case syscallOpen:
                return handleOpen(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);
            case syscallJoin:
                return handleJoin(a0,a1);
            case syscallExec:
                return handleExec(a0,a1,a2);                
            case syscallExit:
                handleExit(a0);
                break;
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
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
        Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                    processor.readRegister(Processor.regA0),
                    processor.readRegister(Processor.regA1),
                    processor.readRegister(Processor.regA2),
                    processor.readRegister(Processor.regA3)
                );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                    Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }
	
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
	private static int CURRENT_PID = 0;
    private static Lock lock = new Lock();
    private static int remainingProcesses = 0;

	private HashMap<Integer, OpenFile> fileDescriptorTable;
    private HashMap<Integer, UserProcess> childProcesses;
    private LinkedList<Integer> availableFileDescriptors;
    private UThread userThread;     
    private UserProcess parent; //Required by JOIN
    private int lastChildStatus;    
    private int PID;        	
}