clear
javac NB32/TST32.java
fasm/fasm NB32/LINUX32JNI.asm
gcc -shared -o NB32/libLINUX32JNI.so NB32/LINUX32JNI.o -z noexecstack
cd NB32
java -Djava.library.path=. TST32
cd ..


