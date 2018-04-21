clear
sudo fasm LINUX32JNI.asm
sudo gcc -shared -o libLINUX32JNI.so LINUX32JNI.o -z noexecstack
