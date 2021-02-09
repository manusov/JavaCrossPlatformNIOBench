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
; OPB:    QWORD OPB[00] = File write result, 0=Error, otherwise dt units 100 ns;
;         QWORD OPB[08] = File read result, 0=Error, otherwise dt units 100ns  ; 
;         QWORD OPB[16] = File copy result, 0=Error, otherwise dt units 100 ns ; 
;         QWORD OPB[24] = File delete result, 0=Error, otherwise > 0           ;
;         QWORD OPB[32] = Used for API function return size                    ;
;         QWORD OPB[40] = Used for API function return time, start point       ;
;         QWORD OPB[48] = Used for API function return time, stop point        ;
;         QWORD OPB[56] = 4KB-aligned source buffer base address               ;
;         QWORD OPB[64] = 4KB-aligned destination buffer base address          ;   
;         1976 bytes OPB [72-2047] = Reserved                                  ;
;         BLOCK_SIZE [800h...X] = Destination file I/O buffer                  ;
;                                                                              ;
;------------------------------------------------------------------------------;

API_WRITE_READ_COPY_DELETE:                    ; Assume RSP aligned before call
push rbx rbx rsi rdi rbp r12 r13 r14 r15       ; RBX push twice for RSP align
;--- Alignment stack and reserve stack space for WinAPI, 32 bytes ---
mov rbp,rsp                   ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h    ; align 16
push rbp rbp                  ; save RBP, twice for align 16
sub rsp,32                    ; make parameter shadow for Win API
;--- Prepare aligned source and destination address ---
; INT3  ; CHECKPOINT FOR BUFFERS ADDRESSES SETUP
lea rax,[rsi+2048]
test eax,0FFFh
jz @f
and rax,0FFFFFFFFFFFFF000h
add rax,00000000000001000h
@@:
mov [rdi+56],rax
lea rax,[rdi+2048]
test eax,0FFFh
jz @f
and rax,0FFFFFFFFFFFFF000h
add rax,00000000000001000h
@@:
mov [rdi+64],rax
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
mov r13,[rsi+16]                       ; R13 = Block count
;--- Write source file ---
; INT3  ; CHECKPOINT FOR WRITE FILE
;--- Cycle for blocks by operation scenario ---
.WriteBlock:
mov r12,[rsi+08]                       ; R12 = Block size
mov r15,[rdi+56-56]                    ; R15 = Buffer offset for sub-blocks
;--- Cycle for sub-blocks by API ---
.WriteSubBlock:
mov rcx,rbx                            ; RCX = Parm#1 = Target file handle
mov rdx,r15                            ; RDX = Parm#2 = Pointer to buffer
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
;--- Cycle for sub-blocks by API ---
add r15,[rdi-24]                       ; Modify offset for sub-blocks per API
sub r12,[rdi-24]                       ; Modify remain size
jnz .WriteSubBlock                     ; Cycle for sub-blocks per API call
;--- Cycle for blocks by operation scenario ---
dec r13
jnz .WriteBlock                        ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- Positioning ---
call [SetFilePointer]
;- mov rcx,rbx
;- call [FlushFileBuffers]
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
mov r14,[rsi+40]                       ; R14 = Measurement cycles count
.ReadMeasurement:
;--- Prepare parameters for READ operation cycle ---
mov r13,[rsi+16]                       ; R13 = Block count
;--- Read source file ---
; INT3  ; CHECKPOINT FOR READ FILE
;--- Cycle for blocks by operation scenario ---
.ReadBlock:
mov r12,[rsi+08]                       ; R12 = Block size
mov r15,[rdi+64-56]                    ; R15 = Buffer offset for sub-blocks
;--- Cycle for sub-blocks by API ---
.ReadSubBlock:
mov rcx,rbx                            ; RCX = Parm#1 = Target file handle
mov rdx,r15                            ; RDX = Parm#2 = Pointer to buffer
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
;--- Cycle for sub-blocks by API ---
add r15,[rdi-24]                       ; Modify offset for sub-blocks per API
sub r12,[rdi-24]                       ; Modify remain size
jnz .ReadSubBlock                      ; Cycle for sub-blocks per API call
;--- Cycle for blocks by operation scenario ---
dec r13
jnz .ReadBlock                         ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- Positioning ---
call [SetFilePointer]
;- mov rcx,rbx
;- call [FlushFileBuffers]
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
mov r13,[rsi+16]                       ; R13 = Block count
;--- Copy source file ---
; INT3  ; CHECKPOINT FOR COPY FILE
;--- Cycle for blocks by operation scenario ---
.CopyBlock:
mov r12,[rsi+08]                       ; R12 = Block size
mov r15,[rdi+64-56]                    ; R15 = Buffer offset for sub-blocks

;--- Cycle for sub-blocks by API ---
.CopyReadSubBlock:
;--- Read phase, source file ---
mov rcx,rbx                            ; RCX = Parm#1 = Source file handle
mov rdx,r15                            ; RDX = Parm#2 = Pointer to buffer
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
;--- Cycle for sub-blocks by API ---
add r15,[rdi-24]                       ; Modify offset for sub-blocks per API
sub r12,[rdi-24]                       ; Modify remain size
jnz .CopyReadSubBlock                  ; Cycle for sub-blocks per API call

;--- Restore sub-block tracking parameters ---
mov r12,[rsi+08]                       ; R12 = Block size
mov r15,[rdi+64-56]                    ; R15 = Buffer offset for sub-blocks

;--- Cycle for sub-blocks by API ---
.CopyWriteSubBlock:
;--- Write phase, destination cycle ---
mov rcx,rbp                            ; RCX = Parm#1 = Destination file handle
mov rdx,r15                            ; RDX = Parm#2 = Pointer to buffer
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
;--- Cycle for sub-blocks by API ---
add r15,[rdi-24]                       ; Modify offset for sub-blocks per API
sub r12,[rdi-24]                       ; Modify remain size
jnz .CopyWriteSubBlock                 ; Cycle for sub-blocks per API call

;--- Cycle for Read/Write phases, blocks by operation scenario ---
dec r13
jnz .CopyBlock                         ; Cycle for blocks

;--- Positioning pointer to position=0 ---
;--- Source ---
mov rcx,rbx                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- Positioning ---
call [SetFilePointer]
;- mov rcx,rbx
;- call [FlushFileBuffers]
;--- Destination ---
mov rcx,rbp                           ; RCX = Parm#1 = Target file handle
xor edx,edx                           ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                           ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                           ; R9  = Parm#4 = Move method = FILE_BEGIN
;--- Positioning ---
call [SetFilePointer]
;- mov rcx,rbp
;- call [FlushFileBuffers]
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
;--- Restore stack after alignment ---
add rsp,32        ; remove parameters shadow
pop rbp rbp       ; restore RBP, twice for align 16
mov rsp,rbp       ; restore RSP after align 16 
;--- Exit ---
pop r15 r14 r13 r12 rbp rdi rsi rbx rbx
ret
