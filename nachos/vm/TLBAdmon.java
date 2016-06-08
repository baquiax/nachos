package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class TLBAdmon {
	public int getTLBSize() {
        return Machine.processor().getTLBSize();
    }
    
    public TranslationEntry getTLBEntry(int indexTLB) {
        TranslationEntry entry = Machine.processor().readTLBEntry(indexTLB);
        return entry;
    }

    public void addTLBEntry(TranslationEntry entry) {
        int index = -1;
        
        for (int i=0; i<getTLBSize();i++) {
            if (getTLBEntry(index).valid) {
                index = i;
                break;
            }
        }
        
        if (index == -1)
            index = Lib.random(getTLBSize());

        Machine.processor().writeTLBEntry(index, entry);
    }
}
