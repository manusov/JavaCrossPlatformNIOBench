;==============================================================================;
;                                                                              ;
;   Disk Read and Write Benchmarks (engineering release). Linux 64 Edition.    ; 
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

; Linux 64 calling convention notes.
; 1) List of syscall numbers: /usr/src/linux/include/asm-x86_64/unistd.h
; Main API information source is man7.org.
;
; 2) Parameters order for system calls
;    (from parameter #7 used stack):
; stack  ; ...
; r9     ; 6th param
; r8     ; 5th param
; r10    ; 4th param
; rdx    ; 3rd param
; rsi    ; 2nd param
; rdi    ; 1st param
; eax    ; syscall_number
; syscall
; Return = rax.
;
; 3) Parameters order for non-system calls
;   (from parameter #7(integer) or #9(float) used stack: 
; stack        ; ...
; stack  xmm7  ; 8th param
; stack  xmm6  ; 7th param
; r9     xmm5  ; 6th param
; r8     xmm4  ; 5th param
; rcx    xmm3  ; 4th param
; rdx    xmm2  ; 3rd param
; rsi    xmm1  ; 2nd param
; rdi    xmm0  ; 1st param
; Return = rax.
;
; 4) Volatile registers:   RAX, RCX, RDX, RSI, RDI, R8-R11, ST(0)-ST(7), K0-K7, 
;                          XMM0-XMM15 / YMM0-YMM15 / ZMM0-ZMM31
; Non-volatile registers:  RBX, RBP, R12-R15
;
; 5) Note 32-bit operations (example: MOV EDX,3) also clear bits D[63-32] of
; target registers and can be used instead 64-operations (example: MOV RDX,3)
; for save space. This note actual not for Linux only, for x64 total.

;---------- Program sample setup variables ------------------------------------; 

BLOCK_SIZE       EQU  1024*1024*1        ; Disk block size, bytes
BLOCK_COUNT      EQU  100                ; Number of disk blocks
MEASURE_REPEATS  EQU  5                  ; Measurement repeats
NUMA_NODE        EQU  0                  ; NUMA node memory alloc.
CPU_AFFINITY     EQU  00                 ; Mask or 0=Skip

; SRC_PATH_STRING  EQU  'SRC_FILE.TMP',0   ; Source file path
; DST_PATH_STRING  EQU  'DST_FILE.TMP',0   ; Destination file path
SRC_PATH_STRING  EQU  'testfile1.txt',0
DST_PATH_STRING  EQU  'testfile2.txt',0

; + 00100000h , make slow WRITE and COPY, but READ fast
; + 00004000h , make slow WRITE, READ, COPY
; + 00001000h , make slow WRITE and COPY, but READ fast
; + 00000400h , make slow WRITE and COPY, but READ fast (effect small)
; https://doc.rust-lang.org/time/src/libc/unix/notbsd/linux/other/b64/x86_64.rs.html

FILE_FLAGS       EQU  00000042h + 00004000h

SYS_READ         = 0      ; Linux API functions (syscall numbers)
SYS_WRITE        = 1
SYS_OPEN         = 2
SYS_CLOSE        = 3
SYS_LSEEK        = 8
SYS_MMAP         = 9
SYS_MUNMAP       = 11
SYS_NANOSLEEP    = 35
SYS_EXIT         = 60
SYS_UNLINK       = 87
SYS_GETTIME      = 228
SYS_GETRES       = 229
SYS_SETAFFINITY  = 203
SYS_GETAFFINITY  = 204
SYS_SETMEMPOLICY = 238    ; Note alternative codes
SYS_GETMEMPOLICY = 239    ; Note alternative codes

format ELF64 executable 3
segment readable executable
entry $

;---------- Code.Initialization --------------------------------------------;
EntryPoint:

;--- Message "Starting..." -------------------------------------------------;

mov edi,1                  ; Parm#1 = RDI = Device = STDOUT
lea rsi,[MsgStarting]      ; Parm#2 = RSI = Pointer to message string
mov edx,SizeStarting       ; Parm#3 = RDX = Length
mov eax,SYS_WRITE          ; Parm#4 = EAX = Linux API function (syscall number)
syscall

;--- (1) --- Check CPU features and measure CPU clock ----------------------;

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

;--- (2) --- Check OS timer, get OS timer resolution -----------------------;

; Parameter clock ID
; #define CLOCK_REALTIME   0
; #ifdef __BSD_VISIBLE
; #define CLOCK_VIRTUAL    1
; #define CLOCK_PROF       2
; #endif
; #define CLOCK_MONOTONIC  4

lea r15,[Step02_CheckOsTimer]
xor edi,edi                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
lea rsi,[TimespecResolution]   ; Parm#2 = Pointer to output DQ sec, ns
mov eax,SYS_GETRES             ; Function code
push rsi
syscall
pop rsi
test rax,rax
xchg r14,rax
jnz ErrorProgram               ; Go if error returned
mov r14d,1
cmp qword [rsi+00],0
jne ErrorProgram               ; Go if seconds > 0 , time units too big
inc r14d
mov rax,[rsi+08]
test rax,rax
jz ErrorProgram                ; Go if nanoseconds = 0 , time units zero
cmp rax,1000000
ja ErrorProgram                ; Go if nanoseconds > 1000000 , time units too big

;--- (3) --- NUMA-aware memory allocation  ---------------------------------;

; BUG.
; lea r15,[Step03_MemoryAllocation] ; duplicated later
; ;--- Set thread memory policy, must be before allocation ---
; xor edi,edi                  ; Parm#1 = MPOL_DEFAULT = 0, check this setting is optimal?
; mov esi,8                    ; Parm#2 = mask size, bytes
; lea rdx,[CpuAffinity]        ; Parm#3 = Pointer to affinity mask
; mov eax,SYS_SETMEMPOLICY
; syscall
; test rax,rax
; xchg r14,rax
; jnz ErrorProgram

PROT_READ    = 1
PROT_WRITE   = 2
PROT_EXEC    = 4
PROT_RW      = PROT_READ + PROT_WRITE

MAP_SHARED   = 1
MAP_PROVATE  = 2
MAP_ANONYMOS = 20h
MAP_MEM      = MAP_ANONYMOS + MAP_SHARED

;--- Memory allocation ----
lea r15,[Step03_MemoryAllocation]
xor r9d,r9d                       ; R9  = Parm#6 = File offset, not used, no file, set 0
xor r8d,r8d                       ; R8  = Parm#5 = File descriptor, not used, no file, set 0
mov r10d,MAP_MEM                  ; R10 = Parm#4 = Flags, anonymos means don't load file
mov edx,PROT_RW                   ; RDX = Parm#3 = Protection attributes: Readable/Writeable/Executable

; 4096 for Page Alignment reserved

mov esi,( BLOCK_SIZE+2048 + 4096 ) * 2   ; RSI = Parm#2 = Size of block, bytes, 100000h = 1MB
mov [MemorySize],rsi
xor edi,edi                       ; RDI = Parm#1 = Base address of block, 0 = Must be selected by OS
mov eax,SYS_MMAP                  ; EAX = Syscall Number = sys_mmap
syscall                           ; Return mapped region 64-bit address at RAX or status if error
test rax,rax
mov r14,rax
js ErrorProgram                   ; Go exit if error detected
mov [TrueMemoryBase],rax

;--- now alignment in the callee ---
; test eax,0FFFh
; jz @f
; and rax,0FFFFFFFFFFFFF000h
; add rax,00000000000001000h  ; This for size alignment
; @@:

mov [MemoryBase],rax

;--- (4) --- Memory blank --------------------------------------------------;
; No error branches for this operation, error reporting is reserved

cld
mov rdi,[MemoryBase]         ; RDI = Block base
mov rcx,[MemorySize]         ; RCX = Block size, bytes count
shr rcx,3                    ; RCX = QWords count = Bytes count / 8
mov rax,'DATA    '           ; This data for fill buffer
rep stosq

;--- (5) --- Set affinity mask ---------------------------------------------;

lea r15,[Step05_SetAffinity]
cmp [CpuAffinity],0
je @f
;--- Get current affinity mask ---
xor edi,edi                  ; Parm#1 = 0, means current thread for affinity mask operation
mov esi,8                    ; Parm#2 = mask size, bytes
lea rdx,[OrigAffinity]       ; Parm#3 = Pointer to affinity mask
mov eax,SYS_GETAFFINITY      ; Function = Get current affinity mask
syscall
;--- Check output: mask size, bytes ---
mov r14,rax
cmp rax,8
jb ErrorProgram
cmp rax,32
ja ErrorProgram
;--- Set new affinity mask ---
xor edi,edi                  ; Parm#1 = 0, means current thread for affinity mask operation
mov esi,8                    ; Parm#2 = mask size, bytes
lea rdx,[CpuAffinity]        ; Parm#3 = Pointer to affinity mask
mov eax,SYS_SETAFFINITY      ; Function = Set new affinity mask
syscall
;--- Check output: status ---
test rax,rax
xchg r14,rax
jnz ErrorProgram
;--- Skip point ---
@@:

;--- (6) --- Operations with target file -----------------------------------;

mov rdi,[MemoryBase]                    ; RDI = Pointer to Input Parms. Block
;- add rdi,2048

xor eax,eax
stosq
mov eax,[BlockSize]
stosq
mov eax,[BlockCount]
stosq
mov eax,[FileFlags]
stosq
stosq
mov eax,[MeasureRepeats]
stosq

push rdi
lea rsi,[SrcFilePath]
@@:
lodsb
stosb
cmp al,0
jne @b
pop rdi

lea rsi,[DstFilePath]
add rdi,976
@@:
lodsb
stosb
cmp al,0
jne @b

mov rsi,[MemoryBase]                 ; RSI = Pointer to Input Parameters Block
;- add rsi,2048
lea rdi,[rsi + BLOCK_SIZE + 2048 + 4096 ]   ; RDI = Pointer to Output Parameters Block
;- mov ebp,MEASURE_REPEATS
;- @@:
call API_WRITE_READ_COPY_DELETE
mov rax,[rdi+00]
add [LinuxWriteClocks],rax  ; ADD, must be pre-blanked
mov rax,[rdi+08]
add [LinuxReadClocks],rax   ; ADD, must be pre-blanked
mov rax,[rdi+16]
add [LinuxCopyClocks],rax   ; ADD, must be pre-blanked
;- dec ebp
;- jnz @b

;--- (7) --- Restore affinity ----------------------------------------------;

lea r15,[Step07_RestoreAffinity]
cmp [CpuAffinity],0
je @f                        ; Skip operation if affinity not changed
;--- Set new affinity mask ---
xor edi,edi                  ; Parm#1 = 0, means current thread for affinity mask operation
mov esi,8                    ; Parm#2 = mask size, bytes
lea rdx,[OrigAffinity]       ; Parm#3 = Pointer to affinity mask
mov eax,SYS_SETAFFINITY      ; Function = Set new (original) affinity mask
syscall
;--- Check output: status ---
test rax,rax
xchg r14,rax
jnz ErrorProgram
;--- Skip point ---
@@:

;--- (8) --- Release memory ------------------------------------------------;

lea r15,[Step08_ReleaseMemory]
mov rdi,[TrueMemoryBase]          ; Parm#1 = Base address, 0 means auto selection
mov esi,( BLOCK_SIZE+2048+4096 ) * 2   ; Parm#2 = Block size, bytes
mov eax,SYS_MUNMAP                ; Function code = memory unmap (release)
syscall
test rax,rax
xchg r14,rax
jnz ErrorProgram

;--- (9) --- Calculate results ---------------------------------------------;

;--- Detect x87 presence ---
lea r15,[Step09_CalculateSpeed]
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

;--- (9) --- Prepare results for visual ------------------------------------;

lea rsi,[Parm01_FilePath]  ; Paths
lea rdi,[TextBuffer]
call ItemWrite
push rsi
lea rsi,[SrcFilePath]
call ItemWrite
mov ax,', '
stosw
lea rsi,[DstFilePath]
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

call ItemWrite_CRLF        ; Benchmarks results: Copy speed
mov rax,[CopyBytesPerSecond]
test rax,rax
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

mov eax,00000A0Dh          ; CR, LF and 0 = termination sequence of strings
stosw
stosd

;--- (10) --- Output parameters strings, console mode ----------------------;

ConsoleWrite:

lea rsi,[TextBuffer]       ; Pointer to data for output
push rsi
xor edx,edx                ; RDX = Number of chars, 0=prepare for count
@@:
lodsb
cmp al,0
je @f
inc edx                    ; RDX is input parameter for next called API
jmp @b
@@:
pop rsi
mov edi,1                  ; STDOUT
mov eax,SYS_WRITE          ; EAX = Linux API function (syscall number)
syscall

;--- (11) --- Exit program -------------------------------------------------;

ExitProgram:

xor edi,edi                  ; exit code 0
mov eax,SYS_EXIT             ; EAX = Linux API function (syscall number)
syscall

;--- (12) --- Exit point for error exit ------------------------------------;
; Reserved for add functionality, restore system context:
; release memory, close handles, restore affinity
; Terminology notes: MB = Message Box
; R15 = Pointer to action phase description string
; R14 = Error code, operation-specific, get from OS API or sub-step ID

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
call HexPrint64
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
MsgStarting                DB  0Dh,0Ah, 'ASM NativeBench v0.31 for Linux x64'
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

;--- Transit buffer for text ---
TextBuffer                 DB  1024 DUP (?)       ; Buffer for text block built

;---------- End ---------------------------------------------------------------;


