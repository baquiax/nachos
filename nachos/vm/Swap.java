package nachos.vm;

import java.util.*;
import nachos.machine.*;
import nachos.userprog.*;
import nachos.threads.*;

public class Swap {
	public static OpenFile swapFile;
	private static int pagesize = Machine.processor().pageSize;
	private static String swapFileName;

	public Swap() {
	}

	public static void initialize(String fileName) {
		swapFile = ThreadKernel.fileSystem.open(fileName, true);
		swapFileName = fileName;
	}

	public static void closeSwapFile() {
		swapFile.close();
		ThreadKernel.fileSystem.remove(swapFileName);
	}
}
