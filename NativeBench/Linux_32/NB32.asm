;==============================================================================;
;                                                                              ;
;   Disk Read and Write Benchmarks (engineering release). Linux 32 Edition.    ; 
;                           (C)2016 IC Book Labs.                              ;
;                                                                              ;
;  This file is main module: translation object, interconnecting all modules.  ;
;                 This revision rejects Read and Write cache.                  ;
;                                                                              ;
;        Translation by Flat Assembler version 1.71.49 (Dec 06, 2015)          ;
;           Visit http://flatassembler.net/ for more information.              ;
;    For right tabulations, please edit by FASM Editor 2.0 or NoteTab Pro.     ;
;                                                                              ;
;==============================================================================;

; Get information from:
; Ray Seyfarth PDF is main valid book
; http://man7.org/linux/man-pages/dir_section_2.html
; http://man7.org/linux/man-pages/man2/creat.2.html
; http://lxr.free-electrons.com/source/include/uapi/asm-generic/fcntl.h
; http://lxr.free-electrons.com/source/include/asm-x86_64/unistd.h?v=2.4.37
; https://sourcecodebrowser.com/numactl/2.0.4~rc2/syscall_8c_source.html
; http://unix.superglobalmegacorp.com/Net2/newsrc/sys/mman.h.html
; http://lxr.free-electrons.com/source/arch/arm/include/asm/unistd.h?v=2.6.32
; http://src.gnu-darwin.org/src/include/time.h.html
; http://syscalls.kernelgrok.com/
; http://www-numi.fnal.gov/offline_software/srt_public_context/WebDocs/Errors/unix_system_errors.html 
; http://lxr.free-electrons.com/source/arch/x86/include/asm/unistd_64.h?v=3.0

; Linux32 system calls notes.
; Function code = EAX
; Input parameters = EBX, ECX, EDX, ESI, EDI, EBP
; Output parameter = EAX

;---------- Program sample setup variables ------------------------------------; 

BLOCK_SIZE       EQU  1024*1024*1     ; Disk block size, bytes
BLOCK_COUNT      EQU  100             ; Number of disk blocks

; Detected 4KB limitation for SYS_WRITE function.
; BLOCK_SIZE    EQU  4096*4
; BLOCK_COUNT   EQU  100*1024*1024 / 4096
; BLOCK_COUNT   EQU  1000*1024 / 4096

MEASURE_REPEATS  EQU  5                     ; Measurement repeats
NUMA_NODE        EQU  0                     ; NUMA node memory alloc.
CPU_AFFINITY     EQU  00                    ; Mask or 0=Skip

; SRC_PATH_STRING  EQU  'SRC_FILE.TMP',0    ; Source file path
; DST_PATH_STRING  EQU  'DST_FILE.TMP',0    ; Destination file path
SRC_PATH_STRING  EQU  'testfile1.txt',0
DST_PATH_STRING  EQU  'testfile2.txt',0

; Flags OR value effective for write = 00004000h , 00001000h , 00100000h

FILE_FLAGS       EQU  00000042h + 00004000h

format ELF executable 3
segment readable executable
entry start

;---------- Code.Initialization --------------------------------------------;
segment readable executable
start:

;--- Message "Starting..." -------------------------------------------------;

mov eax,4
mov ebx,1
lea ecx,[MsgStarting]
mov edx,SizeStarting
int 80h

;--- (1) --- Check CPU features and measure CPU clock ----------------------;

mov [ErrorStep],Step01_MeasureClock  ; Pointer to phase name for error report
mov [ErrorSubStep],1                 ; Status code for error report
call CheckCpuId
jc ErrorProgram                  ; Go if CPUID not supported or locked
inc [ErrorSubStep]
cmp eax,1
jc ErrorProgram                  ; Go if CPUID function 1 not sup. or locked 
inc [ErrorSubStep]
mov eax,1
cpuid
test dl,10h
jz ErrorProgram                  ; Go if TSC not supported
inc [ErrorSubStep]
call MeasureCpuClk
jc ErrorProgram                  ; Go if TSC clock measurement error
inc [ErrorSubStep]
push eax
or eax,edx
pop eax
jz ErrorProgram                  ; Go if TSC error: frequency=0
push ebx
or ebx,ecx
pop ebx
jz ErrorProgram                  ; Go if TSC error: period=0 
mov dword [TscFrequency+0],eax   ; Store TSC frequency, Hz
mov dword [TscFrequency+4],edx
mov dword [TscPeriod+0],ebx      ; Store TSC period, fs
mov dword [TscPeriod+4],ecx

;--- (2) --- Check OS timer, get OS timer resolution -----------------------;

mov [ErrorStep],Step02_CheckOsTimer
xor ebx,ebx                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea ecx,[TimespecResolution]   ; Parm#2 = Pointer to output DQ sec, ns
mov eax,266                    ; Function code = SYS_GETRES
push ecx
int 80h
pop ecx
test eax,eax
mov [ErrorSubStep],eax
jnz ErrorProgram               ; Go if error returned
mov [ErrorSubStep],1
cmp dword [ecx+00],0
jne ErrorProgram               ; Go if seconds > 0 , time units too big
inc [ErrorSubStep]
mov eax,[ecx+04]
test eax,eax
jz ErrorProgram                ; Go if nanoseconds = 0 , time units zero
cmp eax,1000000
ja ErrorProgram                ; Go if nanoseconds > 1000000 , time units too big

;--- (3) --- NUMA-aware memory allocation  ---------------------------------;
; See mempolicy fragment in the x64 variant.

PROT_READ    = 1
PROT_WRITE   = 2
PROT_EXEC    = 4
PROT_RW      = PROT_READ + PROT_WRITE

MAP_SHARED   = 1
MAP_PROVATE  = 2
MAP_ANONYMOS = 20h
MAP_MEM      = MAP_ANONYMOS + MAP_SHARED

;--- Memory allocation ----
mov [ErrorStep],Step03_MemoryAllocation
xor ebp,ebp                       ; EBP = Parm#6 = File offset, not used, no file, set 0
xor edi,edi                       ; EDI = Parm#5 = File descriptor, not used, no file, set 0
mov esi,MAP_MEM                   ; ESI = Parm#4 = Flags, anonymos means don't load file
mov edx,PROT_RW                   ; EDX = Parm#3 = Protection attributes: Readable/Writeable/Executable

; 4096 bytes reserved for Page Alignment

mov ecx,( BLOCK_SIZE+2048 + 4096 ) * 2   ; ECX = Parm#2 = Size of block, bytes, 100000h = 1MB
mov dword [MemorySize],ecx
xor ebx,ebx                       ; EBX = Parm#1 = Base address of block, 0 = Must be selected by OS
mov eax,192                       ; EAX = Syscall Number = SYS_MMAP
int 80h                           ; Return mapped region 32-bit address at EAX or status if error
mov [ErrorSubStep],eax
test eax,eax
jz ErrorProgram                   ; Go exit if error detected, base address = 0
cmp eax,0FFFF0000h
ja ErrorProgram                   ; Go exit if error detected, base address invalid = error code
mov dword [TrueMemoryBase],eax

;--- New alignment in the callee --
; test eax,0FFFh
; jz @f
; and eax,0FFFFF000h
; add eax,000001000h                ; This for size alignment
; @@:

mov dword [MemoryBase],eax

;--- (4) --- Memory blank --------------------------------------------------;
; No error branches for this operation, error reporting is reserved

cld
mov edi,dword [MemoryBase]        ; RDI = Block base
mov ecx,dword [MemorySize]        ; RCX = Block size, bytes count
shr ecx,2                         ; ECX = QWords count = Bytes count / 8
mov eax,'DATA'                    ; This data for fill buffer
rep stosd

;--- (5) --- Set affinity mask ---------------------------------------------;

mov [ErrorStep],Step05_SetAffinity
mov eax,dword [CpuAffinity+0]
or eax,dword [CpuAffinity+4]
jz @f
;--- Get current affinity mask ---
xor ebx,ebx                  ; Parm#1 = 0, means current thread for affinity mask operation
mov ecx,8                    ; Parm#2 = mask size, bytes
lea edx,[OrigAffinity]       ; Parm#3 = Pointer to affinity mask
mov eax,242                  ; Function = Get current affinity mask = SYS_GETAFFINITY
int 80h
;--- Check output: mask size, bytes ---
mov [ErrorSubStep],eax
cmp eax,8
jb ErrorProgram
cmp eax,32
ja ErrorProgram
;--- Set new affinity mask ---
xor ebx,ebx                  ; Parm#1 = 0, means current thread for affinity mask operation
mov ecx,8                    ; Parm#2 = mask size, bytes
lea edx,[CpuAffinity]        ; Parm#3 = Pointer to affinity mask
mov eax,241                  ; Function = Set new affinity mask = SYS_SETAFFINITY
int 80h
;--- Check output: status ---
test eax,eax
mov [ErrorSubStep],eax
jnz ErrorProgram
;--- Skip point ---
@@:

;--- (6) --- Operations with target file -----------------------------------;

mov edi,dword [MemoryBase]   ; EDI = Pointer to Input Parms. Block

xor eax,eax
stosd
stosd

mov eax,dword [BlockSize]
stosd
xor eax,eax
stosd

mov eax,dword [BlockCount]
stosd
xor eax,eax
stosd

mov eax,dword [FileFlags]

push eax
stosd
xor eax,eax
stosd
pop eax

stosd
xor eax,eax
stosd

mov eax,dword [MeasureRepeats]
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

mov esi,dword [MemoryBase]           ; ESI = Pointer to Input Parameters Block
lea edi,[esi + BLOCK_SIZE + 2048 + 4096 ]   ; EDI = Pointer to Output Parameters Block

;- mov ebp,MEASURE_REPEATS
;- @@:

call API_WRITE_READ_COPY_DELETE

mov eax,[edi+00]
mov edx,[edi+04]
add dword [LinuxWriteClocks+0],eax  ; ADD, must be pre-blanked
add dword [LinuxWriteClocks+4],edx

mov eax,[edi+08]
mov edx,[edi+12]
add dword [LinuxReadClocks+0],eax   ; ADD, must be pre-blanked
add dword [LinuxReadClocks+4],edx

mov eax,[edi+16]
mov edx,[edi+20]
add dword [LinuxCopyClocks+0],eax   ; ADD, must be pre-blanked
add dword [LinuxCopyClocks+4],edx

;- dec ebp
;- jnz @b

;--- (7) --- Restore affinity ----------------------------------------------;

mov [ErrorStep],Step07_RestoreAffinity
mov eax,dword [CpuAffinity+0]
or eax,dword [CpuAffinity+4]
je @f                        ; Skip operation if affinity not changed
;--- Set new affinity mask ---
xor ebx,ebx                  ; Parm#1 = 0, means current thread for affinity mask operation
mov ecx,8                    ; Parm#2 = mask size, bytes
lea edx,[OrigAffinity]       ; Parm#3 = Pointer to affinity mask
mov eax,241                  ; Function = Set new (original) affinity mask = SYS_SETAFFINITY
int 80h
;--- Check output: status ---
test eax,eax
mov [ErrorSubStep],eax
jnz ErrorProgram
;--- Skip point ---
@@:

;--- (8) --- Release memory ------------------------------------------------;

mov [ErrorStep],Step08_ReleaseMemory
mov ebx,dword [TrueMemoryBase]        ; Parm#1 = Base address, 0 means auto selection
mov ecx,( BLOCK_SIZE+2048+4096 ) * 2       ; Parm#2 = Block size, bytes
mov eax,91                            ; Function code = memory unmap (release) = SYS_MUNMAP
int 80h
test eax,eax
mov [ErrorSubStep],eax
jnz ErrorProgram

;--- (9) --- Calculate results ---------------------------------------------;

;--- Detect x87 presence ---
mov [ErrorStep],Step09_CalculateSpeed
mov [ErrorSubStep],1
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
fmul st0,st2             ; Load ST0 = Total size transferred, Bytes 
fild [LinuxTimePeriod]   ; Load ST0 = Period, femtoseconds
fild [Const1E10]         ; Load ST0 = Constant for fs to 10*us
;--- Base variables preloaded, after load Clocks ---
; ST3 = Total size transferred, Bytes 
; ST2 = Period, femtoseconds
; ST1 = Constant for fs to 10*us
; ST0 = Read or Write clocks, depend on step
;--- Calculations for Read ---
fild [LinuxReadClocks]   ; ST0 = TSC clocks per target operation
fmul st0,st2             ; ST0 = Femtoseconds per target operation
fdiv st0,st1             ; ST0 = Same interval in units 10*us
fdivr st0,st3            ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [ReadBytesPerSecond]    ; Bytes per 10 uS , approx. MBPS*10
;--- Calculations for Write ---
fild [LinuxWriteClocks]  ; ST0 = TSC clocks per target operation
fmul st0,st2             ; ST0 = Femtoseconds per target operation
fdiv st0,st1             ; ST0 = Same interval in units 10*us
fdivr st0,st3            ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [WriteBytesPerSecond]   ; Bytes per 10 uS , approx. MBPS*10
;--- Calculations for Copy ---
fild [LinuxCopyClocks]   ; ST0 = TSC clocks per target operation
fmul st0,st2             ; ST0 = Femtoseconds per target operation
fdiv st0,st1             ; ST0 = Same interval in units 10*us
fdivr st0,st3            ; ST0 = Size[bytes] / Time[ 10*us ], reverse: ST3/ST0 
fistp [CopyBytesPerSecond]   ; Bytes per 10 uS , approx. MBPS*10

;--- (10) --- Prepare results for visual -----------------------------------;

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
mov eax,dword [MemorySize]
call SizePrint

call ItemWrite_CRLF        ; Memory buffer base
mov eax,dword [MemoryBase]
call HexPrint32 ; 64
mov al,'h'
stosb

call ItemWrite_CRLF        ; NUMA node ID
mov eax,[NumaNode]
call DecimalPrint32 
call ItemWrite_CRLF        ; CPU affinity mask (setup)

mov eax,dword [CpuAffinity+0]
mov edx,dword [CpuAffinity+4]
push eax
or eax,edx
pop eax
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
mov eax,dword [OrigAffinity+0]
mov edx,dword [OrigAffinity+4]
push eax
or eax,edx
pop eax
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
mov eax,dword [TscFrequency+0]
mov edx,dword [TscFrequency+4]
mov ecx,100000             ; Divisor=100000 for [Hz] to [MHz/10]
cmp edx,ecx
jae .BadF
div ecx                    ; RAX = Frequency, [MHz/10]
call FloatPrintP1          ; Print Frequency as X.Y number
mov eax,' MHz'
stosd
jmp .EndF
.BadF:
mov al,'?'
stosb
.EndF:

call ItemWrite_CRLF          ; CPU TSC period
mov eax,dword [TscPeriod+0]
mov edx,dword [TscPeriod+4]
mov ecx,100                  ; Divisor=100 for [fs] to [ps/10]
cmp edx,ecx
jae .BadT
div ecx                      ; EAX = Period, [ps/10]
call FloatPrintP1            ; Print Frequency as X.Y number
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
call FloatPrintP1          ; Print MBPS as X.Y number
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
call FloatPrintP1          ; Print MBPS as X.Y number
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
call FloatPrintP1          ; Print MBPS as X.Y number
mov al,' '
stosb
mov eax,'MBPS'
stosd
jmp .EndCS
.BadCS:
mov al,'?'
stosb
.EndCS:

mov eax,00000A0Dh          ; CR, LF and 0 = termination sequence of strings
stosw
stosd

;--- (10) --- Output parameters strings, console mode ----------------------;

ConsoleWrite:

lea esi,[TextBuffer]       ; Pointer to data for output
push esi
xor edx,edx                ; EDX = Number of chars, 0=prepare for count
@@:
lodsb
cmp al,0
je @f
inc edx                    ; EDX is input parameter for next called API
jmp @b
@@:
pop ecx

mov eax,4
mov ebx,1
int 80h

;--- (11) --- Exit program -------------------------------------------------;

ExitProgram:

mov eax,1
xor ebx,ebx
int 80h

;--- (12) --- Exit point for error exit ------------------------------------;
; Reserved for add functionality, restore system context:
; release memory, close handles, restore affinity
; Terminology notes: MB = Message Box
; [ErrorStep] = Pointer to action phase description string
; [ErrorSubStep] = Error code, operation-specific, get from OS API or sub-step ID

ErrorProgram:

;--- Write operation phase description string ---
cld
lea esi,[ErrorPhaseString]
lea edi,[TextBuffer]
@@:
movsb
cmp byte [esi],0
jne @b
mov esi,[ErrorStep]
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
mov eax,[ErrorSubStep]
call HexPrint32
mov eax,0D0A0D00h + 'h'
stosd
mov ax,000Ah
stosw
jmp ConsoleWrite

;---------- Code.Include ---------------------------------------------------;

include 'Include\stringwrite.inc'    ; Write string, include selector-based
include 'Include\itemwrite.inc'      ; Write string with CR,LF and spaces added 
include 'Include\decprint.inc'       ; Built string for decimal numbers 
include 'Include\hexprint.inc'       ; Built string for hexadecimal numbers
include 'Include\floatprint.inc'     ; Built string for float numbers
include 'Include\sizeprint.inc'      ; Built string for memory block size
include 'Include\checkcpuid.inc'     ; Verify CPUID support, max. sup. function
include 'Include\measurecpuclk.inc'  ; Measure CPU TSC clock frequency

include 'NativeBenchHandler.inc'  ; Callee prepared for JNI integration   

;---------- Data -----------------------------------------------------------;

segment readable writeable

TimespecWait:        ; Seconds=1 and Nanoseconds=0 values, time for wait
DQ  1, 0         

TimespecRemain:      ; Seconds and Nanoseconds, remain stored if wait interrupted
DQ  0, 0

TimespecResolution:  ; Seconds and Nanoseconds, timer resolution
DQ  0, 0 

;--- Names for operations steps ---
Step01_MeasureClock        DB  'Measure CPU clock frequency',0
Step02_CheckOsTimer        DB  'Check OS timer',0
Step03_MemoryAllocation    DB  'NUMA-aware memory allocation',0
Step03_MemoryBlank         DB  'Memory blank',0
Step05_SetAffinity         DB  'Set CPU affinity mask',0
Step06_FileIO              DB  'Operations with target file',0
Step07_RestoreAffinity     DB  'Restore affinity',0
Step08_ReleaseMemory       DB  'Release memory',0
Step09_CalculateSpeed      DB  'Calculate results',0

;--- Names for parameters ---
Parm01_FilePath            DB  0Dh,0Ah, 'Paths:',0
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
SizeStarting               = MsgStartingEnd - MsgStarting
MsgStarting                DB  0Dh,0Ah, 'ASM NativeBench v0.30 for Linux ia32'
                           DB  0Dh,0Ah, '(C)2016 IC Book Labs.'
                           DB  0Dh,0Ah, 'Benchmarking...',0Dh,0Ah,0
MsgStartingEnd:
ErrorPhaseString           DB  0Dh,0Ah,'Disk benchmarks failed',0Dh,0Ah
                           DB  'Error phase: ',0
ErrorStatusString          DB  'Error status: ',0


;--- Memory size and speed units ---
U_B                        DB  'Bytes',0
U_KB                       DB  'KB',0
U_MB                       DB  'MB',0
U_GB                       DB  'GB',0
U_TB                       DB  'TB',0

;--- File paths ---
SrcFilePath                DB  SRC_PATH_STRING
DstFilePath                DB  DST_PATH_STRING

;--- Benchmarks results (pre-blanked=-1) ---
align 8

ReadBytesPerSecond         DQ  0
WriteBytesPerSecond        DQ  0
CopyBytesPerSecond         DQ  0

LinuxReadClocks            DQ  0
LinuxWriteClocks           DQ  0
LinuxCopyClocks            DQ  0

; LinuxTimeStart           DQ  0
; LinuxTimeStop            DQ  0
LinuxTimePeriod            DQ  1000000   ; femtoseconds per 1 ns (Linux Time unit)

;--- Constants for calculations ---
Const1E10                  DQ  1000000000 * 10    ; Constant for fs to 10*us

;--- Common variables/constants ---
BlockSize                  DD  BLOCK_SIZE           ; Size of one blocks, bytes
BlockCount                 DD  BLOCK_COUNT          ; Number of blocks per file               
MeasureRepeats             DD  MEASURE_REPEATS      ; Measurement repeats
NumaNode                   DD  NUMA_NODE            ; NUMA node number
FileFlags                  DD  FILE_FLAGS           ; File performance flags
CpuAffinity                DQ  CPU_AFFINITY,0,0,0   ; New affinity mask
OrigAffinity               DQ  0,0,0,0              ; Original affinity mask
MemoryBase                 DQ  ?       ; This changed by alignment requirements
TrueMemoryBase             DQ  ?       ; This required for memory block release
MemorySize                 DQ  ?       ; Size of allocated memory block
TscFrequency               DQ  ?       ; CPU TSC Frequency, Hz
TscPeriod                  DQ  ?       ; CPU TSC Period, picoseconds

ErrorStep                  DD  ?       ; Step ID for error reporting
ErrorSubStep               DD  ?       ; Sub-step or API status code for error reporting

;--- Transit buffer for text ---
TextBuffer                 DB  1024 DUP (?)       ; Buffer for text block built

;---------- End ---------------------------------------------------------------;


