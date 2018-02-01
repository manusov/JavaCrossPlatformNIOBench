;==============================================================================;
;                                                                              ;
;     Disk Read and Write Benchmarks (engineering release). Win64 Edition.     ; 
;                           (C)2016 IC Book Labs.                              ;
;                                                                              ;
;  This file is main module: translation object, interconnecting all modules.  ;
;                 This revision rejects Read and Write cache.                  ;
;                                                                              ;
;        Translation by Flat Assembler version 1.71.49 (Dec 06, 2015)          ;
;           Visit http://flatassembler.net/ for more information.              ;
;           For right tabulations, please edit by FASM Editor 2.0              ;
;                                                                              ;
;==============================================================================;

;---------- Program sample setup variables ------------------------------------; 

BLOCK_SIZE       EQU  1024*1024*10                  ; Disk block size, bytes
BLOCK_COUNT      EQU  10                            ; Number of disk blocks
MEASURE_REPEATS  EQU  3                             ; Measurement repeats
NUMA_NODE        EQU  0                             ; NUMA node memory alloc.
CPU_AFFINITY     EQU  00                            ; Mask or 0=Skip
PATH_STRING      EQU  'myfile.tmp',0                ; Target file path
FILE_FLAGS       EQU  FILE_ATTRIBUTE_NORMAL     \
                      + FILE_FLAG_NO_BUFFERING  \
                      + FILE_FLAG_WRITE_THROUGH     ; File performance flags

;---------- FASM definitions --------------------------------------------------;

include 'win64a.inc'

;---------- Code section ------------------------------------------------------;

format PE64 GUI
entry start
section '.text' code readable executable
start:

;--- 32 byte parameters shadow and +8 alignment -------------------------------;

sub rsp,8*5

;--- (1) --- Check CPU features and measure CPU clock -------------------------;

lea r15,[Step01_MeasureClock]    ; R15 = Pointer to phase name for error report
mov r14d,1                       ; R14 = Status code for error report
call CheckCpuId
jc ErrorProgram                  ; Go if CPUID not supported or locked
inc r14d
cmp eax,1
jc ErrorProgram                  ; Go if CPUID function 1 not sup. or locked 
inc r14d
mov eax,1
cpuid
test dl,10h
jz ErrorProgram                  ; Go if TSC not supported
inc r14d
call MeasureCpuClk
jc ErrorProgram                  ; Go if TSC clock measurement error
inc r14d
test rax,rax
jz ErrorProgram                  ; Go if TSC error: frequency=0
test rdx,rdx
jz ErrorProgram                  ; Go if TSC error: period=0 
mov [TscFrequency],rax           ; Store TSC frequency, Hz
mov [TscPeriod],rdx              ; Store TSC period, ps

;--- (2) --- OS API detection -------------------------------------------------;

lea r15,[Step02_OsApiDetection]  ; R15 = Pointer to phase name for error report
lea rcx,[NameKernel32]           ; RCX = Parm#1 = Pointer to module name string
call [GetModuleHandle]           ; RAX = Return module handle
mov r14d,1
test rax,rax
jz ErrorProgram                  ; Go if KERNEL32.DLL not found
xchg rbx,rax
;--- Get WinAPI handler for NUMA-aware memory allocation --- 
mov rcx,rbx                      ; RCX = Parm#1 = Pointer to module handle
lea rdx,[NameVirtualAllocExNuma] ; RDX = Parm#2 = Pointer to function name
call [GetProcAddress]            ; RAX = Return function address
inc r14d
test rax,rax
jz ErrorProgram                  ; Go if function not found
mov [PVirtualAllocExNuma],rax
;--- Get WinAPI handler for set thread affinity mask ---
mov rcx,rbx
lea rdx,[NameSetThreadAffinityMask]
call [GetProcAddress]
inc r14d
test rax,rax
jz ErrorProgram
mov [PSetThreadAffinityMask],rax

;--- (3) --- NUMA-aware memory allocation  ------------------------------------;

lea r15,[Step03_MemoryAllocation]
xor r14d,r14d
call [GetCurrentProcess]         ; Return RAX = Handle
test rax,rax
jz ErrorProgram      ; Go if get handle error
;--- NUMA-aware memory allocation --- 
xchg rcx,rax         ; Parm#1 = RCX = Handle
mov eax,[BlockSize]  ; EAX = One block size
mov ebx,[BlockCount] ; EBX = Number of blocks
mul rbx              ; RAX = Total size
test eax,0FFFh
jz @f
and rax,0FFFFFFFFFFFFF000h
add rax,00000000000001000h  ; This for size alignment
@@:
lea r8,[rax+4096]   ; Parm#3 = R8  = Memory block size, with base align rsvd.
mov [MemorySize],r8
xor edx,edx			    ; Parm#2 = RDX = Base address, 0 = auto
mov r9,00003000h	  ; Parm#4 = R9  = Alloc. type: RESERVE(2000h) + COMMIT(1000h)
mov eax,[NumaNode]
push rax			      ; Parm#6 = NUMA node preferred = 0 , use RDX=0 for compact
pushq 04h			      ; Parm#5 = Pages protection attributes = Read/Write
sub rsp,32			    ; Create parameters shadow
call [PVirtualAllocExNuma] 
add rsp,32+16			  ; Remove parameters shadow + parameters 5,6
test rax,rax
jz ErrorProgram     ; Go if memory allocation error
;--- Store true base for release and aligned base for use memory ---
mov [TrueMemoryBase],rax
test eax,0FFFh
jz @f
and rax,0FFFFFFFFFFFFF000h
add rax,00000000000001000h  ; This for size alignment
@@:
mov [MemoryBase],rax

;--- (4) --- Memory blank -----------------------------------------------------;
; No error branches for this operation, error reporting is reserved

cld
mov rdi,[MemoryBase]         ; RDI = Block base
mov rcx,[MemorySize]         ; RCX = Block size, bytes count
shr rcx,3                    ; RCX = QWords count = Bytes count / 8
mov rax,'DATA    '           ; This data for fill buffer
rep stosq

;--- (5) --- Set affinity mask ------------------------------------------------;

lea r15,[Step05_SetAffinity]
mov rbx,[CpuAffinity]
test rbx,rbx
jz @f                  ; Skip affinitization if mask = 0
call [GetCurrentThread]
mov r14d,1
test rax,rax
jz ErrorProgram        ; Go if thread error 
xchg rcx,rax           ; RCX = Thread handle
mov rdx,rbx            ; RDX = Thread affinity mask
call [SetThreadAffinityMask]
inc r14d
test rax,rax           ; RAX = Old affinity mask
jz ErrorProgram        ; Go if affinity mask error 
mov [OrigAffinity],rax
@@:

;---------- Start measurement cycle for WRITE steps (6,7,8,9) -----------------;

;--- Clear variables ---
xor eax,eax            ; This 32-bit instruction clears entire 64-bit RAX
mov [ReadClocks],rax
mov [WriteClocks],rax
mov ebp,[MeasureRepeats]
;--- Start time point for file create-write ---
rdtsc
shl rdx,32
lea rdi,[rax+rdx]      ; RDI = TSC at file create-write start

;--- Measurement cycle begin for file create-write ----------------------------;  

WriteMeasureCycle:

;--- (W6) --- Create target file ----------------------------------------------;

lea r15,[Step06_OpenFile]
xor r14d,r14d
lea rcx,[FilePath]                     ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ OR GENERIC_WRITE  ; RDX = Parm#2 = Desired access
xor r8d,r8d                            ; R8  = Parm#3 = Share mode = 0
xor r9d,r9d                            ; R9  = Parm#4 = Security attributes = 0
push r9                                ; This for stack alignment
push r9                                ; Parm#7 = Template file handle, not used 
mov eax,[FileFlags]
push rax                               ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                     ; Parm#5 = Creation disposition
sub rsp,32
call [CreateFileA]
add rsp,32+32
test rax,rax                           ; Check RAX = Handle
jz ErrorProgram                        ; Go if error create file
xchg r13,rax                           ; R13 = Target file handle

;--- (W7) --- Write target file, cycle for sequence of blocks -----------------;

lea r15,[Step07_WriteFile]
mov esi,[BlockCount]
xor r12d,r12d
.WriteBlock:
mov rcx,r13                            ; RCX = Parm#1 = Target file handle
mov rdx,[MemoryBase]                   ; RDX = Parm#2 = Pointer to buffer
add rdx,r12
mov r8d,[BlockSize]                    ; R8  = Parm#3 = Block size
lea r9,[SizeReturn]                    ; R9  = Parm#4 = Pointer for size ret.
xor eax,eax
mov [r9],rax
push rax                               ; This for stack alignment
push rax                               ; Parm#5 = Pointer to overlapped
sub rsp,32
call [WriteFile]
add rsp,32+16
test rax,rax                           ; Check RAX = Status
jz ErrorProgram                        ; Go if error write file
mov eax,[BlockSize]
add r12,rax
dec esi
jnz .WriteBlock                        ; Cycle for blocks

;--- (W8) --- Read target file with time measurement --------------------------;
; This phase absent for WRITE cycle

;--- (W9) --- Close target file -----------------------------------------------;

lea r15,[Step09_CloseFile]
mov rcx,r13                              ; RCX = Parm#1 = File handle for close
call [CloseHandle]
test rax,rax
jz ErrorProgram                          ; Go if error close file

;---------- End measurement cycle for WRITE steps (6,7,8,9) -------------------;

dec ebp
jnz WriteMeasureCycle

;--- Stop time point for file create-write ---
rdtsc
shl rdx,32
add rax,rdx                            ; RAX = TSC at file write stop 
sub rax,rdi                            ; RAX = Delta TSC
mov [WriteClocks],rax                  ; Store Delta-TSC for WRITE operation

;---------- Start measurement cycle for READ steps (6,7,8,9) ------------------;

mov ebp,[MeasureRepeats]

;--- Start time point for file read ---
rdtsc
shl rdx,32
lea rdi,[rax+rdx]                      ; RDI = TSC at file read start

;--- Measurement cycle begin for file read ------------------------------------;

ReadMeasureCycle:

;--- (R6) --- Open or create target file --------------------------------------;

lea r15,[Step06_OpenFile]
xor r14d,r14d
lea rcx,[FilePath]                     ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ OR GENERIC_WRITE  ; RDX = Parm#2 = Desired access
xor r8d,r8d                            ; R8  = Parm#3 = Share mode = 0
xor r9d,r9d                            ; R9  = Parm#4 = Security attributes = 0
push r9                                ; This for stack alignment
push r9                                ; Parm#7 = Template file handle, not used 
mov eax,[FileFlags]
push rax                               ; Parm#6 = File attribute and flags
push OPEN_ALWAYS ; CREATE_ALWAYS       ; Parm#5 = Creation disposition
sub rsp,32
call [CreateFileA]
add rsp,32+32
test rax,rax                           ; Check RAX = Handle
jz ErrorProgram                        ; Go if error create file
xchg r13,rax                           ; R13 = Target file handle

;--- (R7) --- Write target file with time measurement -------------------------;
; This phase absent for READ cycle

;--- (R8) --- Read target file with time measurement --------------------------;

lea r15,[Step08_ReadFile]
mov esi,[BlockCount]
xor r12d,r12d
.ReadBlock:
mov rcx,r13                            ; RCX = Parm#1 = Target file handle
mov rdx,[MemoryBase]                   ; RDX = Parm#2 = Pointer to buffer
add rdx,r12
mov r8d,[BlockSize]                    ; R8  = Parm#3 = Block size
lea r9,[SizeReturn]                    ; R9  = Parm#4 = Pointer for size ret.
xor eax,eax
mov [r9],rax
push rax                               ; This for stack alignment
push rax                               ; Parm#5 = Pointer to overlapped
sub rsp,32
call [ReadFile]
add rsp,32+16
test rax,rax                           ; Check RAX = Status
jz ErrorProgram                        ; Go if error write file
mov eax,[BlockSize]
add r12,rax
dec esi
jnz .ReadBlock                         ; Cycle for blocks

;--- (R9) --- Close target file -----------------------------------------------;

lea r15,[Step09_CloseFile]
mov rcx,r13                              ; RCX = Parm#1 = File handle for close
call [CloseHandle]
test rax,rax
jz ErrorProgram                          ; Go if error close file

;---------- End measurement cycle for READ steps (6,7,8,9) --------------------;

dec ebp
jnz ReadMeasureCycle

;--- End time point for file read ---
rdtsc
shl rdx,32
add rax,rdx                            ; RAX = TSC at file write start 
sub rax,rdi                            ; RAX = Delta TSC
mov [ReadClocks],rax                   ; Store Delta-TSC for READ operation

;--- (10) --- Restore affinity ------------------------------------------------;

lea r15,[Step10_RestoreAffinity]
cmp [CpuAffinity],0
je @f                        ; Skip operation if affinity not changed
call [GetCurrentThread]
test rax,rax
jz ErrorProgram              ; Go if thread error 
xchg rcx,rax                 ; RCX = Thread handle
mov rdx,[OrigAffinity]       ; RDX = Thread affinity mask
call [SetThreadAffinityMask]
test rax,rax                 ; RAX = Old affinity mask
jz ErrorProgram              ; Go if affinity mask error 
@@:

;--- (11) --- Release memory --------------------------------------------------;

lea r15,[Step11_ReleaseMemory]
call [GetCurrentProcess]     ; Return RAX = Handle
test rax,rax
jz ErrorProgram              ; Go if handle error
xchg rcx,rax                 ; Parm#1 = RCX = Process Handle
mov rdx,[TrueMemoryBase]     ; Parm#2 = Address
xor r8d,r8d                  ; Parm#3 = R8 = Release size, 0 = Entire block
mov r9d,MEM_RELEASE	         ; Parm#4 = Release type
call [VirtualFreeEx]
test rax,rax
jz ErrorProgram              ; Go if function error

;--- (12) --- Calculate speed = F (dTSC, TotalSize) ---------------------------;

;--- Prepare block size and time constant --- 
;lea r15,[Step12_CalculateSpeed]
;mov eax,[BlockSize]
;mov ebx,[BlockCount]
;mov ecx,[MeasureRepeats]
;mul rbx
;mul rcx
;xchg rsi,rax                 ; RSI = Total size transferred, Bytes
;mov rdi,[TscPeriod]          ; RDI = TSC period, femtoseconds
;mov rbp,1000000000 * 10      ; RBP = Constant for fs to 10*us 
;mov r14d,1
;test rsi,rsi
;jz ErrorProgram              ; Go if size=0 error
;--- Calculations for Read ---
;mov rax,[ReadClocks]          ; RAX = Clocks per Read operation
;mul rdi                       ; RDX:RAX = Femtoseconds per Read Operation
;inc r14d
;cmp rdx,rbp
;jae ErrorProgram              ; Go if time interval overflow 
;div rbp
;xchg rbx,rax                  ; RBX = Time interval, units = 10*us, Read
;mov rax,rsi
;xor edx,edx
;div rbx                       ; RAX = Size[bytes] / Time[ 10*us ]
;mov [ReadBytesPerSecond],rax  ; Store READ speed, MBPS*10 
;--- Calculations for Write ---
;mov rax,[WriteClocks]          ; RAX = Clocks per Write operation
;mul rdi                        ; RDX:RAX = Femtoseconds per Read Operation
;inc r14d
;cmp rdx,rbp
;jae ErrorProgram               ; Go if time interval overflow 
;div rbp
;xchg rbx,rax                   ; RBX = Time interval, units = 10*us, Write
;mov rax,rsi
;xor edx,edx
;div rbx                        ; RAX = Size[bytes] / Time[ 10*us ]
;mov [WriteBytesPerSecond],rax  ; Store WRITE speed, MBPS*10
;---

;--- Detect x87 presence ---
lea r15,[Step12_CalculateSpeed]
mov r14d,1
mov eax,1               ; CPUID function 1, get base features
cpuid
test dl,00000001b
jz ErrorProgram         ; Go if x87 FPU absent
;--- Start calculate base variables by x87 FPU --- 
finit
fild [BlockSize]
fild [BlockCount]
fild [MeasureRepeats]
fmul st0,st1
fmul st0,st2            ; Load ST0 = Total size transferred, Bytes 
fild [TscPeriod]        ; Load ST0 = Period, femtoseconds
fild [Const1E10]        ; Load ST0 = Constant for fs to 10*us
;--- Base variables preloaded, after load Clocks ---
; ST3 = Total size transferred, Bytes 
; ST2 = Period, femtoseconds
; ST1 = Constant for fs to 10*us
; ST0 = Read or Write clocks, depend on step
;--- Calculations for Read ---
fild [ReadClocks]       ; ST0 = TSC clocks per target operation
fmul st0,st2            ; ST0 = Femtoseconds per target operation
fdiv st0,st1            ; ST0 = Same interval in units 10*us
fdivr st0,st3           ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [ReadBytesPerSecond]    ; Bytes per 10 uS , approx. MBPS*10
;--- Calculations for Write ---
fild [WriteClocks]      ; ST0 = TSC clocks per target operation
fmul st0,st2            ; ST0 = Femtoseconds per target operation
fdiv st0,st1            ; ST0 = Same interval in units 10*us
fdivr st0,st3           ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [WriteBytesPerSecond]   ; Bytes per 10 uS , approx. MBPS*10

;--- (13) --- Delete test file ------------------------------------------------;

lea r15,[Step13_DeleteFile]
xor r14d,r14d
lea rcx,[FilePath]           ; RCX = Parm#1 = Pointer to file path
call [DeleteFileA]
test rax,rax
jz ErrorProgram              ; Go if function error

;--- (14) --- Prepare results for visual --------------------------------------;   

lea rsi,[Parm01_FilePath]  ; Path
lea rdi,[TextBuffer]
call ItemWrite
push rsi
lea rsi,[FilePath]
call ItemWrite
pop rsi
call ItemWrite_CRLF        ; One block size
mov eax,[BlockSize]
mov rcx,rax
call SizePrint
call ItemWrite_CRLF        ; Blocks count 
mov eax,[BlockCount]
mov bl,0
call DecimalPrint32
mul rcx
call ItemWrite_CRLF        ; Result file size
call SizePrint
call ItemWrite_CRLF        ; File flags
mov eax,[FileFlags]
call HexPrint32
mov al,'h'
stosb
call ItemWrite_CRLF        ; Measurement repeats
mov eax,[MeasureRepeats]
call DecimalPrint32
call ItemWrite_CRLF        ; Memory buffer size
mov rax,[MemorySize]
call SizePrint
call ItemWrite_CRLF        ; Memory buffer base
mov rax,[MemoryBase]
call HexPrint64
mov al,'h'
stosb
call ItemWrite_CRLF        ; NUMA node ID
mov eax,[NumaNode]
call DecimalPrint32 
call ItemWrite_CRLF        ; CPU affinity mask (setup)
mov rax,[CpuAffinity]
test rax,rax
jz .NoAf1
call HexPrint64
mov al,'h'
stosb
jmp .EndAf1
.NoAf1:
mov eax,' n/a'
stosd
.EndAf1:
call ItemWrite_CRLF        ; CPU affinity mask (original, previous)
mov rax,[OrigAffinity]
test rax,rax
jz .NoAf2
call HexPrint64
mov al,'h'
stosb
jmp .EndAf2
.NoAf2:
mov eax,' n/a'
stosd
.EndAf2:
call ItemWrite_CRLF        ; CPU TSC frequency 
mov rax,[TscFrequency]
xor edx,edx                ; This clear entire RDX (not EDX only)
mov rcx,100000             ; Divisor=100000 for [Hz] to [MHz/10]
div rcx                    ; RAX = Frequency, [MHz/10]
shld rdx,rax,32
test edx,edx
jnz .BadF                  ; Go skip if frequency > 2^32
call FloatPrintP1          ; Print Frequency as X.Y number
mov eax,' MHz'
stosd
jmp .EndF
.BadF:
mov al,'?'
stosb
.EndF:
call ItemWrite_CRLF        ; CPU TSC period
mov rax,[TscPeriod]
xor edx,edx                ; This clear entire RDX (not EDX only)
mov rcx,100                ; Divisor=100 for [fs] to [ps/10]
div rcx                    ; RAX = Period, [ps/10]
shld rdx,rax,32
test edx,edx
jnz .BadT                  ; Go skip if frequency > 2^32
call FloatPrintP1          ; Print Frequency as X.Y number
mov eax,' ps '
stosd
jmp .EndT
.BadT:
mov al,'?'
stosb
.EndT:
call ItemWrite_CRLF        ; Benchmarks results: Read speed
mov rax,[ReadBytesPerSecond]
test rax,rax
jz .BadRS
call FloatPrintP1          ; Print Frequency as X.Y number
mov al,' '
stosb
mov eax,'MBPS'
stosd
jmp .EndRS
.BadRS:
mov al,'?'
stosb
.EndRS:
call ItemWrite_CRLF        ; Benchmarks results: Write speed
mov rax,[WriteBytesPerSecond]
test rax,rax
jz .BadWS
call FloatPrintP1          ; Print Frequency as X.Y number
mov al,' '
stosb
mov eax,'MBPS'
stosd
jmp .EndWS
.BadWS:
mov al,'?'
stosb
.EndWS:
mov al,0                   ; Termination sequence of strings
stosb

;--- (15) --- Show box --------------------------------------------------------;

xor ecx,ecx	                 ; Parm#1 = Parent window handle or 0
lea rdx,[TextBuffer]         ; Parm#2 = Pointer to string
lea r8,[ProductID]           ; Parm#3 = Pointer to caption
mov r9d,MB_ICONINFORMATION   ; Parm#4 = Message box icon type
call [MessageBoxA]

;--- (16) --- Exit program ----------------------------------------------------;

ExitProgram:
xor ecx,ecx	               ; RCX = Parm#1 = Exit code
call [ExitProcess]         ; No return from this function

;--- (17) --- Exit points for error exit and normal exit ----------------------;
; Reserved for add functionality, restore system context:
; release memory, close handles, restore affinity
; Terminology notes: MB = Message Box
; R15 = Pointer to action phase description string
; R14 = Error code, 0 means required get from OS API

ErrorProgram:

;--- Write operation phase description string ---
cld
lea rsi,[ErrorPhaseString]
lea rdi,[TextBuffer]
@@:
movsb
cmp byte [rsi],0
jne @b
mov rsi,r15
@@:
movsb
cmp byte [rsi],0
jne @b
mov ax,0A0Dh
stosw
;--- Write status code string ---
lea rsi,[ErrorStatusString]
@@:
movsb
cmp byte [rsi],0
jne @b
xchg rax,r14
test rax,rax
jnz @f
call [GetLastError]
@@:
call HexPrint64
mov ax,0000h + 'h'
stosw

;---
; Display OS message box, return button ID
; Parm#1 = RCX = Parent window handle
; Parm#2 = RDX = Pointer to message string must be valid at this point,
; Parm#3 = R8  = Caption=0 means error message, otherwise pointer to caption
; Parm#4 = R9  = Message Box Icon Error = MB_ICNERROR
; Output = RAX = Pressed button ID, not used at this call
; Note INVOKE replaced to instructions for code size optimization!
; FASM recommend: invoke MessageBoxA,0,r15,0,MB_ICONERROR
;---
xor ecx,ecx	          ; Parm#1, this clear entire RCX by x64 architecture rules
lea rdx,[TextBuffer]	; Parm#2
xor r8d,r8d	          ; Parm#3
mov r9,MB_ICONERROR   ; Parm#4
call [MessageBoxA]
jmp ExitProgram

;---------- Continue code section, connect include modules --------------------;

include 'stringwrite.inc'     ; Write string, include selector-based
include 'itemwrite.inc'       ; Write string with CR,LF and spaces added 
include 'decprint.inc'        ; Built string for decimal numbers 
include 'hexprint.inc'        ; Built string for hexadecimal numbers
include 'floatprint.inc'      ; Built string for float numbers
include 'sizeprint.inc'       ; Built string for memory block size
include 'checkcpuid.inc'      ; Verify CPUID support, return max. sup. function
include 'measurecpuclk.inc'   ; Measure CPU TSC clock frequency  

;---------- Data section ------------------------------------------------------;

section '.data' data readable writeable

;--- Names for operations steps ---
Step01_MeasureClock        DB  'Measure CPU clock frequency',0
Step02_OsApiDetection      DB  'OS API functions detection',0
Step03_MemoryAllocation    DB  'NUMA-aware memory allocation',0
Step04_MemoryBlank         DB  'Memory blank',0
Step05_SetAffinity         DB  'Set CPU affinity mask',0
Step06_OpenFile            DB  'Open or create target file',0
Step07_WriteFile           DB  'Write target file',0
Step08_ReadFile            DB  'Read target file',0
Step09_CloseFile           DB  'Close target file',0
Step10_RestoreAffinity     DB  'Restore affinity',0
Step11_ReleaseMemory       DB  'Release memory',0
Step12_CalculateSpeed      DB  'Calculate results',0
Step13_DeleteFile          DB  'Delete file',0

;--- Names for parameters ---
Parm01_FilePath            DB  'Path:',0
Parm02_BlockSize           DB  'File IO block size',0
Parm03_BlocksCount         DB  'File IO block count',0
Parm04_FileSize            DB  'Result file size',0
Parm05_FileFlags           DB  'File performance flags',0
Parm06_MeasureRepeats      DB  'Measurement repeats',0
Parm07_BufferSize          DB  'Memory buffer size',0
Parm08_BufferStartStop     DB  'Memory bufer base',0
Parm09_NumaNode            DB  'NUMA node ID',0
Parm10_CpuAffinity         DB  'CPU adjusted affinity mask',0
Parm11_CpuOrigAffinity     DB  'CPU original affinity mask',0
Parm12_TscFrequency        DB  'CPU TSC frequency',0
Parm13_TscPeriod           DB  'CPU TSC period',0
Parm14_ReadSpeed           DB  0Dh,0Ah, 'Read speed',0
Parm15_WriteSpeed          DB  'Write speed',0

;--- Common constants ---
ErrorPhaseString           DB  'Disk benchmarks failed',0Dh,0Ah
                           DB  'Error phase: ',0
ErrorStatusString          DB  'Error status: ',0
ProductID                  DB  'NativeBench v0.05 for Windows x64',0
NameKernel32               DB  'KERNEL32.DLL',0
NameVirtualAllocExNuma     DB  'VirtualAllocExNuma',0
NameSetThreadAffinityMask  DB  'SetThreadAffinityMask',0

;--- Memory size and speed units ---
U_B                        DB  'Bytes',0
U_KB                       DB  'KB',0
U_MB                       DB  'MB',0
U_GB                       DB  'GB',0
U_TB                       DB  'TB',0

;--- File path ---
FilePath                   DB  PATH_STRING

;--- Benchmarks results (pre-blanked=-1) ---
align 8
ReadClocks                 DQ  -1
WriteClocks                DQ  -1
ReadBytesPerSecond         DQ  -1
WriteBytesPerSecond        DQ  -1 

;--- Constants for calculations ---
Const1E10                  DQ  1000000000 * 10    ; Constant for fs to 10*us

;--- Common variables/constants ---
BlockSize                  DD  BLOCK_SIZE         ; Size of one blocks, bytes
BlockCount                 DD  BLOCK_COUNT        ; Number of blocks per file               
MeasureRepeats             DD  MEASURE_REPEATS    ; Measurement repeats
NumaNode                   DD  NUMA_NODE          ; NUMA node number
FileFlags                  DD  FILE_FLAGS         ; File performance flags
CpuAffinity                DQ  CPU_AFFINITY       ; New affinity mask
OrigAffinity               DQ  0                  ; Original affinity mask
MemoryBase                 DQ  ?       ; This changed by alignment requirements
TrueMemoryBase             DQ  ?       ; This required for memory block release
MemorySize                 DQ  ?       ; Size of allocated memory block
TscFrequency               DQ  ?       ; CPU TSC Frequency, Hz
TscPeriod                  DQ  ?       ; CPU TSC Period, picoseconds
SizeReturn                 DD  ?, ?    ; Output field for file IO functions 

;--- OS API Functions pointers ---
PVirtualAllocExNuma        DQ  ?       ; Pointer to detectable WinAPI function
PSetThreadAffinityMask     DQ  ?       ; Pointer to detectable WinAPI function

;--- Transit buffer for text ---
TextBuffer                 DB  1024 DUP (?)       ; Buffer for text block built

;---------- Import section ----------------------------------------------------;

section '.idata' import data readable writeable

library user32, 'USER32.DLL', kernel32, 'KERNEL32.DLL'
include 'api\user32.inc'
include 'api\kernel32.inc'

;---------- End ---------------------------------------------------------------;
