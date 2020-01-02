;---------- Copy file with measurement iterations -----------------------------;
;                                                                              ;
; Input:   RSI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Linux64 JNI OK                         ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O allocated by Java Code,                           ;
; base address of this buffer = OPB+4096                                       ;
; size of this buffer = I/O block size + 4096, for alignment reservation       ;  
; For Copy: both source and destination files parameters used                  ;
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
; QWORD [IPB+00] = Required function ID                                        ;
; QWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; QWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; QWORD [IPB+24] = Source file performance attributes,                         ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; QWORD [IPB+32] = Destination file performance attributes,                    ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; QWORD [IPB+40] = Number of measurement repeats iterations                    ;
; Max. 976 BYTES [IPB+48] = Reserved                                           ; 
; 1024 BYTES [IPB+1024] = Source file path string, null terminated             ;
; 1024 BYTES [IPB+2048] = Destination file path string, null terminated        ;
; 1024 BYTES [IPB+3072] = Reserved                                             ;  
;                                                                              ;
; OPB layout (4096 bytes):                                                     ;
; QWORD [OPB+00] = Memory buffer base address, available after alignment       ;
; QWORD [OPB+08] = Memory buffer size, bytes, available after alignment        ;       
; QWORD [OPB+16] = Source file handle, available after open file               ;
; QWORD [OPB+24] = Destination file handle, available after open file          ;
; QWORD [OPB+32] = Executed read/write/copy operation size,                    ; 
;                  available after operation execute                           ;
; QWORD [OPB+40] = Timer at start of measurement repeats, units = 100 ns       ;
; QWORD [OPB+48] = Timer at end of measurement repeats, units = 100 ns         ;
; QWORD [OPB+56] = Last called OS API status, see Linux Sys Call Reference     ;
; QWORD [OPB+64] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
; 4024 BYTES [IPB+72] = Reserved                                               ;
;                                                                              ;
;------------------------------------------------------------------------------;
MeasureCopyFile:
push rbx rbx rsi rdi rbp r12 r13 r14 r15       ; RBX push twice for RSP align

;--- Copy parameters to non-volatile registers ---
mov r14,rsi                 ; R14 = Pointer to IPB
mov r15,rdi                 ; R15 = Pointer to OPB

;--- Clear output variables ---
cld
xor eax,eax
mov ecx,OPB_CLEAR_QWORDS
rep stosq

;--- Calculate aligned base and size for memory buffer ---
lea rbx,OPB_DATA_ARRAY
test ebx,0FFFh
jz @f
and rbx,0FFFFFFFFFFFFF000h
add rbx,00000000000001000h
@@:
mov OPB_BUFFER_BASE,rbx     ; RBX = Work buffer base
mov rbp,IPB_BLOCK_SIZE
mov OPB_BUFFER_SIZE,rbp     ; RBP = Work buffer size

;--- Open source file ---
lea rdi,IPB_SRC_PATH        ; RDI = Parm#1 = Pointer to source file path
mov rsi,IPB_SRC_ATTRIBUTES  ; RSI = Parm#2 = File operation mode flags
mov edx,00000C00h           ; RDX = Parm#3 = File access read-write mode flags
mov eax,SYS_OPEN            ; RAX = Function code, open file
syscall
mov ecx,STEP_OPEN_READ
test rax,rax
jz ErrorFileContext
cmp rax,7FFFFFFFh
ja ErrorFileContext
mov OPB_SRC_HANDLE,rax      ; Store source file handle

;--- Open destination file ---
lea rdi,IPB_DST_PATH        ; RDI = Parm#1 = Pointer to destination file path
mov rsi,IPB_DST_ATTRIBUTES  ; RSI = Parm#2 = File operation mode flags
mov edx,00000C00h           ; RDX = Parm#3 = File access read-write mode flags
mov eax,SYS_OPEN            ; RAX = Function code, open file
syscall
mov ecx,STEP_OPEN_WRITE
test rax,rax
jz ErrorFileContext
cmp rax,7FFFFFFFh
ja ErrorFileContext
mov OPB_DST_HANDLE,rax      ; Store destination file handle

;--- Get and store time at file COPY start ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T1      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_READ
test rax,rax
jnz ErrorFileContext

;--- Measurement cycle for COPY ---
mov r13,IPB_ITERATIONS     ; R13 = measurement counter

;--- Start sequence of blocks cycle for COPY ---
.CopyMeasurement:
xor r12,r12                ; R12 = total size accumulator

;--- Read block of source file ---
.CopyBlock:
mov rsi,rbx                ; RSI = Parm#2 = Pointer to buffer
mov rdx,rbp                ; RDX = Parm#3 = RDX = Buffer size, byte count per call

;--- Read sub-block of source file ---
.ReadSubBlock:
mov rdi,OPB_SRC_HANDLE     ; RDI = Parm#1 = File descriptor, handle
mov eax,SYS_READ           ; RAX = Function code = Read file
push rdx rsi
syscall
pop rsi rdx
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jz ErrorFileContext        ; Go if error read file, length = 0
js ErrorFileContext        ; Go if error read file, length < 0, status

;--- Cycle for sub-blocks ---
add rsi,rax                ; Advance RSI = pointer by returned size
sub rdx,rax                ; Subtract returned size from RDX = total block size
jnz .ReadSubBlock          ; Repeat if block size remain

mov rsi,rbx                ; RSI = Parm#2 = Pointer to buffer
mov rdx,rbp                ; RDX = Parm#3 = RDX = Buffer size, byte count per call

;--- Write sub-block of destination file ---
.WriteSubBlock:
mov rdi,OPB_DST_HANDLE     ; RDI = Parm#1 = File descriptor, handle
mov eax,SYS_WRITE          ; RAX = Function code = Write to file
push rdx rsi
syscall
pop rsi rdx
mov ecx,STEP_WRITE
test rax,rax               ; Check RAX = Status
jz ErrorFileContext        ; Go if error write file, length = 0
js ErrorFileContext        ; Go if error write file, length < 0, status;

;--- Cycle for sub-blocks ---
add rsi,rax                ; Advance RSI = pointer by returned size
sub rdx,rax                ; Subtract returned size from RDX = total block size
jnz .WriteSubBlock         ; Repeat if block size remain

;--- Cycle for blocks, read file as sequence of blocks ---
add r12,rbp
cmp r12,IPB_REQUEST_SIZE
jb .CopyBlock

;--- Reset source file pointer for repeats, to position = 0 ---
mov rdi,OPB_SRC_HANDLE     ; RDI = Parm#1 = File descriptor, handle
xor esi,esi                ; RSI = Parm#2 = File offset = 0
xor edx,edx                ; RDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK          ; RAX = Function code = seek
syscall
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jnz ErrorFileContext       ; Go if error seek file

;--- Reset destination file pointer for repeats, to position = 0 ---
mov rdi,OPB_DST_HANDLE     ; RDI = Parm#1 = File descriptor, handle
xor esi,esi                ; RSI = Parm#2 = File offset = 0
xor edx,edx                ; RDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK          ; RAX = Function code = seek
syscall
mov ecx,STEP_WRITE
test rax,rax               ; Check RAX = Status
jnz ErrorFileContext       ; Go if error seek file

;--- Cycle for measurement iterations ---
dec r13
jnz .CopyMeasurement

;--- Get and store time at file COPY end ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T2      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_READ
test rax,rax
jnz ErrorFileContext

;--- Close source file ---
mov rdi,OPB_SRC_HANDLE      ; RDI = Parm#1 = Source file descriptor, handle
mov eax,SYS_CLOSE
syscall
mov ecx,STEP_CLOSE
test rax,rax
jnz ErrorFileContext

;--- Close destination file ---
mov rdi,OPB_DST_HANDLE      ; RDI = Parm#1 = Destination file descriptor, handle
mov eax,SYS_CLOSE
syscall
mov ecx,STEP_CLOSE
test rax,rax
jnz ErrorFileContext

;--- Go to restore stack after alignment and done ---
jmp ExitFileContext 
