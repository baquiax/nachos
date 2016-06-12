#include "syscall.h"
#include "stdio.h"

int main(int argc, char *argv[]) {
	char *fileName = "secondNewFile.txt";
	int secondFile = creat(fileName);
	char text[15] = "Desde el join!\n";
	write(secondFile, text, 15);
	close(secondFile);    
	exit(0);
}
