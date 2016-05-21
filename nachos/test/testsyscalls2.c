#include "syscall.h"
#include "stdio.h"

int main() {
  unlink("newFile.txt");
  halt();
}
