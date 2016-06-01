#include "syscall.h"
#include "stdio.h"

int main(int argc, char *argv[]) {
	char *fileName = "newFile.txt";
	int secondFile = open(fileName);
    close(secondFile);    
    exit(1);
}
