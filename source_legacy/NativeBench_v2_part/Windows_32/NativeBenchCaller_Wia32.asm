;==============================================================================;
;                                                                              ;
;     Disk Read and Write Benchmarks (engineering release). Win32 Edition.     ; 
;                           (C)2018 IC Book Labs.                              ;
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

BLOCK_SIZE       EQU  1024*1024*10  ; 1024*1024*1   ; Disk block size, bytes
BLOCK_COUNT      EQU  10            ; 100           ; Number of disk blocks
MEASURE_REPEATS  EQU  5                             ; Measurement repeats
NUMA_NODE        EQU  0                             ; NUMA node memory alloc.
CPU_AFFINITY     EQU  00                            ; Mask or 0=Skip
SRC_PATH_STRING  EQU  'SRC_FILE.TMP',0              ; Source file path
DST_PATH_STRING  EQU  'DST_FILE.TMP',0              ; Destination file path

FILE_FLAGS       EQU  FILE_ATTRIBUTE_NORMAL     \
                      + FILE_FLAG_NO_BUFFERING  \
                      + FILE_FLAG_WRITE_THROUGH     ; File performance flags

;---------- FASM definitions --------------------------------------------------;

include 'win32a.inc'

;---------- Code section ------------------------------------------------------;

format PE GUI 4.0
entry start
section '.text' code readable executable
start:

;--- (1) --- Check CPU features and measure CPU clock -------------------------;

mov [StepName],Step01_MeasureClock   ; Pointer to phase name for error report
mov [StepError],1                    ; Status code for error report
call CheckCpuId
jc ErrorProgram                 ; Go if CPUID not supported or locked
inc [StepError]
cmp eax,1
jc ErrorProgram                  ; Go if CPUID function 1 not sup. or locked 
inc [StepError]
mov eax,1
cpuid
test dl,10h
jz ErrorProgram                  ; Go if TSC not supported
inc [StepError]
call MeasureCpuClk
jc ErrorProgram                  ; Go if TSC clock measurement error
inc [StepError]
mov dword [TscFrequency+0],ebx   ; Store TSC frequency, low dword
mov dword [TscFrequency+4],ecx   ; Store TSC frequency, high dword
or ecx,ebx
jz ErrorProgram                  ; Go if TSC error: frequency=0
mov dword [TscPeriod+0],eax      ; Store TSC period, low dword
mov dword [TscPeriod+4],edx      ; Store TSC period, high dword
or eax,edx
jz ErrorProgram                  ; Go if TSC error: period=0

;--- (2) --- OS API detection -------------------------------------------------;

mov [StepName],Step02_OsApiDetection   ; Pointer to phase name for error report
push NameKernel32                ; Parm#1 = Pointer to module name string
call [GetModuleHandle]           ; EAX = Return module handle
mov [StepError],1
test eax,eax
jz ErrorProgram                  ; Go if KERNEL32.DLL not found
xchg ebx,eax
;--- Get WinAPI handler for NUMA-aware memory allocation --- 
push NameVirtualAllocExNuma      ; Parm#2 = Pointer to function name
push ebx                         ; Parm#1 = Pointer to module handle
call [GetProcAddress]            ; EAX = Return function address
inc [StepError]
test eax,eax
jz ErrorProgram                  ; Go if function not found
mov [PVirtualAllocExNuma],eax
;--- Get WinAPI handler for set thread affinity mask ---
push NameSetThreadAffinityMask
push ebx
call [GetProcAddress]
inc [StepError]
test eax,eax
jz ErrorProgram
mov [PSetThreadAffinityMask],eax

;--- (3) --- NUMA-aware memory allocation  ------------------------------------;

mov [StepName],Step03_MemoryAllocation
mov [StepError],0
call [GetCurrentProcess]         ; Return EAX = Handle
test eax,eax
jz ErrorProgram       ; Go if get handle error
;--- NUMA-aware memory allocation --- 
xchg ecx,eax            ; Parm#1 = ECX = Handle
; mov eax,[BlockSize]   ; EAX = One block size
; mov ebx,[BlockCount]  ; EBX = Number of blocks
; mul ebx               ; RAX = Total size
;
mov eax, ( BLOCK_SIZE+4096 ) * 2
;
test eax,0FFFh
jz @f
and eax,0FFFFF000h
add eax,000001000h   ; This for size alignment
@@:
add eax,4096
mov [MemorySize],eax
push [NumaNode]		   ; Parm#6 = NUMA node preferred = 0
push 00000004h			 ; Parm#5 = Pages protection attributes = Read/Write
push 00003000h	     ; Parm#4 = Alloc. type: RESERVE(2000h) + COMMIT(1000h)
push eax             ; Parm#3 = Memory block size, with base align rsvd.
push 0               ; Parm#2 = Base address, 0 = auto 
push ecx             ; Parm#1 = ECX = Handle 
call [PVirtualAllocExNuma] 
test eax,eax
jz ErrorProgram      ; Go if memory allocation error
;--- Store true base for release and aligned base for use memory ---
mov [TrueMemoryBase],eax
test eax,0FFFh
jz @f
and eax,0FFFFF000h
add eax,000001000h   ; This for size alignment
@@:
mov [MemoryBase],eax

;--- (4) --- Memory blank -----------------------------------------------------;
; No error branches for this operation, error reporting is reserved

cld
mov edi,[MemoryBase]         ; EDI = Block base
mov ecx,[MemorySize]         ; ECX = Block size, bytes count
shr ecx,2                    ; ECX = DWords count = Bytes count / 4
mov eax,'DATA'               ; This data for fill buffer
rep stosd

;--- (5) --- Set affinity mask ------------------------------------------------;

mov [StepName],Step05_SetAffinity
mov ebx,[CpuAffinity]
test ebx,ebx
jz @f                  ; Skip affinitization if mask = 0
call [GetCurrentThread]
mov [StepError],1
test eax,eax
jz ErrorProgram        ; Go if thread error 
push ebx               ; Parm#2 = Thread affinity mask
push eax               ; Parm#1 = Thread handle
call [SetThreadAffinityMask]
inc [StepError]
test eax,eax           ; EAX = Old affinity mask
jz ErrorProgram        ; Go if affinity mask error 
mov [OrigAffinity],eax
@@:

mov edi,[MemoryBase]                    ; EDI = Pointer to Input Parms. Block

xor eax,eax
stosd
stosd

mov eax,[BlockSize]
stosd
xor eax,eax
stosd

mov eax,[BlockCount]
stosd
xor eax,eax
stosd

mov eax,[FileFlags]
stosd
push eax
xor eax,eax
stosd
pop eax

stosd
xor eax,eax
stosd

mov eax,[MeasureRepeats]
stosd
xor eax,eax
stosd

push edi
lea esi,[SrcFilePath]
@@:
lodsb
stosb
cmp al,0
jne @b
pop edi

lea esi,[DstFilePath]
add edi,976
@@:
lodsb
stosb
cmp al,0
jne @b

mov esi,[MemoryBase]                 ; RSI = Pointer to Input Parameters Block
lea edi,[esi + BLOCK_SIZE + 4096 ]   ; RDI = Pointer to Output Parameters Block
; mov ebp,MEASURE_REPEATS
; @@:
call API_WRITE_READ_COPY_DELETE

mov eax,[edi+00]
mov edx,[edi+04]
add dword [WinWriteClocks+00],eax  ; ADD, must be pre-blanked
adc dword [WinWriteClocks+04],edx

mov eax,[edi+08]
mov edx,[edi+12]
add dword [WinReadClocks+00],eax   ; ADD, must be pre-blanked
adc dword [WinReadClocks+04],edx

mov eax,[edi+16]
mov edx,[edi+20]
add dword [WinCopyClocks+00],eax   ; ADD, must be pre-blanked
adc dword [WinCopyClocks+04],edx

; dec ebp
; jnz @b


;--- (10) --- Restore affinity ------------------------------------------------;

mov [StepName],Step10_RestoreAffinity
mov [StepError],0
cmp [CpuAffinity],0
je @f                        ; Skip operation if affinity not changed
call [GetCurrentThread]
test eax,eax
jz ErrorProgram              ; Go if thread error 
push [OrigAffinity]          ; Parm#2 = Thread affinity mask
push eax                     ; Parm#1 = EAX = Thread handle 
call [SetThreadAffinityMask]
test eax,eax                 ; EAX = Old affinity mask
jz ErrorProgram              ; Go if affinity mask error 
@@:

;--- (11) --- Release memory --------------------------------------------------;

mov [StepName],Step11_ReleaseMemory
mov [StepError],0
call [GetCurrentProcess]     ; Return EAX = Handle
test eax,eax
jz ErrorProgram              ; Go if handle error
push MEM_RELEASE	           ; Parm#4 = Release type
push 0                       ; Parm#3 = Release size, 0 = Entire block
push [TrueMemoryBase]        ; Parm#2 = Address
push eax                     ; Parm#1 = EAX = Process Handle
call [VirtualFreeEx]
test eax,eax
jz ErrorProgram              ; Go if function error

;--- (12) --- Calculate speed = F (dTSC, TotalSize) ---------------------------;

;--- Detect x87 presence ---
mov [StepName],Step12_CalculateSpeed
mov [StepError],1
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
fild [WinTimePeriod]    ; Load ST0 = Period, femtoseconds
fild [Const1E10]        ; Load ST0 = Constant for fs to 10*us
;--- Base variables preloaded, after load Clocks ---
; ST3 = Total size transferred, Bytes 
; ST2 = Period, femtoseconds
; ST1 = Constant for fs to 10*us
; ST0 = Read or Write clocks, depend on step
;--- Calculations for Read ---
fild [WinReadClocks]    ; ST0 = TSC clocks per target operation
fmul st0,st2            ; ST0 = Femtoseconds per target operation
fdiv st0,st1            ; ST0 = Same interval in units 10*us
fdivr st0,st3           ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [ReadBytesPerSecond]    ; Bytes per 10 uS , approx. MBPS*10
;--- Calculations for Write ---
fild [WinWriteClocks]   ; ST0 = TSC clocks per target operation
fmul st0,st2            ; ST0 = Femtoseconds per target operation
fdiv st0,st1            ; ST0 = Same interval in units 10*us
fdivr st0,st3           ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [WriteBytesPerSecond]   ; Bytes per 10 uS , approx. MBPS*10
;--- Calculations for Copy ---
fild [WinCopyClocks]    ; ST0 = TSC clocks per target operation
fmul st0,st2            ; ST0 = Femtoseconds per target operation
fdiv st0,st1            ; ST0 = Same interval in units 10*us
fdivr st0,st3           ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [CopyBytesPerSecond]   ; Bytes per 10 uS , approx. MBPS*10

;--- (13) --- Delete test file ------------------------------------------------;

; mov [StepName],Step13_DeleteFile
; mov [StepError],0
; push FilePath                ; Parm#1 = Pointer to file path
; call [DeleteFileA]
; test eax,eax
; jz ErrorProgram              ; Go if function error

;--- (14) --- Prepare results for visual --------------------------------------;   

lea esi,[Parm01_FilePath]  ; Paths
lea edi,[TextBuffer]
call ItemWrite
push esi
lea esi,[SrcFilePath]
call ItemWrite
mov ax,', '
stosw
lea esi,[DstFilePath]
call ItemWrite
pop esi
call ItemWrite_CRLF        ; One block size
mov eax,[BlockSize]
mov ecx,eax
call SizePrint
call ItemWrite_CRLF        ; Blocks count 
mov eax,[BlockCount]
mov bl,0
call DecimalPrint32
mul ecx
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
mov eax,[MemorySize]
call SizePrint
call ItemWrite_CRLF        ; Memory buffer base
mov eax,[MemoryBase]
call HexPrint32
mov al,'h'
stosb
call ItemWrite_CRLF        ; NUMA node ID
mov eax,[NumaNode]
call DecimalPrint32 
call ItemWrite_CRLF        ; CPU affinity mask (setup)
mov eax,[CpuAffinity]
test eax,eax
jz .NoAf1
call HexPrint32
mov al,'h'
stosb
jmp .EndAf1
.NoAf1:
mov eax,' n/a'
stosd
.EndAf1:
call ItemWrite_CRLF        ; CPU affinity mask (original, previous)
mov eax,[OrigAffinity]
test eax,eax
jz .NoAf2
call HexPrint32
mov al,'h'
stosb
jmp .EndAf2
.NoAf2:
mov eax,' n/a'
stosd
.EndAf2:
call ItemWrite_CRLF        ; CPU TSC frequency 
mov eax,dword [TscFrequency + 0]
mov edx,dword [TscFrequency + 4]
mov ecx,100000             ; Divisor=100000 for [Hz] to [MHz/10]
cmp edx,ecx
jae .BadF
div ecx                    ; EDX:EAX = Frequency, [MHz/10]
call FloatPrintP1          ; Print Frequency as X.Y number
mov eax,' MHz'
stosd
jmp .EndF
.BadF:
mov al,'?'
stosb
.EndF:
call ItemWrite_CRLF        ; CPU TSC period
mov eax,dword [TscPeriod + 0]
mov edx,dword [TscPeriod + 4]
mov ecx,100                ; Divisor=100 for [fs] to [ps/10]
cmp edx,ecx
jae .BadT
div ecx                    ; EAX = Period, [ps/10]
call FloatPrintP1          ; Print Frequency as X.Y number
mov eax,' ps '
stosd
jmp .EndT
.BadT:
mov al,'?'
stosb
.EndT:

call ItemWrite_CRLF        ; Benchmarks results: Read speed
mov eax,dword [ReadBytesPerSecond]
test eax,eax
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
mov eax,dword [WriteBytesPerSecond]
test eax,eax
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

call ItemWrite_CRLF        ; Benchmarks results: Copy speed
mov eax,dword [CopyBytesPerSecond]
test eax,eax
jz .BadCS
call FloatPrintP1          ; Print Frequency as X.Y number
mov al,' '
stosb
mov eax,'MBPS'
stosd
jmp .EndCS
.BadCS:
mov al,'?'
stosb
.EndCS:

mov al,0                   ; Termination sequence of strings
stosb

;--- (15) --- Show box --------------------------------------------------------;

push MB_ICONINFORMATION    ; Parm#4 = Message box icon type
push ProductID             ; Parm#3 = Pointer to caption
push TextBuffer            ; Parm#2 = Pointer to string
push 0	                   ; Parm#1 = Parent window handle or 0
call [MessageBoxA]

;--- (16) --- Exit program ----------------------------------------------------;

ExitProgram:
xor ecx,ecx	               ; ECX = Parm#1 = Exit code
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
lea esi,[ErrorPhaseString]
lea edi,[TextBuffer]
@@:
movsb
cmp byte [esi],0
jne @b
mov esi,[StepName]
@@:
movsb
cmp byte [esi],0
jne @b
mov ax,0A0Dh
stosw
;--- Write status code string ---
lea esi,[ErrorStatusString]
@@:
movsb
cmp byte [esi],0
jne @b
mov eax,[StepError]
test eax,eax
jnz @f
call [GetLastError]
@@:
call HexPrint32
mov ax,0000h + 'h'
stosw

;---
; Display OS message box, return button ID
; Parm#1 = Parent window handle
; Parm#2 = Pointer to message string must be valid at this point,
; Parm#3 = Caption=0 means error message, otherwise pointer to caption
; Parm#4 = Message Box Icon Error = MB_ICNERROR
; Output = EAX = Pressed button ID, not used at this call
; Note INVOKE replaced to instructions for code size optimization!
; FASM recommend: invoke MessageBoxA,0,r15,0,MB_ICONERROR
;---
push MB_ICONERROR   ; Parm#4
push 0	            ; Parm#3
push TextBuffer	    ; Parm#2
push 0	            ; Parm#1
call [MessageBoxA]
jmp ExitProgram

;---------- Continue code section, connect include modules --------------------;

include 'Include\stringwrite.inc'    ; Write string, include selector-based
include 'Include\itemwrite.inc'      ; Write string with CR,LF and spaces added 
include 'Include\decprint.inc'       ; Built string for decimal numbers 
include 'Include\hexprint.inc'       ; Built string for hexadecimal numbers
include 'Include\floatprint.inc'     ; Built string for float numbers
include 'Include\sizeprint.inc'      ; Built string for memory block size
include 'Include\checkcpuid.inc'     ; Verify CPUID support, max. sup. function
include 'Include\measurecpuclk.inc'  ; Measure CPU TSC clock frequency

include 'NativeBenchHandler.inc'  ; Callee prepared for JNI integration  

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
Parm16_CopySpeed           DB  'Copy speed',0

;--- Common constants ---
ErrorPhaseString           DB  'Disk benchmarks failed',0Dh,0Ah
                           DB  'Error phase: ',0
ErrorStatusString          DB  'Error status: ',0
ProductID                  DB  'NativeBench v0.22 for Windows ia32',0
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
SrcFilePath                DB  SRC_PATH_STRING
DstFilePath                DB  DST_PATH_STRING

;--- Benchmarks results (pre-blanked=-1) ---
align 8

ReadBytesPerSecond         DQ  0
WriteBytesPerSecond        DQ  0
CopyBytesPerSecond         DQ  0

WinReadClocks              DQ  0
WinWriteClocks             DQ  0
WinCopyClocks              DQ  0

WinTimeStart               DQ  0
WinTimeStop                DQ  0
WinTimePeriod              DQ  100000000    ; femtoseconds per 100 nanoseconds

;--- Constants for calculations ---
Const1E10                  DQ  1000000000 * 10    ; Constant for fs to 10*us

;--- Common variables/constants ---
BlockSize                  DD  BLOCK_SIZE         ; Size of one blocks, bytes
BlockCount                 DD  BLOCK_COUNT        ; Number of blocks per file               
MeasureRepeats             DD  MEASURE_REPEATS    ; Measurement repeats
NumaNode                   DD  NUMA_NODE          ; NUMA node number
FileFlags                  DD  FILE_FLAGS         ; File performance flags
CpuAffinity                DD  CPU_AFFINITY       ; New affinity mask
OrigAffinity               DD  0                  ; Original affinity mask
MemoryBase                 DD  ?       ; This changed by alignment requirements
TrueMemoryBase             DD  ?       ; This required for memory block release
MemorySize                 DD  ?       ; Size of allocated memory block
TscFrequency               DQ  ?       ; CPU TSC Frequency, Hz
TscPeriod                  DQ  ?       ; CPU TSC Period, picoseconds
SizeReturn                 DD  ?, ?    ; Output field for file IO functions 

;--- OS API Functions pointers ---
PVirtualAllocExNuma        DD  ?       ; Pointer to detectable WinAPI function
PSetThreadAffinityMask     DD  ?       ; Pointer to detectable WinAPI function

;--- Step ID for error reporting ---
StepName                   DD  ?       ; Pointer to step name string
StepError                  DD  ?       ; Sub-step ID or 0 = Get WinAPI error

;--- Temporary storage for TSC ---
TempTSC                    DD ?

;--- Temporary storage for file handle ---
TempHandle                 DD ?

;--- Transit buffer for text ---
TextBuffer                 DB  1024 DUP (?)    ; Buffer for text block built

;---------- Import section ----------------------------------------------------;

section '.idata' import data readable writeable

library user32, 'USER32.DLL', kernel32, 'KERNEL32.DLL'
include 'api\user32.inc'
include 'api\kernel32.inc'

;---------- End ---------------------------------------------------------------;
