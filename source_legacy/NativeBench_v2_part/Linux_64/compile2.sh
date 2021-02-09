clear
sudo fasm LINUX64JNI.asm
sudo gcc -shared -o libLINUX64JNI.so LINUX64JNI.o -z noexecstack
