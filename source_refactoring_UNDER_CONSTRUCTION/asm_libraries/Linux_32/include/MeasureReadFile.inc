;---------- Read file with measurement iterations -----------------------------;
;                                                                              ;
; Input:   ESI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          EDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  EAX = JNI Status: 0=Error, 1=Linux32 JNI OK                         ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O allocated by Java Code,                           ;
; base address of this buffer = OPB+4096                                       ;
; size of this buffer = I/O block size + 4096, for alignment reservation       ;  
; For Read: source file parameters used, destination file parameters ignored   ;
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
; DWORD [IPB+00] = Required function ID                                        ;
; DWORD [IPB+04] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; DWORD [IPB+12] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; DWORD [IPB+20] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+24] = Source file performance attributes,                         ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; DWORD [IPB+28] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+32] = Destination file performance attributes,                    ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; DWORD [IPB+36] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+40] = Number of measurement repeats iterations                    ;
; DWORD [IPB+44] = Reserved for x64 layout compatibility                       ;
; Max. 976 BYTES [IPB+48] = Reserved                                           ; 
; 1024 BYTES [IPB+1024] = Source file path string, null terminated             ;
; 1024 BYTES [IPB+2048] = Destination file path string, null terminated        ;
; 1024 BYTES [IPB+3072] = Reserved                                             ;  
;                                                                              ;
; OPB layout (4096 bytes):                                                     ;
; DWORD [OPB+00] = Memory buffer base address, available after alignment       ;
; DWORD [OPB+04] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+08] = Memory buffer size, bytes, available after alignment        ;       
; DWORD [OPB+12] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+16] = Source file handle, available after open file               ;
; DWORD [OPB+20] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+24] = Destination file handle, available after open file          ;
; DWORD [OPB+28] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+32] = Executed read/write/copy operation size,                    ; 
;                  available after operation execute                           ;
;                  This is QWORD even for 32-bit version                       ;
; QWORD [OPB+40] = Timer delta for measurement repeats, units = 1 ns           ;
; DWORD [OPB+48] = Last called OS API status, see Linux Sys Call Reference     ;
; DWORD [OPB+52] = Reserved for x64 layout compatibility                       ;
; QWORD [OPB+56] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
; DWORD [OPB+60] = Reserved for x64 layout compatibility                       ;
; 4024 BYTES [OPB+72] = Reserved                                               ;
;                                                                              ;
;------------------------------------------------------------------------------;
MeasureReadFile:
push ebx esi edi ebp

;--- Clear output variables ---
push edi
cld
xor eax,eax
mov ecx,OPB_CLEAR_DWORDS
rep stosd
pop edi

;--- Calculate aligned base and size for memory buffer ---
lea eax,OPB_DATA_ARRAY
test eax,0FFFh
jz @f
and eax,0FFFFF000h
add eax,000001000h
@@:
mov OPB_BUFFER_BASE,eax     ; Setup Work buffer base
mov eax,IPB_BLOCK_SIZE
mov OPB_BUFFER_SIZE,eax     ; Setup Work buffer size

;--- Open source file ---
push esi edi
lea ebx,IPB_SRC_PATH          ; EBX = Parm#1 = Pointer to source file path
mov ecx,FILE_FLAGS_BLANK      ; ECX = Parm#2 = File operation mode flags
test IPB_SRC_ATTRIBUTES,00000011b
jz @f
or ecx,FILE_FLAG_O_DIRECT
@@:
mov edx,FILE_SHARE_MODE       ; EDX = Parm#3 = File access read-write mode flags
mov eax,5                     ; EAX = Function code = SYS_OPEN
int 80h
pop edi esi
test eax,eax
jz ErrorFileContext
cmp eax,7FFFFFFFh
ja ErrorFileContext
mov OPB_SRC_HANDLE,eax

;--- Get and store time at file READ start ---
push esi edi
xor ebx,ebx                   ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T1        ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorRead

;--- Measurement cycle for READ ---
mov eax,IPB_ITERATIONS        ; Setup measurement counter
mov OPB_TRANSIT_REPEATS,eax

;--- Start sequence of blocks cycle for READ ---
.ReadMeasurement:
xor eax,eax                   ; Setup total size accumulator
mov OPB_TRANSIT_SIZE_L,eax
mov OPB_TRANSIT_SIZE_H,eax

;--- Read block of source file ---
.ReadBlock:
mov ecx,OPB_BUFFER_BASE    ; ECX = Parm#2 = Pointer to buffer
mov edx,OPB_BUFFER_SIZE    ; EDX = Parm#3 = Buffer size, byte count per call

;--- Read sub-block of source file ---
.ReadSubBlock:
push ecx edx esi edi
mov ebx,OPB_SRC_HANDLE     ; EBX = Parm#1 = File descriptor, handle
mov eax,3                  ; EAX = Function code = Read file
int 80h
pop edi esi edx ecx
test eax,eax               ; Check EAX = Status
jz ErrorRead               ; Go if error write file, length = 0
js ErrorRead               ; Go if error write file, length < 0, status

;--- Cycle for sub-blocks ---
add ecx,eax                ; Advance ECX = pointer by returned size
sub edx,eax                ; Subtract returned size from EDX = total block size
jnz .ReadSubBlock          ; Repeat if block size remain

;--- Cycle for blocks, write file as sequence of blocks ---
mov eax,OPB_BUFFER_SIZE
add OPB_TRANSIT_SIZE_L,eax
adc OPB_TRANSIT_SIZE_H,0
jnz .DoneReadBlock
mov eax,OPB_TRANSIT_SIZE_L
cmp eax,IPB_REQUEST_SIZE
jb .ReadBlock
.DoneReadBlock:

;--- Reset file pointer for repeats, to position = 0 ---
push esi edi
mov ebx,OPB_SRC_HANDLE     ; EBX = Parm#1 = File descriptor, handle
xor ecx,ecx                ; ECX = Parm#2 = File offset = 0
xor edx,edx                ; EDX = Parm#3 = Whence, 0 means SEEK_SET = from file begin
mov eax,19                 ; EAX = Function code = seek
int 80h
pop edi esi
test eax,eax               ; Check EAX = Status
jnz ErrorRead              ; Go if error seek file

;--- Cycle for measurement iterations ---
dec OPB_TRANSIT_REPEATS
jnz .ReadMeasurement

;--- Get and store time at file READ end ---
push esi edi
xor ebx,ebx                ; EBX = Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,OPB_TRANSIT_T2     ; ECX = Parm#2 = Pointer to output DQ sec, ns
mov eax,265                ; EAX = Function code = SYS_GETTIME
int 80h
pop edi esi
test eax,eax
jnz ErrorRead

;--- Calculate delta-time in nanoseconds ---
mov eax,OPB_TRANSIT_T2_S
sub eax,OPB_TRANSIT_T1_S       ; EAX = Delta seconds
mov ebx,1000000000             ; EBX = Nanoseconds per second
mul ebx                        ; EDX:EAX = Delta nanoseconds, addend #1 by seconds
xchg ebx,eax
mov ecx,edx                    ; ECX:EBX = Delta nanoseconds, addend #1 by seconds
mov eax,OPB_TRANSIT_T2_NS
sub eax,OPB_TRANSIT_T1_NS      ; EDX:EAX = Delta nanoseconds, addend #2 by nanoseconds
cdq
add eax,ebx
adc edx,ecx                   ; EDX:EAX = sum of addends #1, #2, total delta in nanoseconds 
mov OPB_TIMER_DELTA_L,eax
mov OPB_TIMER_DELTA_H,edx

;--- Close file ---
mov ebx,OPB_SRC_HANDLE      ; EBX = Parm#1 = Source file descriptor, handle
mov eax,6                   ; EAX = Function code = SYS_CLOSE
int 80h
mov ecx,STEP_CLOSE
test eax,eax
jnz ErrorFileContext

;--- Restore stack after alignment ---
ExitFileContext:
;--- Exit ---
pop ebp edi esi ebx
ret
;--- Errors handling ---
ErrorFileContext:
mov OPB_LAST_OPERATION,ecx
;- call [GetLastError]
mov OPB_LAST_ERROR,eax
jmp ExitFileContext

;--- Error handling points ---
ErrorRead:
mov ecx,STEP_READ
jmp ErrorFileContext


