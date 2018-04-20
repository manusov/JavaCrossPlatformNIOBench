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
;         QWORD IPB[40] = Measurement iterations count
;         976 bytes IPB[48-1023] = Source file path string                     ;
;         1024 bytes IPB[1024-2047] = Destination file path string             ;
;         BLOCK_SIZE [800h...X] = Source file I/O buffer                       ;
;                                                                              ;
; OPB:    QWORD OPB[00] = File write result, 0=Error, otherwise dt units 100 ns;
;         QWORD OPB[08] = File read result, 0=Error, otherwise dt units 100ns  ; 
;         QWORD OPB[16] = File copy result, 0=Error, otherwise dt units 100 ns ; 
;         QWORD OPB[24] = File delete result, 0=Error, otherwise > 0           ;
;         QWORD OPB[32] = Used for API function return size                    ;
;         QWORD OPB[40] = Used for API function return time, start point       ;
;         QWORD OPB[48] = Used for API function return time, stop point        ;
;         1992 bytes OPB [56-2047] = Reserved                                  ;
;         BLOCK_SIZE [800h...X] = Destination file I/O buffer                  ;
;                                                                              ;
;------------------------------------------------------------------------------;

API_WRITE_READ_COPY_DELETE:                    ; Assume RSP aligned before call
push rbx rbx rsi rdi rbp r12 r13 r14 r15       ; RBX push twice for RSP align

;--- v0.47 ---
mov rbp,rsp                   ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h    ; align 16
push rbp rbp                  ; save RBP, twice for align 16
sub rsp,32                    ; make parameter shadow
;--- v0.47 ---

;--- Clear output variables ---
cld
xor eax,eax
mov ecx,7
rep stosq
;--- Create source file ---
lea rcx,[rsi+48]                       ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ OR GENERIC_WRITE  ; RDX = Parm#2 = Desired access
xor r8d,r8d                            ; R8  = Parm#3 = Share mode = 0
xor r9d,r9d                            ; R9  = Parm#4 = Security attributes = 0
push r9                                ; This for stack alignment
push r9                                ; Parm#7 = Template file handle, not used 
push qword [rsi+24]                    ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                     ; Parm#5 = Creation disposition
sub rsp,32
call [CreateFileA]
add rsp,32+32
test rax,rax                           ; Check RAX = Handle
jz .Skip_Operation                     ; Go if error create file
xchg rbx,rax                           ; RBX = Source file handle, XCHG compact
;--- Get time at file WRITE start ---
lea rcx,[rdi-16]
call [GetSystemTimeAsFileTime]         ; Return time stamp, units = 100 ns 
;--- Measurement cycle for WRITE ---
mov r14,[rsi+40]
.WriteMeasurement:
;--- Prepare parameters for WRITE operation cycle ---
mov r12,[rsi+08]                       ; R12 = Block size
mov r13,[rsi+16]                       ; R13 = Block count
;--- Write source file ---
.WriteBlock:
mov rcx,rbx                            ; RCX = Parm#1 = Target file handle
lea rdx,[rsi+2048]                     ; RDX = Parm#2 = Pointer to buffer
mov r8,r12                             ; R8  = Parm#3 = Block size
lea r9,[rdi-24]                        ; R9  = Parm#4 = Pointer for size ret.
xor eax,eax
mov [r9],rax
push rax                               ; This for stack alignment
push rax                               ; Parm#5 = Pointer to overlapped
sub rsp,32
call [WriteFile]
add rsp,32+16
test rax,rax                           ; Check RAX = Status
jz .Skip_Operation                     ; Go if error write file
dec r13
jnz .WriteBlock                        ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- v0.47 --- sub rsp,32
call [SetFilePointer]
;--- Start of added at v0.47 ---
;- mov rcx,rbx
;- call [FlushFileBuffers]
;--- End of added at v0.47 ---
;--- v0.47 --- add rsp,32
;--- Measurement cycle for WRITE ---
dec r14
jnz .WriteMeasurement
;--- Get time at file WRITE stop, calculate delta ---
lea rcx,[rdi-08]
call [GetSystemTimeAsFileTime]         ; Return time stamp, units = 100 ns 
mov rax,[rdi-08]
sub rax,[rdi-16]
mov [rdi-56],rax
;--- Get time at file READ start ---
lea rcx,[rdi-16]
call [GetSystemTimeAsFileTime]         ; Return time stamp, units = 100 ns 
;--- Measurement cycle for READ ---
mov r14,[rsi+40]
.ReadMeasurement:
;--- Prepare parameters for READ operation cycle ---
mov r12,[rsi+08]                       ; R12 = Block size
mov r13,[rsi+16]                       ; R13 = Block count
;--- Read source file ---
.ReadBlock:
mov rcx,rbx                            ; RCX = Parm#1 = Target file handle
lea rdx,[rdi+2048-56]                  ; RDX = Parm#2 = Pointer to buffer
mov r8,r12                             ; R8  = Parm#3 = Block size
lea r9,[rdi-24]                        ; R9  = Parm#4 = Pointer for size ret.
xor eax,eax
mov [r9],rax
push rax                               ; This for stack alignment
push rax                               ; Parm#5 = Pointer to overlapped
sub rsp,32
call [ReadFile]
add rsp,32+16
test rax,rax                           ; Check RAX = Status
jz .Skip_Operation                      ; Go if error write file
dec r13
jnz .ReadBlock                         ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- v0.47 --- sub rsp,32
call [SetFilePointer]
;--- Start of added at v0.47 ---
;- mov rcx,rbx
;- call [FlushFileBuffers]
;--- End of added at v0.47 ---
;--- v0.47 --- add rsp,32
;--- Measurement cycle for READ ---
dec r14
jnz .ReadMeasurement
;--- Get time at file READ stop, calculate delta ---
lea rcx,[rdi-08]
call [GetSystemTimeAsFileTime]         ; Return time stamp, units = 100 ns 
mov rax,[rdi-08]
sub rax,[rdi-16]
mov [rdi-48],rax
;--- Create destination file ---
lea rcx,[rsi+1024]                     ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ OR GENERIC_WRITE  ; RDX = Parm#2 = Desired access
xor r8d,r8d                            ; R8  = Parm#3 = Share mode = 0
xor r9d,r9d                            ; R9  = Parm#4 = Security attributes = 0
push r9                                ; This for stack alignment
push r9                                ; Parm#7 = Template file handle, not used 
push qword [rsi+32]                    ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                     ; Parm#5 = Creation disposition
sub rsp,32
call [CreateFileA]
add rsp,32+32
test rax,rax                           ; Check RAX = Handle
jz .Skip_Operation                     ; Go if error create file
xchg rbp,rax                           ; RBP = Destination file handle
;--- Positioning pointer to position=0 ---
; mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
; xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
; xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
; xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
; sub rsp,32
; call [SetFilePointer]
; add rsp,32
;--- Get time at file COPY start ---
lea rcx,[rdi-16]
call [GetSystemTimeAsFileTime]         ; Return time stamp, units = 100 ns 
;--- Measurement cycle for COPY ---
mov r14,[rsi+40]
.CopyMeasurement:
;--- Prepare parameters for COPY operation cycle ---
mov r12,[rsi+08]                       ; R12 = Block size
mov r13,[rsi+16]                       ; R13 = Block count
;--- Copy source file ---
.CopyBlock:
;--- Read phase, source file ---
mov rcx,rbx                            ; RCX = Parm#1 = Source file handle
lea rdx,[rdi+2048-56]                  ; RDX = Parm#2 = Pointer to buffer
mov r8,r12                             ; R8  = Parm#3 = Block size
lea r9,[rdi-24]                        ; R9  = Parm#4 = Pointer for size ret.
xor eax,eax
mov [r9],rax
push rax                               ; This for stack alignment
push rax                               ; Parm#5 = Pointer to overlapped
sub rsp,32
call [ReadFile]
add rsp,32+16
test rax,rax                           ; Check RAX = Status
jz .Skip_Operation                      ; Go if error write file
;--- Write phase, destination cycle ---
mov rcx,rbp                            ; RCX = Parm#1 = Destination file handle
lea rdx,[rdi+2048-56]                  ; RDX = Parm#2 = Pointer to buffer
mov r8,r12                             ; R8  = Parm#3 = Block size
lea r9,[rdi-24]                        ; R9  = Parm#4 = Pointer for size ret.
xor eax,eax
mov [r9],rax
push rax                               ; This for stack alignment
push rax                               ; Parm#5 = Pointer to overlapped
sub rsp,32
call [WriteFile]
add rsp,32+16
test rax,rax                           ; Check RAX = Status
jz .Skip_Operation                     ; Go if error write file
;--- Cycle for Read/Write phases ---
dec r13
jnz .CopyBlock                         ; Cycle for blocks
;--- Positioning pointer to position=0 ---
;--- Source ---
mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- v0.47 --- sub rsp,32
call [SetFilePointer]
;--- Start of added at v0.47 ---
;- mov rcx,rbx
;- call [FlushFileBuffers]
;--- End of added at v0.47 ---
;--- v0.47 --- add rsp,32
;--- Destination ---
mov rcx,rbp                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- v0.47 --- sub rsp,32
call [SetFilePointer]
;--- Start of added at v0.47 ---
;- mov rcx,rbp
;- call [FlushFileBuffers]
;--- End of added at v0.47 ---
;--- v0.47 --- add rsp,32
;--- Measurement cycle for COPY ---
dec r14
jnz .CopyMeasurement
;--- Get time at file COPY stop, calculate delta ---
lea rcx,[rdi-08]
call [GetSystemTimeAsFileTime]         ; Return time stamp, units = 100 ns 
mov rax,[rdi-08]
sub rax,[rdi-16]
mov [rdi-40],rax
;--- Exit point with files delete ---
.Skip_Operation:
;--- Close and delete source file ---
mov rcx,rbx
call [CloseHandle]
test rax,rax
jz @f
lea rcx,[rsi+48]                       ; RCX = Parm#1 = Pointer to file path
call [DeleteFileA]
@@:
;--- Close and delete destination file ---
mov rcx,rbp
call [CloseHandle]
test rax,rax
jz @f
lea rcx,[rsi+1024]                     ; RCX = Parm#1 = Pointer to file path
call [DeleteFileA]
mov [rdi-32],rax
@@:
;--- Exit ---

;--- v0.47 ---
add rsp,32        ; remove parameters shadow
pop rbp rbp       ; restore RBP, twice for align 16
mov rsp,rbp       ; restore RSP after align 16 
;--- v0.47 ---

pop r15 r14 r13 r12 rbp rdi rsi rbx rbx
ret
