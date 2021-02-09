;------------------------------------------------------------------------------;
;     Module for debug, this fragment used for run under debugger (OllyDbg).   ;    
;------------------------------------------------------------------------------;
include 'win32a.inc'
include 'include\BaseEquations.inc'
format PE GUI
entry start
section '.text' code readable executable
start:
lea esi,[IPB]
lea edi,[OPB]
;---

mov IPB_REQUEST_SIZE, 65536
mov IPB_BLOCK_SIZE,   4096
mov IPB_SRC_ATTRIBUTES,00000011b
mov IPB_ITERATIONS,5
mov IPB_FILE_COUNT,20

lea ecx,[ReadPrefix]
lea edx,IPB_SRC_PATH_PREFIX
call StringHelper
lea ecx,[ReadPostfix]
lea edx,IPB_SRC_PATH_POSTFIX
call StringHelper
lea ecx,[WritePrefix]
lea edx,IPB_DST_PATH_PREFIX
call StringHelper
lea ecx,[WritePostfix]
lea edx,IPB_DST_PATH_POSTFIX
call StringHelper

call PrecisionLinear

;--- Exit OS ---
push 0
call [ExitProcess]
;--- Helper ---
StringHelper:
@@:
mov al,[ecx]
mov [edx],al
inc ecx
inc edx
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
BUFFER        DB  1024*1024 DUP (?)  ; 16384 DUP (?)
BUFALIGN      DB  4096      DUP (?)
;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library user32, 'USER32.DLL', kernel32, 'KERNEL32.DLL', gdi32, 'GDI32.DLL'
include 'api\user32.inc'    ; Win API, user interface
include 'api\gdi32.inc'     ; Win API, graphice 
include 'api\kernel32.inc'  ; Win API, OS standard kernel functions

