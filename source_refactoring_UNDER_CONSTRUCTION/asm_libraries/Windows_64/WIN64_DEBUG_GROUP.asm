;------------------------------------------------------------------------------;
;     Module for debug, this fragment used for run under debugger (FDBG).      ;    
;------------------------------------------------------------------------------;
include 'win64a.inc'
include 'include\BaseEquations.inc'
format PE64 GUI
entry start
section '.text' code readable executable
start:
sub rsp,8*5
lea rsi,[IPB]
lea rdi,[OPB]
;---
mov IPB_REQUEST_SIZE, 65536
mov IPB_BLOCK_SIZE,   4096
mov IPB_SRC_ATTRIBUTES,00000011b
mov IPB_ITERATIONS,5
mov IPB_FILE_COUNT,20

lea rcx,[ReadPrefix]
lea rdx,IPB_SRC_PATH_PREFIX
call StringHelper
lea rcx,[ReadPostfix]
lea rdx,IPB_SRC_PATH_POSTFIX
call StringHelper
lea rcx,[WritePrefix]
lea rdx,IPB_DST_PATH_PREFIX
call StringHelper
lea rcx,[WritePostfix]
lea rdx,IPB_DST_PATH_POSTFIX
call StringHelper

call PrecisionLinear

;--- Exit OS ---
xor ecx,ecx
call [ExitProcess]
;--- Helper ---
StringHelper:
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
ret
;---------- Library main functionality ----------------------------------------;
include 'include\BaseRoutines.inc'
include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureDeleteFile.inc'
include 'include\PrecisionLinear.inc'
;---------- Data section ------------------------------------------------------;
section '.data' data readable writeable
ReadPrefix    DB  'C:\TEMP\src',0
ReadPostfix   DB  '.bin',0
WritePrefix   DB  'C:\TEMP\dst',0
WritePostfix  DB  '.bin',0
align 4096
IPB           DB  4096      DUP (?)
OPB           DB  4096      DUP (?)
BUFFER        DB  1024*1024 DUP (?)
BUFALIGN      DB  4096      DUP (?)
;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library user32, 'USER32.DLL', kernel32, 'KERNEL32.DLL', gdi32, 'GDI32.DLL'
include 'api\user32.inc'    ; Win API, user interface
include 'api\gdi32.inc'     ; Win API, graphice 
include 'api\kernel32.inc'  ; Win API, OS standard kernel functions

