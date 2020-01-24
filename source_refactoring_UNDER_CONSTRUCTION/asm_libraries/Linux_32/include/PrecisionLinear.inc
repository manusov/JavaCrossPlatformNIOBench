;---------- Write, Copy, Read sequence of files with measurement iterations ---;
; Note legacy NIOBench scenario with all 3 operations (write, copy, read)      ;
; per 1 file at iteration can be supported with fileCount = 1                  ;
; This optimal when insufficiient disk space, but can reduce speed             ;
; compare with sequence: Write-All , Copy-All , Read - All.                    ;
;                                                                              ;
; Input:   ESI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          EDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  EAX = JNI Status: 0=Error, 2=Win64 JNI OK                           ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O and files handles allocated by Java Code,         ;
; base address of file IO buffer = OPB + 4096                                  ;
; size of files handles buffer = fileCount * 8,                                ;
; base address of files handles buffer = OPB + 4096 + fileCount * 8            ;
; size of file I/O buffer = I/O block size + 4096, for alignment reservation   ;
; use 8 bytes per handle for x64 layout compatibility,                         ; 
; but for this ia32 code, high dword = 0                                       ;   
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
;                                                                              ;
; DWORD [IPB+00] = Required function ID                                        ;
; DWORD [IPB+04] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; DWORD [IPB+12] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; DWORD [IPB+20] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+24] = Source file performance attributes,                         ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; DWORD [IPB+28] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+32] = Destination file performance attributes,                    ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; DWORD [IPB+36] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+40] = Number of measurement repeats iterations                    ;
; DWORD [IPB+44] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+48] = Number of files per series,                                 ; 
;                  note 1 file per series means legacy NIOBench scenario       ;
; DWORD [IPB+52] = Reserved for x64 layout compatibility                       ; 
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
; DWORD [OPB+00] = Memory buffer base address, available after alignment       ;
; DWORD [OPB+04] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+08] = Memory buffer size, bytes, available after alignment        ;
; DWORD [OPB+12] = Reserved for x64 layout compatibility                       ;       
; DWORD [OPB+16] = Source file handle, available after open file               ;
; DWORD [OPB+20] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+24] = Destination file handle, available after open file          ;
; DWORD [OPB+28] = Reserved for x64 layout compatibility                       ;
; QWORD [OPB+32] = Executed read/write/copy operation size,                    ; 
;                  available after operation execute                           ;
; QWORD [OPB+40] = Timer delta for measurement repeats, units = 1 ns           ;
;                  For multi-operations this value used as last                ;
;                  operation result, see separate values for Read, Write, Copy ;
; DWORD [OPB+48] = Last called OS API status, see Linux Sys Call Reference     ;
; DWORD [OPB+52] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+56] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
; DWORD [OPB+60] = Reserved for x64 layout compatibility                       ;
;                                                                              ;
; DATA AREA from [OPB+64] to [OPB+127] can be used for                         ; 
; OS-scefific undocumented scratch pads                                        ;                                                                   
; Use OS-specific scratch pad at OPB[80-95], 16 bytes (for timer API)          ;
; Use OS-specific measurement counter at OPB[96-104], 8 bytes                  ;
; OS-specific additions possible to this DATA AREA usage                       ; 
;                                                                              ;
; Special layout for multi operation (Read, Write, Copy) functions:            ;
; DWORD [OPB+128] = Memory buffer base address for store source files handles  ;
;                   8 bytes per handle, buffer size = file count * 8           ;
; DWORD [OPB+132] = Reserved for x64 layout compatibility                      ;
; QWORD [OPB+136] = Memory buf. base addr. for store destination files handles ;
;                   8 bytes per handle, buffer size = file count * 8           ;
; DWORD [OPB+140] = Reserved for x64 layout compatibility                      ;
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
push ebx esi edi ebp

;--- Clear output variables ---
call HelperClearOPB

;--- Calculate aligned base and size for memory buffer ---
call HelperDTA       ; Setup Work buffer base and Work buffer size

;========== Source and Destination Files Create phase =========================;

;--- Initializing cycle for creating source files ---
add eax,ecx                   ; EAX = Pointer to files handles buffer
mov OPB_HANDLES_SRC_BASE,eax
imul ecx,IPB_FILE_COUNT,8
add eax,ecx
mov OPB_HANDLES_DST_BASE,eax
mov ebx,OPB_HANDLES_SRC_BASE  ; EBX = Pointer for write handles list
xor ebp,ebp                   ; EBP = Counter for number of files
;--- Cycle for creating source files ---  
.CreateSrcFiles:
push ebx
lea ecx,IPB_SRC_PATH_PREFIX
lea edx,IPB_SRC_PATH_POSTFIX
lea ebx,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
call HelperCreateSrcGroup     ; Return EAX = file handle or status
pop ebx
mov [ebx],eax                 ; Store handle
mov dword [ebx+4],0
test eax,eax                  ; EAX = 0 if typical errors detected
mov eax,STEP_OPEN_WRITE 
jz ErrorFileContext 
add ebx,8
inc ebp
cmp ebp,IPB_FILE_COUNT
jb .CreateSrcFiles
;--- Initializing cycle for creating destination files ---
mov ebx,OPB_HANDLES_DST_BASE  ; EBX = Pointer for write handles list
xor ebp,ebp                   ; EBP = Counter for number of files
;--- Cycle for creating destination files ---
.CreateDstFiles:
push ebx
lea ecx,IPB_DST_PATH_PREFIX
lea edx,IPB_DST_PATH_POSTFIX
lea ebx,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
call HelperCreateDstGroup     ; Return EAX = file handle or status
pop ebx
mov [ebx],eax                 ; Store handle
mov dword [ebx+4],0
test eax,eax                  ; EAX = 0 if typical errors detected
mov eax,STEP_OPEN_WRITE 
jz ErrorFileContext 
add ebx,8
inc ebp
cmp ebp,IPB_FILE_COUNT
jb .CreateDstFiles

;========== Source Files Write phase ==========================================;

;--- Initializing cycle for write files ---
mov eax,IPB_ITERATIONS        
mov OPB_TRANSIT_REPEATS,eax   ; Counter for Measurement iterations
;--- Get timer at start of write files sequence ---
push esi edi
xor ebx,ebx               ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T1    ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265               ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorWrite
;--- Measurement iterations ---
.WriteMeasurement:
xor ebp,ebp                     ; EBP = Files counter
;--- Cycle for write files ---
.WriteFiles:
mov OPB_TRANSIT_BLOCK,0
;--- Write file ---
.WriteBlock:
push esi edi ebx ebp
imul eax,ebp,8
add eax,OPB_HANDLES_SRC_BASE
mov ebx,[eax]              ; EBX = Parm#1 = File descriptor, handle
mov ecx,OPB_BUFFER_BASE    ; ECX = Parm#2 = Pointer to buffer
mov edx,OPB_BUFFER_SIZE    ; EDX = Parm#3 = Buffer size, byte count per call
mov eax,4                  ; EAX = Function code = Write to file
int 80h
pop ebp ebx edi esi
test eax,eax               ; Check EAX = Status
jz ErrorWrite              ; Go if error write file, length = 0
js ErrorWrite              ; Go if error write file, length < 0, status
add OPB_TOTAL_WRITE_L,eax  ; Support total write size for code self-check
adc OPB_TOTAL_WRITE_H,0 
;--- Cycle for blocks ---
add OPB_TRANSIT_BLOCK,eax
mov eax,IPB_REQUEST_SIZE
cmp OPB_TRANSIT_BLOCK,eax
jb .WriteBlock
;--- End measurement cycle, get and store stop timer value ---
.WriteDone:
;--- Re-positioning to source file begin ---
push esi edi ebx ebp
imul eax,ebp,8
add eax,OPB_HANDLES_SRC_BASE
mov ebx,[eax]       ; EBX = Parm#1 = File descriptor, handle
xor ecx,ecx         ; ECX = Parm#2 = File offset = 0
xor edx,edx         ; EDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,19          ; EAX = Function code = seek
int 80h
pop ebp ebx edi esi
test eax,eax        ; Check EAX = Status
jnz ErrorWrite      ; Go if error seek file
;--- Count files in the sequence ---
inc ebp
cmp ebp,IPB_FILE_COUNT
jb .WriteFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec OPB_TRANSIT_REPEATS
jnz .WriteMeasurement
;--- Get timer at etop of write files sequence ---
push esi edi
xor ebx,ebx               ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T2    ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265               ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorWrite
call HelperDeltaTime
mov OPB_TIMER_WRITE_L,eax
mov OPB_TIMER_WRITE_H,edx

;========== Source to Destination Files Copy phase ============================;
; Note file pointers re-positioned to files begin by previous sysles

;--- Initializing cycle for copy files ---
mov eax,IPB_ITERATIONS         ; Measurement iterations
mov OPB_TRANSIT_REPEATS,eax
;--- Get timer at start of copy files sequence ---
push esi edi
xor ebx,ebx               ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T1    ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265               ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorWrite
;--- Measurement iteration ---
.CopyMeasurement:
xor ebp,ebp                 ; EBP = Files counter
;--- Cycle for copy files ---
.CopyFiles:
mov OPB_TRANSIT_SIZE,0
.ReadWriteBlock:
;--- Read one block from source file ---
mov OPB_TRANSIT_BLOCK,0     ; Block size accumulator
.ReadCopyBlock:
push esi edi ebp
imul eax,ebp,8
add eax,OPB_HANDLES_SRC_BASE
mov ebx,[eax]              ; EBX = Parm#1 = File descriptor, handle
mov ecx,OPB_BUFFER_BASE    ; ECX = Parm#2 = Pointer to buffer
mov edx,OPB_BUFFER_SIZE    ; EDX = Parm#3 = Buffer size, byte count per call
mov eax,3                  ; EAX = Function code = Read file
int 80h
pop ebp edi esi
test eax,eax               ; Check EAX = Status
jz ErrorRead               ; Go if error read file, length = 0
js ErrorRead               ; Go if error read file, length < 0, status
;--- Cycle for blocks READ as part of COPY ---
add OPB_TRANSIT_BLOCK,eax
mov eax,IPB_BLOCK_SIZE
cmp OPB_TRANSIT_BLOCK,eax
jb .ReadCopyBlock
.ReadCopyDone:
;--- Write one block to destination file ---
mov OPB_TRANSIT_BLOCK,0
.WriteCopyBlock:
push esi edi ebp
imul eax,ebp,8
add eax,OPB_HANDLES_DST_BASE
mov ebx,[eax]       ; EBX = Parm#1 = File descriptor, handle
mov ecx,OPB_BUFFER_BASE    ; ECX = Parm#2 = Pointer to buffer
mov edx,OPB_BUFFER_SIZE    ; EDX = Parm#3 = Buffer size, byte count per call
mov eax,4                  ; EAX = Function code = Write to file
int 80h
pop ebp edi esi
test eax,eax        ; Check EAX = Status
jz ErrorWrite              ; Go if error write file, length = 0
js ErrorWrite              ; Go if error write file, length < 0, status
;--- Cycle for blocks WRITE as part of COPY ---
add OPB_TOTAL_COPY_L,eax    ; Support total write size for code self-check
adc OPB_TOTAL_COPY_H,0 
add OPB_TRANSIT_BLOCK,eax
mov eax,IPB_BLOCK_SIZE
cmp OPB_TRANSIT_BLOCK,eax
jb .WriteCopyBlock
.WriteCopyDone:
;--- Cycle for blocks COPY ---
mov eax,IPB_BLOCK_SIZE
add OPB_TRANSIT_SIZE,eax
mov eax,IPB_REQUEST_SIZE
cmp OPB_TRANSIT_SIZE,eax 
jb .ReadWriteBlock  
;--- Re-positioning to source file begin ---
push esi edi ebp
imul eax,ebp,8
add eax,OPB_HANDLES_SRC_BASE
mov ebx,[eax]       ; EBX = Parm#1 = File descriptor, handle
xor ecx,ecx         ; ECX = Parm#2 = File offset = 0
xor edx,edx         ; EDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,19          ; EAX = Function code = seek
int 80h
pop ebp edi esi
test eax,eax        ; Check EAX = Status
jnz ErrorRead       ; Go if error seek file
;--- Re-positioning to destination file begin ---
push esi edi ebp
imul eax,ebp,8
add eax,OPB_HANDLES_DST_BASE
mov ebx,[eax]       ; EBX = Parm#1 = File descriptor, handle
xor ecx,ecx         ; ECX = Parm#2 = File offset = 0
xor edx,edx         ; EDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,19          ; EAX = Function code = seek
int 80h
pop ebp edi esi
test eax,eax        ; Check EAX = Status
jnz ErrorWrite      ; Go if error seek file
;--- Count files in the sequence ---
inc ebp
cmp ebp,IPB_FILE_COUNT
jb .CopyFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec OPB_TRANSIT_REPEATS
jnz .CopyMeasurement
;--- Get timer at etop of copy files sequence ---
push esi edi
xor ebx,ebx               ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T2    ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265               ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorWrite
call HelperDeltaTime
mov OPB_TIMER_COPY_L,eax
mov OPB_TIMER_COPY_H,edx

;========== Source Files Read phase ===========================================;

;--- Initializing cycle for read files ---
mov eax,IPB_ITERATIONS        
mov OPB_TRANSIT_REPEATS,eax   ; Counter for Measurement iterations
;--- Get timer at start of read files sequence ---
push esi edi
xor ebx,ebx               ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T1    ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265               ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorRead
;--- Measurement iterations ---
.ReadMeasurement:
xor ebp,ebp                     ; EBP = Files counter
;--- Cycle for read files ---
.ReadFiles:
mov OPB_TRANSIT_BLOCK,0
;--- Read file ---
.ReadBlock:
push esi edi ebx ebp
imul eax,ebp,8
add eax,OPB_HANDLES_SRC_BASE
mov ebx,[eax]              ; EBX = Parm#1 = File descriptor, handle
mov ecx,OPB_BUFFER_BASE    ; ECX = Parm#2 = Pointer to buffer
mov edx,OPB_BUFFER_SIZE    ; EDX = Parm#3 = Buffer size, byte count per call
mov eax,3                  ; EAX = Function code = Read file
int 80h
pop ebp ebx edi esi
test eax,eax               ; Check EAX = Status
jz ErrorRead               ; Go if error read file, length = 0
js ErrorRead               ; Go if error read file, length < 0, status
add OPB_TOTAL_READ_L,eax   ; Support total read size for code self-check
adc OPB_TOTAL_READ_H,0 
;--- Cycle for blocks ---
add OPB_TRANSIT_BLOCK,eax
mov eax,IPB_REQUEST_SIZE
cmp OPB_TRANSIT_BLOCK,eax
jb .ReadBlock
;--- End measurement cycle, get and store stop timer value ---
.ReadDone:
;--- Re-positioning to source file begin ---
push esi edi ebx ebp
imul eax,ebp,8
add eax,OPB_HANDLES_SRC_BASE
mov ebx,[eax]       ; EBX = Parm#1 = File descriptor, handle
xor ecx,ecx         ; ECX = Parm#2 = File offset = 0
xor edx,edx         ; EDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,19          ; EAX = Function code = seek
int 80h
pop ebp ebx edi esi
test eax,eax        ; Check EAX = Status
jnz ErrorRead      ; Go if error seek file
;--- Count files in the sequence ---
inc ebp
cmp ebp,IPB_FILE_COUNT
jb .ReadFiles
;--- Count measurement iterations, set total read size include meas. cycle ---
dec OPB_TRANSIT_REPEATS
jnz .ReadMeasurement
;--- Get timer at etop of read files sequence ---
push esi edi
xor ebx,ebx               ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T2    ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265               ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorRead
call HelperDeltaTime
mov OPB_TIMER_READ_L,eax
mov OPB_TIMER_READ_H,edx

;========== Source and Destination Files Delete phase =========================;

; ;--- Delete source files ---
; mov ebx,OPB_HANDLES_SRC_BASE   ; EBX = Pointer to handles list
; xor ebp,ebp                    ; EBP = Files counter
; .DeleteSrc:
; push esi edi ebx ebp
; mov ebx,[ebx]      ; EBX = Parm#1 = Source file descriptor, handle
; mov eax,6          ; EAX = Function code = SYS_CLOSE
; int 80h
; pop ebp ebx edi esi
; test eax,eax
; ;- jnz @f  ; BUG, ebx corrupted at next string 3
; lea ecx,IPB_SRC_PATH_PREFIX
; lea edx,IPB_SRC_PATH_POSTFIX
; lea ebx,OPB_COMPLEX_PATH
; mov eax,ebp
; call HelperComplexPath
; push esi edi ebx ebp
; lea ebx,OPB_COMPLEX_PATH    ; EBX = Parm#1 = Pointer to source file path
; mov eax,10                  ; EAX = Function code = SYS_UNLINK
; int 80h
; pop ebp ebx edi esi
; @@:
; test eax,eax
; mov eax,STEP_DELETE
; jnz ErrorFileContext
; add ebx,8
; inc ebp
; cmp ebp,IPB_FILE_COUNT       ; EBP = Files counter
; jb .DeleteSrc
; ;--- Delete destination files ---
; mov ebx,OPB_HANDLES_DST_BASE   ; EBX = Pointer to handles list
; xor ebp,ebp                    ; EBP = Files counter
; .DeleteDst:
; push esi edi ebx ebp
; mov ebx,[ebx]      ; EBX = Parm#1 = Source file descriptor, handle
; mov eax,6          ; EAX = Function code = SYS_CLOSE
; int 80h
; pop ebp ebx edi esi
; test eax,eax
; ;- jnz @f  ; BUG, ebx corrupted at next string 3
; lea ecx,IPB_DST_PATH_PREFIX
; lea edx,IPB_DST_PATH_POSTFIX
; lea ebx,OPB_COMPLEX_PATH
; mov eax,ebp
; call HelperComplexPath
; push esi edi ebx ebp
; lea ebx,OPB_COMPLEX_PATH    ; EBX = Parm#1 = Pointer to source file path
; mov eax,10                  ; EAX = Function code = SYS_UNLINK
; int 80h
; pop ebp ebx edi esi 
; @@:
; test eax,eax
; mov eax,STEP_DELETE
; jnz ErrorFileContext
; add ebx,8
; inc ebp
; cmp ebp,IPB_FILE_COUNT       ; EBP = Files counter
; jb .DeleteDst
;


;--- Delete source files ---
mov ebx,OPB_HANDLES_SRC_BASE   ; EBX = Pointer to handles list
xor ebp,ebp                    ; EBP = Files counter
.DeleteSrc:
push esi edi ebx ebp
mov ebx,[ebx]      ; EBX = Parm#1 = Source file descriptor, handle
mov eax,6          ; EAX = Function code = SYS_CLOSE
int 80h
pop ebp ebx edi esi
test eax,eax
jnz @f
push esi edi ebx ebp
lea ecx,IPB_SRC_PATH_PREFIX
lea edx,IPB_SRC_PATH_POSTFIX
lea ebx,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
lea ebx,OPB_COMPLEX_PATH    ; EBX = Parm#1 = Pointer to source file path
mov eax,10                  ; EAX = Function code = SYS_UNLINK
int 80h
pop ebp ebx edi esi
@@:
test eax,eax
mov eax,STEP_DELETE
jnz ErrorFileContext
add ebx,8
inc ebp
cmp ebp,IPB_FILE_COUNT       ; EBP = Files counter
jb .DeleteSrc
;--- Delete destination files ---
mov ebx,OPB_HANDLES_DST_BASE   ; EBX = Pointer to handles list
xor ebp,ebp                    ; EBP = Files counter
.DeleteDst:
push esi edi ebx ebp
mov ebx,[ebx]      ; EBX = Parm#1 = Source file descriptor, handle
mov eax,6          ; EAX = Function code = SYS_CLOSE
int 80h
pop ebp ebx edi esi
test eax,eax
jnz @f
push esi edi ebx ebp
lea ecx,IPB_DST_PATH_PREFIX
lea edx,IPB_DST_PATH_POSTFIX
lea ebx,OPB_COMPLEX_PATH
mov eax,ebp
call HelperComplexPath
lea ebx,OPB_COMPLEX_PATH    ; EBX = Parm#1 = Pointer to source file path
mov eax,10                  ; EAX = Function code = SYS_UNLINK
int 80h
pop ebp ebx edi esi 
@@:
test eax,eax
mov eax,STEP_DELETE
jnz ErrorFileContext
add ebx,8
inc ebp
cmp ebp,IPB_FILE_COUNT       ; EBP = Files counter
jb .DeleteDst


;--- Go to pop registers and done ---

jmp ExitFileContext 

