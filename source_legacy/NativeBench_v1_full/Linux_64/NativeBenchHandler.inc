;--- File operations benchmarks: Write, Read, Copy, Delete --------------------; 
; Input:  RSI = Pointer to IPB (Input Parameters Block)                        ;
;         RDI = Pointer to OPB (Output Parameters Block)                       ;
;         Data at IPB                                                          ;
;                                                                              ;
; Output: RAX = Reserved for status                                            ;
;         Data at OPB                                                          ;
;                                                                              ;
; IPB:    DWORD IPB[00] = Reserved for subfunction code                        ;
;         DWORD IPB[04] = Reserved                                             ;
;         QWORD IPB[08] = Block size                                           ;
;         QWORD IPB[16] = Blocks count                                         ;
;         QWORD IPB[24] = Source file attributes                               ;
;         QWORD IPB[32] = Destination file attributes                          ;
;         QWORD IPB[40] = Measurement iterations count                         ;
;         976 bytes IPB[48-1023] = Source file path string                     ;
;         1024 bytes IPB[1024-2047] = Destination file path string             ;
;         BLOCK_SIZE [800h...X] = Source file I/O buffer                       ;
;                                                                              ;
; OPB:    QWORD OPB[00] = File write result, 0=Error, otherwise dt units 1 ns  ;
;         QWORD OPB[08] = File read result, 0=Error, otherwise dt units 1 ns   ; 
;         QWORD OPB[16] = File copy result, 0=Error, otherwise dt units 1 ns   ; 
;         QWORD OPB[24] = File delete result, 0=Error, otherwise > 0           ;
;         QWORD OPB[32] = Used for API function return size or position        ;
;         QWORDs OPB[40-55] = Used for API function return time, start point   ;
;         QWORDs OPB[56-71] = Used for API function return time, stop point    ;
;         1976 bytes OPB [72-2047] = Reserved                                  ;
;         BLOCK_SIZE [800h...X] = Destination file I/O buffer                  ;
;                                                                              ;
;------------------------------------------------------------------------------;

API_WRITE_READ_COPY_DELETE:                    ; Assume RSP aligned before call
push rbx rbx rsi rdi rbp r12 r13 r14 r15       ; RBX push twice for RSP align
;--- Copy parameters to non-volatile registers ---
mov r14,rsi
mov r15,rdi
;--- Clear output variables ---
cld
xor eax,eax
mov ecx,9
rep stosq
;--- Create source file ---
lea rdi,[r14+48]         ; Parm#1 = Pointer to source file path
mov rsi,[r14+24]         ; Parm#2 = File operation mode flags
mov edx,00000C00h        ; Parm#3 = File access read-write mode flags
mov eax,SYS_OPEN
syscall
test rax,rax
jz .Skip_Operation
cmp rax,7FFFFFFFh
ja .Skip_Operation
xchg rbx,rax             ; RBX = Source file handle, XCHG compact
;--- Get time at file WRITE start ---
xor edi,edi              ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[r15+40]         ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME      ; Function code = Get timer
syscall
test rax,rax
jnz .Skip_Operation
;--- Measurement cycle for WRITE ---
mov r13,[r14+40]               ; R13 = Measurement counter
.WriteMeasurement:
;--- Prepare parameters for WRITE operation cycle ---
mov r12,[r14+16]               ; R12 = Block count
;--- Write source file ---
.WriteBlock:
mov rdi,rbx                    ; Parm#1 = File descriptor, handle
lea rsi,[r14+2048]             ; Parm#2 = Pointer to buffer
and rsi,0FFFFFFFFFFFFF000h     ; Required 4KB page alignment
add rsi,00000000000001000h     ; Alignment possible: size reserved
mov rdx,[r14+08]               ; Parm#3 = Buffer size, byte count per call
mov eax,SYS_WRITE              ; Function code = Write to file
syscall
test rax,rax                   ; Check RAX = Status
jz .Skip_Operation             ; Go if error write file, length = 0
js .Skip_Operation             ; Go if error write file, length < 0, status
dec r12
jnz .WriteBlock                ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov rdi,rbx                    ; Parm#1 = File descriptor, handle
xor esi,esi                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
lea r10,[r15+32]               ; Parm#4 = Pointer to variable return position
xor r9d,r9d                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK              ; Function code = seek
syscall
test rax,rax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Measurement cycle for WRITE ---
dec r13
jnz .WriteMeasurement
;--- Get time at file WRITE stop, calculate delta ---
xor edi,edi                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[r15+56]               ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME            ; Function code = Get timer
syscall
test rax,rax
jnz .Skip_Operation
mov rax,1000000000             ; RAX = Nanoseconds per one second
mul qword [r15+40]             ; First qword = Seconds
add rax,[r15+48]               ; Second qword = Nanoseconds
xchg rcx,rax
mov rax,1000000000             ; RAX = Nanoseconds per one second
mul qword [r15+56]             ; First qword = Seconds
add rax,[r15+64]               ; Second qword = Nanoseconds
sub rax,rcx                    ; RAX = After operation time, RCX = Before operation time
mov [r15+00],rax               ; Return delta = RAX-RCX nanoseconds per WRITE operation 
;--- Get time at file READ start ---
xor edi,edi                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[r15+40]               ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME            ; Function code = Get timer
syscall
test rax,rax
jnz .Skip_Operation
;--- Measurement cycle for READ ---
mov r13,[r14+40]               ; R13 = Measurement counter
.ReadMeasurement:
;--- Prepare parameters for READ operation cycle ---
mov r12,[r14+16]               ; R12 = Block count
;--- Read source file ---
.ReadBlock:
mov rdi,rbx                    ; Parm#1 = File descriptor, handle
lea rsi,[r15+2048]             ; Parm#2 = Pointer to buffer
and rsi,0FFFFFFFFFFFFF000h     ; Required 4KB page alignment
add rsi,00000000000001000h     ; Alignment possible: size reserved
mov rdx,[r14+08]               ; Parm#3 = Buffer size, byte count per call
mov eax,SYS_READ               ; Function code = Read from file
syscall
test rax,rax                   ; Check RAX = Status
jz .Skip_Operation             ; Go if error read file, length = 0
js .Skip_Operation             ; Go if error read file, length < 0, status
dec r12
jnz .ReadBlock                 ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov rdi,rbx                    ; Parm#1 = File descriptor, handle
xor esi,esi                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
lea r10,[r15+32]               ; Parm#4 = Pointer to variable return position
xor r9d,r9d                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK              ; Function code = seek
syscall
test rax,rax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Measurement cycle for READ ---
dec r13
jnz .ReadMeasurement
;--- Get time at file READ stop, calculate delta ---
xor edi,edi                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[r15+56]               ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME            ; Function code = Get timer
syscall
test rax,rax
jnz .Skip_Operation
mov rax,1000000000             ; RAX = Nanoseconds per one second
mul qword [r15+40]             ; First qword = Seconds
add rax,[r15+48]               ; Second qword = Nanoseconds
xchg rcx,rax
mov rax,1000000000             ; RAX = Nanoseconds per one second
mul qword [r15+56]             ; First qword = Seconds
add rax,[r15+64]               ; Second qword = Nanoseconds
sub rax,rcx                    ; RAX = After operation time, RCX = Before operation time
mov [r15+08],rax               ; Return delta = RAX-RCX nanoseconds per READ operation 
;--- Create destination file ---
lea rdi,[r14+1024]             ; Parm#1 = Pointer to destination file path
mov rsi,[r14+32]               ; Parm#2 = Mode
mov edx,00000C00h              ; Parm#3 = Flags
mov eax,SYS_OPEN
syscall
test rax,rax
jz .Skip_Operation
cmp rax,7FFFFFFFh
ja .Skip_Operation
xchg rbp,rax                   ; RBP = Destination file handle, XCHG compact
;--- Get time at file COPY start ---
xor edi,edi                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[r15+40]               ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME            ; Function code = Get timer
syscall
test rax,rax
jnz .Skip_Operation
;--- Measurement cycle for COPY ---
mov r13,[r14+40]               ; R13 = Measurement counter
.CopyMeasurement:
;--- Prepare parameters for COPY operation cycle ---
mov r12,[r14+16]               ; R12 = Block count
;--- Copy source file to destination cycle ---
.CopyBlock:
;--- Read phase, source file ---
mov rdi,rbx                    ; Parm#1 = Source file descriptor, handle
lea rsi,[r15+2048]             ; Parm#2 = Pointer to buffer
and rsi,0FFFFFFFFFFFFF000h     ; Required 4KB page alignment
add rsi,00000000000001000h     ; Alignment possible: size reserved
mov rdx,[r14+08]               ; Parm#3 = Buffer size, byte count per call
mov eax,SYS_READ               ; Function code = Read from file
syscall
test rax,rax                   ; Check RAX = Status
jz .Skip_Operation             ; Go if error READ file, length = 0
js .Skip_Operation             ; Go if error READ file, length < 0, status
;--- Write phase, destination cycle ---
mov rdi,rbp                    ; Parm#1 = Destination descriptor, handle
lea rsi,[r15+2048]             ; Parm#2 = Pointer to buffer
and rsi,0FFFFFFFFFFFFF000h     ; Required 4KB page alignment
add rsi,00000000000001000h     ; Alignment possible: size reserved
mov rdx,[r14+08]               ; Parm#3 = Buffer size, byte count per call
mov eax,SYS_WRITE              ; Function code = Write to file
syscall
test rax,rax                   ; Check RAX = Status
jz .Skip_Operation             ; Go if error WRITE file, length = 0
js .Skip_Operation             ; Go if error WRITE file, length < 0, status
;--- Cycle for Read/Write phases ---
dec r12
jnz .CopyBlock                         ; Cycle for blocks
;--- Positioning pointer to position=0 ---
;--- Source ---
mov rdi,rbx                    ; Parm#1 = Source file descriptor, handle
xor esi,esi                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
lea r10,[r15+32]               ; Parm#4 = Pointer to variable return position
xor r9d,r9d                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK              ; Function code = seek
syscall
test rax,rax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Destination ---
mov rdi,rbp                    ; Parm#1 = Destination file descriptor, handle
xor esi,esi                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
lea r10,[r15+32]               ; Parm#4 = Pointer to variable return position
xor r9d,r9d                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK              ; Function code = seek
syscall
test rax,rax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Measurement cycle for COPY ---
dec r13
jnz .CopyMeasurement
;--- Get time at file COPY stop, calculate delta ---
xor edi,edi                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[r15+56]               ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME            ; Function code = Get timer
syscall
test rax,rax
jnz .Skip_Operation
mov rax,1000000000             ; RAX = Nanoseconds per one second
mul qword [r15+40]             ; First qword = Seconds
add rax,[r15+48]               ; Second qword = Nanoseconds
xchg rcx,rax
mov rax,1000000000             ; RAX = Nanoseconds per one second
mul qword [r15+56]             ; First qword = Seconds
add rax,[r15+64]               ; Second qword = Nanoseconds
sub rax,rcx                    ; RAX = After operation time, RCX = Before operation time
mov [r15+16],rax               ; Return delta = RAX-RCX nanoseconds per COPY operation 
;--- Exit point with files delete ---
.Skip_Operation:
;--- Close and delete source file ---
mov rdi,rbx                    ; Parm#1 = Source file descriptor, handle
mov eax,SYS_CLOSE
syscall
test rax,rax
jnz @f
lea rdi,[r14+48]               ; Parm#1 = Pointer to source file path
mov eax,SYS_UNLINK
syscall
@@:
;--- Close and delete destination file ---
mov rdi,rbp                    ; Parm#1 = Destination file descriptor, handle
mov eax,SYS_CLOSE
syscall
test rax,rax
jnz @f
lea rdi,[r14+1024]             ; Parm#1 = Pointer to destination file path
mov eax,SYS_UNLINK
syscall
test rax,rax
jnz @f                         ; Skip set status flag if error
inc dword [r15+24]
@@:
;--- Exit ---
.Exit_Operation:
pop r15 r14 r13 r12 rbp rdi rsi rbx rbx
ret


