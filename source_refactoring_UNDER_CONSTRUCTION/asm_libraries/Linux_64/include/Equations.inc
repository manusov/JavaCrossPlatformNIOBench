;------------------------------------------------------------------------------;
;                Equations for native file I/O library.                        ;
;------------------------------------------------------------------------------;
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
;---------- Equations for Linux API functions ---------------------------------;
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
;--- Constants equations ------------------------------------------------------; 
FILE_FLAGS_BLANK    EQU  00000042h
FILE_FLAG_O_DIRECT  EQU  00004000h
FILE_SHARE_MODE     EQU  -1         ; note old value is 00000C00h required sudo
;---------- File I/O benchmarking support functions, equations ----------------;
; IPB (Input Parameters Block) layout declaration
IPB_REQUEST_ID      EQU  qword [r14 + 00] 
IPB_REQUEST_SIZE    EQU  qword [r14 + 08]
IPB_BLOCK_SIZE      EQU  qword [r14 + 16]
IPB_SRC_ATTRIBUTES  EQU  qword [r14 + 24]
IPB_DST_ATTRIBUTES  EQU  qword [r14 + 32]
IPB_ITERATIONS      EQU  qword [r14 + 40]
IPB_SRC_PATH        EQU  [r14 + 1024]
IPB_DST_PATH        EQU  [r14 + 2048]
; OPB (Output Parameters Block) layout declaration
OPB_BUFFER_BASE     EQU  qword [r15 + 00]
OPB_BUFFER_SIZE     EQU  qword [r15 + 08]
OPB_SRC_HANDLE      EQU  qword [r15 + 16]
OPB_DST_HANDLE      EQU  qword [r15 + 24]
OPB_OPERATION_SIZE  EQU  qword [r15 + 32]
OPB_TIMER_DELTA     EQU  qword [r15 + 40]
OPB_LAST_OPERATION  EQU  qword [r15 + 48]
OPB_LAST_ERROR      EQU  qword [r15 + 56]
OPB_DATA_ARRAY      EQU  [r15 + 4096]
OPB_CLEAR_QWORDS    EQU  9  
; Step identification constants
STEP_NONE           EQU  0
STEP_OPEN_READ      EQU  1
STEP_READ           EQU  2
STEP_OPEN_WRITE     EQU  3
STEP_WRITE          EQU  4
STEP_CLOSE          EQU  5
STEP_DELETE         EQU  6
; transit fields
OPB_TRANSIT_T1      EQU  [r15 + 80]
OPB_TRANSIT_T1_S    EQU  qword [r15 + 80]
OPB_TRANSIT_T1_NS   EQU  qword [r15 + 88]
OPB_TRANSIT_T2      EQU  [r15 + 96]
OPB_TRANSIT_T2_S    EQU  qword [r15 + 96]
OPB_TRANSIT_T2_NS   EQU  qword [r15 + 104]
OPB_TRANSIT_SEEK    EQU  [r15 + 112]  ; usage removed

