;---------- Write, Copy, Read sequence of files with measurement iterations ---;
; Note legacy NIOBench scenario with all 3 operations (write, copy, read)      ;
; per 1 file at iteration can be supported with fileCount = 1                  ;
; This optimal when insufficiient disk space, but can reduce speed             ;
; compare with sequence: Write-All , Copy-All , Read - All.                    ;
;                                                                              ;
; Input:   RSI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK                           ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O and files handles allocated by Java Code,         ;
; base address of file IO buffer = OPB + 4096                                  ;
; size of files handles buffer = fileCount * 8,                                ;
; base address of files handles buffer = OPB + 4096 + fileCount * 8            ;
; size of file I/O buffer = I/O block size + 4096, for alignment reservation   ;  
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
;                                                                              ;
; QWORD [IPB+00] = Required function ID                                        ;
; QWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; QWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; QWORD [IPB+24] = Source file performance attributes,                         ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; QWORD [IPB+32] = Destination file performance attributes,                    ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;;                  see Linux Sys Call Reference, OPEN function, parm#2        ;
; QWORD [IPB+40] = Number of measurement repeats iterations                    ;
; QWORD [IPB+48] = Number of files per series,                                 ; 
;                  note 1 file per series means legacy NIOBench scenario       ; 
; DATA AREA 968 BYTES [IPB+56] = Reserved                                      ; 
; 512 BYTES [IPB+1024] = Source file path PRIFIX string, null terminated       ;
; 512 BYTES [IPB+1536] = Source file path POSTFIX string, null terminated      ;
; 512 BYTES [IPB+2048] = Destination file path PREFIX string, null terminated  ;
; 512 BYTES [IPB+2560] = Destination file path POSTFIX string, null term.      ;
;                      For operations with series of files, file ID generated  ; 
;                      as decimal number string between PREFIX and POSTFIX     ;
; DATA AREA 1024 BYTES [IPB+3072] = Reserved                                   ;  
;                                                                              ;
; OPB layout (4096 bytes):                                                     ;
;                                                                              ;
; Unified layout, same as single operation (Read, Write, Copy) functions,      ;
; actual for file, associated with last executed operation                     ; 
; QWORD [OPB+00] = Memory buffer base address, available after alignment       ;
; QWORD [OPB+08] = Memory buffer size, bytes, available after alignment        ;       
; QWORD [OPB+16] = Source file handle, available after open file               ;
; QWORD [OPB+24] = Destination file handle, available after open file          ;
; QWORD [OPB+32] = Executed read/write/copy operation size,                    ; 
;                  available after operation execute                           ;
; QWORD [OPB+40] = Timer delta for measurement repeats, units = 1 ns           ;
;                  For multi-operations this value used as last                ;
;                  operation result, see separate values for Read, Write, Copy ;
; QWORD [OPB+48] = Last called OS API status, see Linux Sys Call Reference     ;
; QWORD [OPB+56] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
;                                                                              ;
; DATA AREA from [OPB+64] to [OPB+127] can be used for                         ; 
; OS-scefific undocumented scratch pads                                        ;                                                                   
; Use OS-specific scratch pad at OPB[80-95], 16 bytes (for timer API)          ;
; Use OS-specific measurement counter at OPB[96-104], 8 bytes                  ; 
;                                                                              ;
; Special layout for multi operation (Read, Write, Copy) functions:            ;
; QWORD [OPB+128] = Memory buffer base address for store source files handles  ;
;                   8 bytes per handle, buffer size = file count * 8           ;
; QWORD [OPB+136] = Memory buf. base addr. for store destination files handles ;
;                   8 bytes per handle, buffer size = file count * 8           ;
;                                                                              ;
; QWORD [OPB+144] = Multi-operation Read phase dT, units = 1 ns                ; 
; QWORD [OPB+152] = Multi-operation Write phase dT, units = 1 ns               ;
; QWORD [OPB+160] = Multi-operation Copy phase dT, units = 1 ns                ;
;                                                                              ;
; QWORD [OPB+168] = Multi-operation Read phase total executed IO size          ;  
; QWORD [OPB+176] = Multi-operation Write phase total executed IO size         ; 
; QWORD [OPB+184] = Multi-operation Copy phase total executed IO size          ; 
;                                                                              ;
; DATA AREA 832 BYTES from [OPB+192] to [OPB+1023] = Reserved                  ;
; DATA AREA 1024 BYTES [OPB+1024] = Build path string:                         ;
;                                   prefix + number + postfix                  ;
; DATA AREA 2048 BYTES from [OPB+2048] to [OPB+4095] = Reserved                ;
;                                                                              ;
;------------------------------------------------------------------------------;

PrecisionLinear:
push rbx rbx rsi rdi rbp r12 r13 r14 r15       ; RBX push twice for RSP align

;--- Copy parameters to non-volatile registers ---
mov r14,rsi                 ; R14 = Pointer to IPB
mov r15,rdi                 ; R15 = Pointer to OPB

;--- Clear output variables ---
call HelperClearOPB

;--- Calculate aligned base and size for memory buffer ---
call HelperDTA               ; RBX = Work buffer base , RBP = Work buffer size

;========== Source and Destination Files Create phase =========================;

;--- Initializing cycle for creating source files ---
lea r12,[rbx+rbp]             ; R12 = Pointer to files handles buffer
mov OPB_HANDLES_SRC_BASE,r12
imul rax,IPB_FILE_COUNT,8
add rax,r12
mov OPB_HANDLES_DST_BASE,rax
xor r13,r13                   ; R13 = Counter for number of files
;--- Cycle for creating source files ---  
.CreateSrcFiles:
lea rcx,IPB_SRC_PATH_PREFIX
lea rdx,IPB_SRC_PATH_POSTFIX
lea r8,OPB_COMPLEX_PATH
mov eax,r13d
call HelperComplexPath
call HelperCreateSrcGroup     ; Return RAX = file handle or
test rax,rax                  ; RAX = 0 if typical errors detected
mov [r12],rax                 ; Store handle
mov eax,STEP_OPEN_WRITE 
jz ErrorFileContext 
add r12,8
inc r13
cmp r13,IPB_FILE_COUNT
jb .CreateSrcFiles
;--- Initializing cycle for creating destination files ---
mov r12,OPB_HANDLES_DST_BASE  ; R12 = Pointer to files handles buffer
xor r13,r13                   ; R13 = Counter for number of files
;--- Cycle for creating destination files ---  
.CreateDstFiles:
lea rcx,IPB_DST_PATH_PREFIX
lea rdx,IPB_DST_PATH_POSTFIX
lea r8,OPB_COMPLEX_PATH
mov eax,r13d
call HelperComplexPath
call HelperCreateDstGroup     ; Return RAX = file handle or
test rax,rax                  ; RAX = 0 if typical errors detected
mov [r12],rax                 ; Store handle
mov eax,STEP_OPEN_WRITE 
jz ErrorFileContext 
add r12,8
inc r13
cmp r13,IPB_FILE_COUNT
jb .CreateDstFiles

;========== Source Files Write phase ==========================================;

;--- Initializing cycle for write files ---
mov r13,IPB_ITERATIONS        ; R13 = Measurement iterations 
;--- Get timer at start of write files sequence ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T1      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_WRITE
test rax,rax
jnz ErrorFileContext
;--- Measurement iterationa ---
.WriteMeasurement:
mov rax,IPB_FILE_COUNT         
mov OPB_TRANSIT_FILE,eax       ; Files counter, note qword/dword mismatch rax/eax
mov r12,OPB_HANDLES_SRC_BASE   ; R12 = Pointer to array of opened files handles
;--- Cycle for write files ---
.WriteFiles:
mov OPB_TRANSIT_BLOCK,0
mov rbx,OPB_BUFFER_BASE
;--- Write file ---
.WriteBlock:
mov rdi,[r12]              ; RDI = Parm#1 = File descriptor, handle
mov rsi,rbx                ; RSI = Parm#2 = Pointer to buffer
mov rdx,OPB_BUFFER_SIZE    ; RDX = Parm#3 = RDX = Buffer size, byte count per call
mov eax,SYS_WRITE          ; RAX = Function code = Write to file
syscall
mov ecx,STEP_WRITE
test rax,rax               ; Check RAX = Status
jz ErrorFileContext        ; Go if error write file, length = 0
js ErrorFileContext        ; Go if error write file, length < 0, status
add OPB_TOTAL_WRITE,rax
;--- Cycle for blocks ---
add OPB_TRANSIT_BLOCK,rax
mov rax,IPB_REQUEST_SIZE
cmp OPB_TRANSIT_BLOCK,rax
jb .WriteBlock
;--- End measurement cycle, get and store stop timer value ---
.WriteDone:
mov rdi,[r12]              ; RDI = Parm#1 = File descriptor, handle
xor esi,esi                ; RSI = Parm#2 = File offset = 0
xor edx,edx                ; RDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK          ; RAX = Function code = seek
syscall
mov ecx,STEP_WRITE
test rax,rax               ; Check RAX = Status
jnz ErrorFileContext       ; Go if error seek file
;--- Count files in the sequence ---
add r12,8
dec OPB_TRANSIT_FILE
jnz .WriteFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec r13
jnz .WriteMeasurement
;--- Get timer at etop of write files sequence ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T2      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_WRITE
test rax,rax
jnz ErrorFileContext
call HelperDeltaTime
mov OPB_TIMER_WRITE,rax

;========== Source to Destination Files Copy phase ============================;
; Note file pointers re-positioned to files begin by previous sysles

;--- Initializing cycle for copy files ---
mov rax,IPB_ITERATIONS         ; Measurement iterations
mov OPB_TRANSIT_MEASURE,eax
;--- Get timer at start of copy files sequence ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T1      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_READ
test rax,rax
jnz ErrorFileContext
;--- Measurement iteration ---
.CopyMeasurement:
xor r12,r12                 ; R12 = Files counter
;--- Cycle for copy files ---
.CopyFiles:
xor ebp,ebp
mov OPB_TRANSIT_BLOCK,0     ; Block size accumulator
.ReadWriteBlock:
;--- Read one block from source file ---
mov rbx,OPB_BUFFER_BASE
.ReadCopyBlock:
imul rax,r12,8
add rax,OPB_HANDLES_SRC_BASE
mov rdi,[rax]              ; RDI = Parm#1 = File descriptor, handle
mov rsi,rbx                ; RSI = Parm#2 = Pointer to buffer
mov rdx,OPB_BUFFER_SIZE    ; RDX = Parm#3 = RDX = Buffer size, byte count per call
mov eax,SYS_READ           ; RAX = Function code = Read file
syscall
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jz ErrorFileContext        ; Go if error read file, length = 0
js ErrorFileContext        ; Go if error read file, length < 0, status
;--- Cycle for blocks READ ---
add rbx,rax
add OPB_TRANSIT_BLOCK,rax
mov rax,IPB_BLOCK_SIZE
cmp OPB_TRANSIT_BLOCK,rax
jb .ReadCopyBlock
.ReadCopyDone:
;--- Write one block to destination file ---
mov OPB_TRANSIT_BLOCK,0     ; Block size accumulator
mov rbx,OPB_BUFFER_BASE
.WriteCopyBlock:
imul rax,r12,8
add rax,OPB_HANDLES_DST_BASE
mov rdi,[rax]              ; RDI = Parm#1 = File descriptor, handle
mov rsi,rbx                ; RSI = Parm#2 = Pointer to buffer
mov rdx,OPB_BUFFER_SIZE    ; RDX = Parm#3 = RDX = Buffer size, byte count per call
mov eax,SYS_WRITE          ; RAX = Function code = Write file
syscall
mov ecx,STEP_WRITE
test rax,rax               ; Check RAX = Status
jz ErrorFileContext        ; Go if error read file, length = 0
js ErrorFileContext        ; Go if error read file, length < 0, status
add OPB_TOTAL_COPY,rax
;--- Cycle for blocks WRITE ---
add rbx,rax
add OPB_TRANSIT_BLOCK,rax
mov rax,IPB_BLOCK_SIZE
cmp OPB_TRANSIT_BLOCK,rax
jb .WriteCopyBlock
.WriteCopyDone:
;--- Cycle for blocks COPY ---
add rbp,IPB_BLOCK_SIZE
cmp rbp,IPB_REQUEST_SIZE
jb .ReadWriteBlock  
;--- Re-positioning to SOURCE file begin ---
imul rax,r12,8
add rax,OPB_HANDLES_SRC_BASE
mov rdi,[rax]              ; RDI = Parm#1 = File descriptor, handle
xor esi,esi                ; RSI = Parm#2 = File offset = 0
xor edx,edx                ; RDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK          ; RAX = Function code = seek
syscall
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jnz ErrorFileContext       ; Go if error seek file
;--- Re-positioning to DESTINATION file begin ---
imul rax,r12,8
add rax,OPB_HANDLES_DST_BASE
mov rdi,[rax]              ; RDI = Parm#1 = File descriptor, handle
xor esi,esi                ; RSI = Parm#2 = File offset = 0
xor edx,edx                ; RDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK          ; RAX = Function code = seek
syscall
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jnz ErrorFileContext       ; Go if error seek file
;--- Count files in the sequence ---
inc r12
cmp r12,IPB_FILE_COUNT         ; R12 = Files counter
jb .CopyFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec OPB_TRANSIT_MEASURE
jnz .CopyMeasurement
;--- Get timer at etop of copy files sequence ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T2      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_WRITE
test rax,rax
jnz ErrorFileContext
call HelperDeltaTime
mov OPB_TIMER_COPY,rax

;========== Source Files Read phase ===========================================;

;--- Initializing cycle for read files ---
mov r13,IPB_ITERATIONS        ; R13 = Measurement iterations 
;--- Get timer at start of read files sequence ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T1      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_READ
test rax,rax
jnz ErrorFileContext
;--- Measurement iterationa ---
.ReadMeasurement:
mov rax,IPB_FILE_COUNT         
mov OPB_TRANSIT_FILE,eax       ; Files counter, note qword/dword mismatch rax/eax
mov r12,OPB_HANDLES_SRC_BASE   ; R12 = Pointer to array of opened files handles
;--- Cycle for read files ---
.ReadFiles:
mov OPB_TRANSIT_BLOCK,0
mov rbx,OPB_BUFFER_BASE
;--- Read file ---
.ReadBlock:
mov rdi,[r12]              ; RDI = Parm#1 = File descriptor, handle
mov rsi,rbx                ; RSI = Parm#2 = Pointer to buffer
mov rdx,OPB_BUFFER_SIZE    ; RDX = Parm#3 = RDX = Buffer size, byte count per call
mov eax,SYS_READ           ; RAX = Function code = Read file
syscall
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jz ErrorFileContext        ; Go if error read file, length = 0
js ErrorFileContext        ; Go if error read file, length < 0, status
add OPB_TOTAL_READ,rax
;--- Cycle for blocks ---
add OPB_TRANSIT_BLOCK,rax
mov rax,IPB_REQUEST_SIZE
cmp OPB_TRANSIT_BLOCK,rax
jb .ReadBlock
;--- End measurement cycle, get and store stop timer value ---
.ReadDone:
mov rdi,[r12]              ; RDI = Parm#1 = File descriptor, handle
xor esi,esi                ; RSI = Parm#2 = File offset = 0
xor edx,edx                ; RDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,SYS_LSEEK          ; RAX = Function code = seek
syscall
mov ecx,STEP_READ
test rax,rax               ; Check RAX = Status
jnz ErrorFileContext       ; Go if error seek file
;--- Count files in the sequence ---
add r12,8
dec OPB_TRANSIT_FILE
jnz .ReadFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec r13
jnz .ReadMeasurement
;--- Get timer at etop of read files sequence ---
xor edi,edi                 ; RDI = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,OPB_TRANSIT_T2      ; RSI = Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETTIME         ; RAX = Function code = Get timer
syscall
mov ecx,STEP_READ
test rax,rax
jnz ErrorFileContext
call HelperDeltaTime
mov OPB_TIMER_READ,rax

;========== Source and Destination Files Delete phase =================;

;--- Delete source files ---
mov rbx,OPB_HANDLES_SRC_BASE   ; RBX = Pointer to handles list
xor ebp,ebp                    ; EBP = Files counter
.DeleteSrc:
mov rdi,[rbx]      ; RDI = Parm#1 = Source file descriptor, handle
mov eax,SYS_CLOSE
syscall
test rax,rax
jnz @f
lea rcx,IPB_SRC_PATH_PREFIX
lea rdx,IPB_SRC_PATH_POSTFIX
lea r8,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
lea rdi,OPB_COMPLEX_PATH     ; Parm#1 = Pointer to source file path
mov eax,SYS_UNLINK
syscall
@@:
test rax,rax
mov eax,STEP_DELETE
jnz ErrorFileContext
add rbx,8
inc ebp
cmp rbp,IPB_FILE_COUNT       ; EBP = Files counter
jb .DeleteSrc
;--- Delete destination files ---
mov rbx,OPB_HANDLES_DST_BASE   ; RBX = Pointer to handles list
xor ebp,ebp                    ; EBP = Files counter
.DeleteDst:
mov rdi,[rbx]      ; RDI = Parm#1 = Source file descriptor, handle
mov eax,SYS_CLOSE
syscall
test rax,rax
jnz @f
lea rcx,IPB_DST_PATH_PREFIX
lea rdx,IPB_DST_PATH_POSTFIX
lea r8,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
lea rdi,OPB_COMPLEX_PATH     ; Parm#1 = Pointer to source file path
mov eax,SYS_UNLINK
syscall
@@:
test rax,rax
mov eax,STEP_DELETE
jnz ErrorFileContext
add rbx,8
inc ebp
cmp rbp,IPB_FILE_COUNT       ; EBP = Files counter
jb .DeleteDst

;--- Go to restore stack after alignment and done ---
jmp ExitFileContext 


