clear
javac NB64/TST64.java
fasm/fasm NB64/LINUX64JNI.asm
gcc -shared -o NB64/libLINUX64JNI.so NB64/LINUX64JNI.o -z noexecstack
cd NB64
java -Djava.library.path=. TST64
cd ..


