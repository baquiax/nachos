NachOS
=========
![Nachoooos](https://raw.githubusercontent.com/baquiax/nachos/master/nachos.jpg)

# Informacion general

 - Asdrubal Batz
 - Douglas Figueroa
 - Alexander Baquiax

[Ver Repositorio](https://github.com/baquiax/nachos.git)

# Configuración
Para la fase 2 es necesario configurar algunas cosas, la siguiente configuración supone que el proyecto está siendo ejecutado sobre un sistema GNU/Linux.


## Variables de entorno ##
Algunos archivos `Makefile` usan la variable de entorno `ARCHDIR` para buscar el compilador de `MIPS`. En este proyecto encontrará la carpeta que contiene los binarios para x86 (`/mips-x86.linux-xgcc`). 

Para crear las variables de entorno puede modificar el archivo`~/.bashrc` y agregar:

 1. `export ARCHDIR='<mips-x86.linux-xgcc-dir>'`
 2. `export PATH=$PATH:<mips-x86.linux-xgcc-dir>`
 
 Donde `<mips-x86.linux-xgcc-dir>`, representa el path donde alojo la carpeta de compilador.

Por último genera un link simbólico de `make`:
`sudo ln -s /usr/bin/make /usr/bin/gmake`

Este último paso es opcional, pues en algunos `Makefile`s hacen refrencia a  `gmake` en lugar de `make`.
# Modo de uso
Acceda a la carpeta **proj1**
`make clean && make && nachos -d jw`

Acceda a la carpeta **proj2**
`make clean && make && nachos -d ma`



----------
Galileo University
2016

