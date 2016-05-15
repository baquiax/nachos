#include "syscall.h"
#include "stdio.h"

int main() {
    int newFile = creat("newFile.txt");
    char hello[29] = "Hola, estoy escribiendo esto!";
    write(newFile, hello, 29);
    close(newFile);
    halt();
}
