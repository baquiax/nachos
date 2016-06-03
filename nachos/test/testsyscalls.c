#include "syscall.h"

int main() {
    int newFile = creat("newFile.txt");
    char hello[30] = "Hola, estoy escribiendo esto!\n";
    write(newFile, hello, 30);
    
    //New process    
    char *argv[1];
    argv[0] = "a";
    int pid = exec("testsyscalls2.coff", 0, argv);
    if (pid > 0) {
    	int status;
	char bye[6] = "JOIN!\n";
    printf("ENTRANDO DE JOIN\n");
	write(newFile, bye, 6);
        join(pid, &status);
        printf("SALIENDO DE JOIN\n");
     }

    char bye[7] = "Aloha!\n";
    write(newFile, bye, 7);
    close(newFile);
}
