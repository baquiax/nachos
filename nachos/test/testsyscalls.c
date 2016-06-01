#include "syscall.h"
#include "stdio.h"

int main() {
    int newFile = creat("newFile.txt");
    char hello[29] = "Hola, estoy escribiendo esto!";
    write(newFile, hello, 29);
    
    //New process    
    char *argv[1];
    argv[0] = "a";
    int pid = exec("testsyscalls2.coff", 1, argv);
    if (pid > 1) {
    	int status;
    	join(pid, &status);
	}

    //char bye[6] = "Aloha!";
    //write(newFile, bye, 6);
    close(newFile);
    halt();	
}
