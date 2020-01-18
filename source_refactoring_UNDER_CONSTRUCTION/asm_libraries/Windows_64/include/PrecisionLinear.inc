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
;                  see MSDN, CreateFileA API parm#6                            ;
; QWORD [IPB+32] = Destination file performance attributes,                    ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;                  see MSDN, CreateFileA API parm#6                            ;
; QWORD [IPB+40] = Number of measurement repeats iterations                    ;
; QWORD [IPB+48] = Number of files per series,                                 ; 
;                  note 1 file per series means legacy NIOBench scenario       ; 
; DATA AREA 968 BYTES [IPB+56] = Reserved                                      ; 
; 1024 BYTES [IPB+1024] = Source file path PRIFIX string, null terminated      ;
; 1024 BYTES [IPB+1536] = Source file path POSTFIX string, null terminated     ;
; 1024 BYTES [IPB+2048] = Destination file path PREFIX string, null terminated ;
; 1024 BYTES [IPB+2560] = Destination file path POSTFIX string, null term.     ;
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
; QWORD [OPB+48] = Last called OS API status, see MSDN                         ;
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
push rbx rsi rdi rbp r12 r13 r14 r15
;--- Clear output variables ---
call HelperClearOPB
;--- Alignment stack and reserve stack space for WinAPI, 32 bytes ---
mov rbp,rsp                   ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h    ; align 16
push rbp          ; must be aligned by 16 with next operation
sub rsp,32+24     ; make parameter shadow plus >4 parameters for Win API
;--- Calculate aligned base and size for memory buffer ---
call HelperDTA                ; RBX = Work buffer base, RBP = Work buffer size  

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
lea rcx,OPB_TRANSIT_T1
call [GetSystemTimeAsFileTime]
;--- Measurement iterationa ---
.WriteMeasurement:
mov r14,IPB_FILE_COUNT         ; R14 = Files counter
mov r12,OPB_HANDLES_SRC_BASE   ; R12 = Pointer to array of opened files handles
;--- Cycle for write files ---
.WriteFiles:
xor r15,r15
mov rbx,OPB_BUFFER_BASE
;--- Write file ---
.WriteBlock:
mov rcx,[r12]              ; RCX = Parm#1 = Target file handle
mov rdx,rbx                ; RDX = Parm#2 = Pointer to buffer
mov r8,rbp                 ; R8  = Parm#3 = Block size
lea r9,PARM_6              ; R9  = Parm#4 = Pointer for size return
mov PARM_5,0               ; Parm#5 = Pointer to overlapped, not used
call [WriteFile]
test rax,rax
mov eax,STEP_WRITE 
jz ErrorFileContext
mov rax,PARM_6             ; RAX = Return executed write size
test rax,rax
jz .WriteDone              ; Go done if zero size returned
js .WriteDone              ; Go done if negative size returned
add OPB_TOTAL_WRITE,rax    ; Support total write size for code self-check
;--- Cycle for blocks ---
add r15,rax
cmp r15,IPB_REQUEST_SIZE
jb .WriteBlock
;--- End measurement cycle, get and store stop timer value ---
.WriteDone:
;--- Re-positioning to file begin ---
mov rcx,[r12]              ; RCX = Parm#1 = Target file handle
xor edx,edx                ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                ; R9  = Parm#4 = Move method = FILE_BEGIN
call [SetFilePointer]
cmp rax,INVALID_SET_FILE_POINTER
mov eax,STEP_WRITE 
je ErrorFileContext
;--- Count files in the sequence ---
add r12,8
dec r14
jnz .WriteFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec r13
jnz .WriteMeasurement
;--- Get timer at etop of write files sequence ---
lea rcx,OPB_TRANSIT_T2
call [GetSystemTimeAsFileTime]
call HelperDeltaTime
mov OPB_TIMER_WRITE,rax

;========== Source to Destination Files Copy phase ============================;
; Note file pointers re-positioned to files begin by previous sysles

;--- Initializing cycle for copy files ---
mov rax,IPB_ITERATIONS         ; Measurement iterations
mov OPB_TRANSIT_MEASURE,rax
;--- Get timer at start of copy files sequence ---
lea rcx,OPB_TRANSIT_T1
call [GetSystemTimeAsFileTime]
;--- Measurement iteration ---
.CopyMeasurement:
mov r12,OPB_HANDLES_SRC_BASE   ; R12 = Pointer to array of opened src. handles 
mov r14,OPB_HANDLES_DST_BASE   ; R14 = Pointer to array of opened src. handles
mov r13,IPB_FILE_COUNT         ; R13 = Files counter
;--- Cycle for write files ---
.CopyFiles:
xor ebp,ebp
xor r15,r15
.ReadWriteBlock:
;--- Read one block from source file ---
mov rbx,OPB_BUFFER_BASE
.ReadCopyBlock:
mov rcx,[r12]              ; RCX = Parm#1 = Target file handle
mov rdx,rbx                ; RDX = Parm#2 = Pointer to buffer
mov r8,OPB_BUFFER_SIZE     ; R8  = Parm#3 = Block size
lea r9,PARM_6              ; R9  = Parm#4 = Pointer for size return
mov PARM_5,0               ; Parm#5 = Pointer to overlapped, not used
call [ReadFile]
test rax,rax
mov eax,STEP_READ 
jz ErrorFileContext
mov rax,PARM_6
test rax,rax
jz .ReadCopyDone           ; Go done if zero size returned
js .ReadCopyDone           ; Go done if negative size returned
;--- Cycle for blocks READ ---
add rbx,rax
add r15,rax
cmp r15,IPB_BLOCK_SIZE
jb .ReadCopyBlock
.ReadCopyDone:
;--- Write one block to destination file ---
xor r15,r15
mov rbx,OPB_BUFFER_BASE
.WriteCopyBlock:
mov rcx,[r14]              ; RCX = Parm#1 = Target file handle
mov rdx,rbx                ; RDX = Parm#2 = Pointer to buffer
mov r8,OPB_BUFFER_SIZE     ; R8  = Parm#3 = Block size
lea r9,PARM_6              ; R9  = Parm#4 = Pointer for size return
mov PARM_5,0               ; Parm#5 = Pointer to overlapped, not used
call [WriteFile]
test rax,rax
mov eax,STEP_WRITE 
jz ErrorFileContext
mov rax,PARM_6             ; RAX = Return executed write(copy) size
test rax,rax
jz .WriteCopyDone          ; Go done if zero size returned
js .WriteCopyDone          ; Go done if negative size returned
add OPB_TOTAL_COPY,rax     ; Support total copy size for code self-check
;--- Cycle for blocks WRITE ---
add rbx,rax
add r15,rax
cmp r15,IPB_BLOCK_SIZE
jb .WriteCopyBlock
.WriteCopyDone:
;--- Cycle for blocks COPY ---
add rbp,IPB_BLOCK_SIZE
cmp rbp,IPB_REQUEST_SIZE
jb .ReadWriteBlock  
;--- Re-positioning to SOURCE file begin ---
mov rcx,[r12]           ; RCX = Parm#1 = Target file handle
xor edx,edx             ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d             ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d             ; R9  = Parm#4 = Move method = FILE_BEGIN
call [SetFilePointer]
cmp rax,INVALID_SET_FILE_POINTER
mov eax,STEP_READ 
je ErrorFileContext
;--- Re-positioning to DESTINATION file begin ---
mov rcx,[r14]           ; RCX = Parm#1 = Target file handle
xor edx,edx             ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d             ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d             ; R9  = Parm#4 = Move method = FILE_BEGIN
call [SetFilePointer]
cmp rax,INVALID_SET_FILE_POINTER
mov eax,STEP_WRITE 
je ErrorFileContext
;--- Count files in the sequence ---
add r12,8
add r14,8
dec r13
jnz .CopyFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec OPB_TRANSIT_MEASURE
jnz .CopyMeasurement
;--- Get timer at etop of copy files sequence ---
lea rcx,OPB_TRANSIT_T2
call [GetSystemTimeAsFileTime]
call HelperDeltaTime
mov OPB_TIMER_COPY,rax

;========== Source Files Read phase ===========================================;

;--- Initializing cycle for read files ---
mov rbp,OPB_BUFFER_SIZE
mov r13,IPB_ITERATIONS        ; R13 = Measurement iterations 
;--- Get timer at start of read files sequence ---
lea rcx,OPB_TRANSIT_T1
call [GetSystemTimeAsFileTime]
;--- Measurement iterationa ---
.ReadMeasurement:
mov r14,IPB_FILE_COUNT         ; R14 = Files counter
mov r12,OPB_HANDLES_SRC_BASE   ; R12 = Pointer to array of opened files handles
;--- Cycle for read files ---
.ReadFiles:
xor r15,r15
mov rbx,OPB_BUFFER_BASE
;--- Read file ---
.ReadBlock:
mov rcx,[r12]                  ; RCX = Parm#1 = Target file handle
mov rdx,rbx                    ; RDX = Parm#2 = Pointer to buffer
mov r8,rbp                     ; R8  = Parm#3 = Block size
lea r9,PARM_6                  ; R9  = Parm#4 = Pointer for size return
mov PARM_5,0                   ; Parm#5 = Pointer to overlapped, not used
call [ReadFile]
test rax,rax
mov eax,STEP_READ 
jz ErrorFileContext
mov rax,PARM_6                 ; RAX = Return executed read size
test rax,rax
jz .ReadDone                   ; Go done if zero size returned
js .ReadDone                   ; Go done if negative size returned
add OPB_TOTAL_READ,rax         ; Support total read size for code self-check
;--- Cycle for blocks ---
add r15,rax
cmp r15,IPB_REQUEST_SIZE
jb .ReadBlock
;--- End measurement cycle, get and store stop timer value ---
.ReadDone:
;--- Re-positioning to file begin ---
mov rcx,[r12]                    ; RCX = Parm#1 = Target file handle
xor edx,edx                    ; RDX = Parm#2 = Distance Low = 0
xor r8d,r8d                    ; R8  = Parm#3 = Distance High = 0
xor r9d,r9d                    ; R9  = Parm#4 = Move method = FILE_BEGIN
call [SetFilePointer]
cmp rax,INVALID_SET_FILE_POINTER
mov eax,STEP_READ 
je ErrorFileContext
;--- Count files in the sequence ---
add r12,8
dec r14
jnz .ReadFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec r13
jnz .ReadMeasurement
;--- Get timer at etop of read files sequence ---
lea rcx,OPB_TRANSIT_T2
call [GetSystemTimeAsFileTime]
call HelperDeltaTime
mov OPB_TIMER_READ,rax

;========== Source Files Delete phase =========================================;

;--- Delete source files ---
mov rbx,OPB_HANDLES_SRC_BASE   ; RBX = Pointer to handles list
xor ebp,ebp                    ; EBP = Files counter
.DeleteSrc:
mov rcx,[rbx]                  ; RCX    = Parm#1 = Handle
call [CloseHandle]
test rax,rax
jz @f
lea rcx,IPB_SRC_PATH_PREFIX
lea rdx,IPB_SRC_PATH_POSTFIX
lea r8,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
lea rcx,OPB_COMPLEX_PATH      ; RCX    = Parm#1 = Pointer to file path
call [DeleteFileA]
test rax,rax
@@:
mov eax,STEP_DELETE 
jz ErrorFileContext
add rbx,8
inc ebp
cmp rbp,IPB_FILE_COUNT         ; RBX = Files counter
jb .DeleteSrc
;--- Delete destination files ---
mov rbx,OPB_HANDLES_DST_BASE   ; RBX = Pointer to handles list
xor ebp,ebp                    ; EBP = Files counter
.DeleteDst:
mov rcx,[rbx]                  ; RCX    = Parm#1 = Handle
call [CloseHandle]
test rax,rax
jz @f
lea rcx,IPB_DST_PATH_PREFIX
lea rdx,IPB_DST_PATH_POSTFIX
lea r8,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
lea rcx,OPB_COMPLEX_PATH      ; RCX    = Parm#1 = Pointer to file path
call [DeleteFileA]
test rax,rax
@@:
mov eax,STEP_DELETE 
jz ErrorFileContext
add rbx,8
inc ebp
cmp rbp,IPB_FILE_COUNT         ; RBX = Files counter
jb .DeleteDst

;--- Done, restore RSP, pop registers, return ---
jmp ExitFileContext



