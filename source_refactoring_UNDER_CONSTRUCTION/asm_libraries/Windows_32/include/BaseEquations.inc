;---------- File I/O benchmarking support functions, equations ----------------;
; IPB (Input Parameters Block) layout declaration, all high dwords reserved
IPB_REQUEST_ID        EQU  dword [esi + 00] 
IPB_REQUEST_SIZE      EQU  dword [esi + 08]
IPB_BLOCK_SIZE        EQU  dword [esi + 16]
IPB_SRC_ATTRIBUTES    EQU  dword [esi + 24]
IPB_DST_ATTRIBUTES    EQU  dword [esi + 32]
IPB_ITERATIONS        EQU  dword [esi + 40]
; IPB layout continue, fields for "Precision" operations groups functions
IPB_FILE_COUNT        EQU  dword [esi + 48]
; IPB layout continue, paths strings
IPB_SRC_PATH          EQU  [esi + 1024]
IPB_DST_PATH          EQU  [esi + 2048]
; IPB layout continue, paths strings for "Precision" ops. groups functions
IPB_SRC_PATH_PREFIX   EQU  [esi + 1024]
IPB_SRC_PATH_POSTFIX  EQU  [esi + 1024 + 512]
IPB_DST_PATH_PREFIX   EQU  [esi + 2048]
IPB_DST_PATH_POSTFIX  EQU  [esi + 2048 + 512]
; OPB (Output Parameters Block) layout declaration, all high dwords reserved
OPB_CLEAR_DWORDS      EQU  9*2
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
; OPB layout continue, fields for "Precision" operations groups functions  
OPB_HANDLES_SRC_BASE  EQU  dword [edi + 128]
OPB_HANDLES_DST_BASE  EQU  dword [edi + 136]
OPB_TIMER_READ        EQU  qword [edi + 144]
OPB_TIMER_READ_L      EQU  dword [edi + 144]
OPB_TIMER_READ_H      EQU  dword [edi + 148]
OPB_TIMER_WRITE       EQU  qword [edi + 152]
OPB_TIMER_WRITE_L     EQU  dword [edi + 152]
OPB_TIMER_WRITE_H     EQU  dword [edi + 156]
OPB_TIMER_COPY        EQU  qword [edi + 160]
OPB_TIMER_COPY_L      EQU  dword [edi + 160]
OPB_TIMER_COPY_H      EQU  dword [edi + 164]
OPB_TOTAL_READ        EQU  qword [edi + 168]
OPB_TOTAL_READ_L      EQU  dword [edi + 168]
OPB_TOTAL_READ_H      EQU  dword [edi + 172]
OPB_TOTAL_WRITE       EQU  qword [edi + 176]
OPB_TOTAL_WRITE_L     EQU  dword [edi + 176]
OPB_TOTAL_WRITE_H     EQU  dword [edi + 180]
OPB_TOTAL_COPY        EQU  qword [edi + 184]
OPB_TOTAL_COPY_L      EQU  dword [edi + 184]
OPB_TOTAL_COPY_H      EQU  dword [edi + 188]
OPB_COMPLEX_PATH      EQU  [edi + 1024] 
; Step identification constants
STEP_NONE             EQU  0
STEP_OPEN_READ        EQU  1
STEP_READ             EQU  2
STEP_OPEN_WRITE       EQU  3
STEP_WRITE            EQU  4
STEP_CLOSE            EQU  5
STEP_DELETE           EQU  6
; Scratch pad, OS-specific usage
OPB_TRANSIT_T1        EQU  qword [edi + 80]
OPB_TRANSIT_T1_L      EQU  dword [edi + 80]
OPB_TRANSIT_T1_H      EQU  dword [edi + 84]
OPB_TRANSIT_T2        EQU  qword [edi + 88]
OPB_TRANSIT_T2_L      EQU  dword [edi + 88]
OPB_TRANSIT_T2_H      EQU  dword [edi + 92]
; additional support for 32-bit version, because small number of registers
OPB_TRANSIT_SIZE_L    EQU  dword [edi + 96]
OPB_TRANSIT_SIZE_H    EQU  dword [edi + 100]  
OPB_TRANSIT_REPEATS   EQU  dword [edi + 104]
OPB_TRANSIT_BLOCK     EQU  dword [edi + 108]
OPB_TRANSIT_SIZE      EQU  dword [edi + 112]
; additional definitions by MSDN
INVALID_SET_FILE_POINTER  EQU  -1
 