;---------- File I/O benchmarking support functions, equations ----------------;
; IPB (Input Parameters Block) layout declaration
IPB_REQUEST_ID      EQU  qword [rsi + 00] 
IPB_REQUEST_SIZE    EQU  qword [rsi + 08]
IPB_BLOCK_SIZE      EQU  qword [rsi + 16]
IPB_SRC_ATTRIBUTES  EQU  qword [rsi + 24]
IPB_DST_ATTRIBUTES  EQU  qword [rsi + 32]
IPB_ITERATIONS      EQU  qword [rsi + 40]
IPB_SRC_PATH        EQU  [rsi + 1024]
IPB_DST_PATH        EQU  [rsi + 2048]
; OPB (Output Parameters Block) layout declaration
OPB_BUFFER_BASE     EQU  qword [rdi + 00]
OPB_BUFFER_SIZE     EQU  qword [rdi + 08]
OPB_SRC_HANDLE      EQU  qword [rdi + 16]
OPB_DST_HANDLE      EQU  qword [rdi + 24]
OPB_OPERATION_SIZE  EQU  qword [rdi + 32]
OPB_TIMER_START     EQU  qword [rdi + 40]
OPB_TIMER_STOP      EQU  qword [rdi + 48]
OPB_LAST_OPERATION  EQU  qword [rdi + 56]
OPB_LAST_ERROR      EQU  qword [rdi + 64]
OPB_DATA_ARRAY      EQU  [rdi + 4096]
OPB_CLEAR_QWORDS    EQU  9  
; Step identification constants
STEP_NONE           EQU  0
STEP_OPEN_READ      EQU  1
STEP_READ           EQU  2
STEP_OPEN_WRITE     EQU  3
STEP_WRITE          EQU  4
STEP_CLOSE          EQU  5
STEP_DELETE         EQU  6
; Stack parameters addressing constants
PARM_SHADOW_1       EQU  qword [rsp + 00]
PARM_SHADOW_2       EQU  qword [rsp + 08]
PARM_SHADOW_3       EQU  qword [rsp + 16]
PARM_SHADOW_4       EQU  qword [rsp + 24]
PARM_5              EQU  qword [rsp + 32]
PARM_6              EQU  qword [rsp + 40]
PARM_7              EQU  qword [rsp + 48]
; additional definitions by MSDN
INVALID_SET_FILE_POINTER  EQU  -1
