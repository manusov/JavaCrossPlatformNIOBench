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

;--- Constants equations ------------------------------------------------------; 
FILE_FLAGS_BLANK    EQU  00000042h
FILE_FLAG_O_DIRECT  EQU  00004000h
FILE_SHARE_MODE     EQU  -1         ; note old value is 00000C00h required sudo
;---------- File I/O benchmarking support functions, equations ----------------;
; IPB (Input Parameters Block) layout declaration, all high dwords reserved
IPB_REQUEST_ID        EQU  dword [esi + 00] 
IPB_REQUEST_SIZE      EQU  dword [esi + 08]
IPB_BLOCK_SIZE        EQU  dword [esi + 16]
IPB_SRC_ATTRIBUTES    EQU  dword [esi + 24]
IPB_DST_ATTRIBUTES    EQU  dword [esi + 32]
IPB_ITERATIONS        EQU  dword [esi + 40]
IPB_SRC_PATH          EQU  [esi + 1024]
IPB_DST_PATH          EQU  [esi + 2048]
; OPB (Output Parameters Block) layout declaration, all high dwords reserved
OPB_BUFFER_BASE       EQU  dword [edi + 00]
OPB_BUFFER_SIZE       EQU  dword [edi + 08]
OPB_SRC_HANDLE        EQU  dword [edi + 16]
OPB_DST_HANDLE        EQU  dword [edi + 24]
OPB_OPERATION_SIZE_L  EQU  dword [edi + 32]
OPB_OPERATION_SIZE_H  EQU  dword [edi + 36]
OPB_TIMER_DELTA_L     EQU  dword [edi + 40]
OPB_TIMER_DELTA_H     EQU  dword [edi + 44]
OPB_LAST_OPERATION    EQU  dword [edi + 48]
OPB_LAST_ERROR        EQU  dword [edi + 56]
OPB_DATA_ARRAY        EQU  [edi + 4096]
OPB_CLEAR_DWORDS      EQU  9*2  
; Step identification constants
STEP_NONE             EQU  0
STEP_OPEN_READ        EQU  1
STEP_READ             EQU  2
STEP_OPEN_WRITE       EQU  3
STEP_WRITE            EQU  4
STEP_CLOSE            EQU  5
STEP_DELETE           EQU  6
; additional support for 32-bit version, because small number of registers
OPB_TRANSIT_SIZE_L    EQU  dword [edi + 72]
OPB_TRANSIT_SIZE_H    EQU  dword [edi + 76]  
OPB_TRANSIT_REPEATS   EQU  dword [edi + 80]
; transit fields
OPB_TRANSIT_T1        EQU  [edi + 88]
OPB_TRANSIT_T1_S      EQU  dword [edi + 88]
OPB_TRANSIT_T1_NS     EQU  dword [edi + 92]
OPB_TRANSIT_T2        EQU  [edi + 104]
OPB_TRANSIT_T2_S      EQU  dword [edi + 104]
OPB_TRANSIT_T2_NS     EQU  dword [edi + 108]
OPB_TRANSIT_SEEK      EQU  [edi + 120]  ; usage removed
; this required for restore ESI, EDI, 
; because temporary used by Linux 32 calling convention
BACKUP_ESI            EQU  dword [esp + 08]
BACKUP_EDI            EQU  dword [esp + 04]
