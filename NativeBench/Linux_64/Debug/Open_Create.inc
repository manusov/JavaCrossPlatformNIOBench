SYS_READ   = 0   ; Linux API functions (syscall numbers)
SYS_WRITE  = 1
SYS_OPEN   = 2
SYS_CLOSE  = 3
SYS_LSEEK  = 8
SYS_UNLINK = 87

; mov r15,111h ; Marker

lea rbp,[PathName1]  ; RBP = Pointer to path

mov rdi,rbp
mov esi,00000042h
mov edx,00000C00h
mov eax,SYS_OPEN
syscall
test rax,rax
js Skip_Next
xchg rbx,rax          ; RBX = Handle

mov rdi,rbx
lea rsi,[MiscBuffer]
mov edx,100
mov eax,SYS_WRITE
syscall
test rax,rax
js Skip_Next

mov rdi,rbx
lea rsi,[MiscBuffer]
mov edx,100
mov eax,SYS_WRITE
syscall
test rax,rax
js Skip_Next

mov rdi,rbx
xor esi,esi
xor edx,edx
mov eax,SYS_LSEEK
syscall
test rax,rax
js Skip_Next

mov rdi,rbx
lea rsi,[MiscBuffer]
mov edx,100
mov eax,SYS_WRITE
syscall
test rax,rax
js Skip_Next

mov rdi,rbx
mov eax,SYS_CLOSE
syscall
test rax,rax
js Skip_Next

mov rdi,rbp
mov eax,SYS_UNLINK
syscall


Skip_Next:


