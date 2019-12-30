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
OPB_TIMER_START       EQU  dword [edi + 40]
OPB_TIMER_STOP        EQU  dword [edi + 48]
OPB_LAST_OPERATION    EQU  dword [edi + 56]
OPB_LAST_ERROR        EQU  dword [edi + 64]
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
; additional definitions by MSDN
INVALID_SET_FILE_POINTER  EQU  -1

; additional support for 32-bit version, because small number of registers
OPB_TRANSIT_SIZE_L    EQU  dword [edi + 72]
OPB_TRANSIT_SIZE_H    EQU  dword [edi + 80]  
OPB_TRANSIT_REPEATS   EQU  dword [edi + 88]
 