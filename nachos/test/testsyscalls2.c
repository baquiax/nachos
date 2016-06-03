#include "syscall.h"
#include "stdio.h"

int main(int argc, char *argv[]) {
	char *fileName = "secondNewFile.txt";
	int secondFile = creat(fileName);
	char text[16] = "Desde el join!\n";
	write(secondFile, text, 16);
	close(secondFile);    
	exit(0);
}
