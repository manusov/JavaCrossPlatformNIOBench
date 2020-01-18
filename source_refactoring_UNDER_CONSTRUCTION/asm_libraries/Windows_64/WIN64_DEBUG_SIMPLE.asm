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
mov IPB_REQUEST_SIZE, 16384
mov IPB_BLOCK_SIZE,   4096
;- mov eax,FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH + FILE_ATTRIBUTE_NORMAL
;- mov IPB_SRC_ATTRIBUTES,rax
mov IPB_SRC_ATTRIBUTES,00000011b
mov IPB_ITERATIONS,5
lea rcx,[ReadFileName]
lea rdx,IPB_SRC_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
call MeasureReadFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
;- mov eax,FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH + FILE_ATTRIBUTE_NORMAL
;- mov IPB_SRC_ATTRIBUTES,rax
mov IPB_SRC_ATTRIBUTES,00000011b
mov IPB_ITERATIONS,1            ; 5
; lea rcx,[WriteFileName]
  lea rcx,[ReadFileName]
lea rdx,IPB_SRC_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
call MeasureWriteFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
;- mov eax,FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH + FILE_ATTRIBUTE_NORMAL
;- mov IPB_SRC_ATTRIBUTES,rax
mov IPB_SRC_ATTRIBUTES,00000011b
mov IPB_DST_ATTRIBUTES,00000011b
mov IPB_ITERATIONS,5
lea rcx,[ReadFileName]
lea rdx,IPB_SRC_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
lea rcx,[WriteFileName]
lea rdx,IPB_DST_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
call MeasureCopyFile
;---
;; lea rcx,[ReadFileName]
;  lea rcx,[WriteFileName]
;lea rdx,IPB_SRC_PATH
;@@:
;mov al,[rcx]
;mov [rdx],al
;inc rcx
;inc rdx
;cmp al,0
;jne @b
;call MeasureDeleteFile
;--- Exit OS ---
xor ecx,ecx
call [ExitProcess]
;---------- Library main functionality ----------------------------------------;
include 'include\BaseRoutines.inc'
include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureDeleteFile.inc'
;---------- Data section ------------------------------------------------------;
section '.data' data readable writeable
ReadFileName   DB  'C:\TEMP\a1.bin',0
WriteFileName  DB  'C:\TEMP\a2.bin',0
align 4096
IPB       DB  4096      DUP (?)
OPB       DB  4096      DUP (?)
BUFFER    DB  1024*1024 DUP (?)  ; 16384 DUP (?)
BUFALIGN  DB  4096      DUP (?)
;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library user32, 'USER32.DLL', kernel32, 'KERNEL32.DLL', gdi32, 'GDI32.DLL'
include 'api\user32.inc'    ; Win API, user interface
include 'api\gdi32.inc'     ; Win API, graphice 
include 'api\kernel32.inc'  ; Win API, OS standard kernel functions

