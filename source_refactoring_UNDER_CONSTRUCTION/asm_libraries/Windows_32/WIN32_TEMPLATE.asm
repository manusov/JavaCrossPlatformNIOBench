; Template

include 'win32a.inc'

format PE GUI
entry start
section '.text' code readable executable
start:

; ...
push 0
call [ExitProcess]

section '.data' data readable writeable
align 4096
BUFFER_SIZE  EQU  16384
Buffer DB BUFFER_SIZE DUP (?)

section '.idata' import data readable writeable
library user32, 'USER32.DLL', kernel32, 'KERNEL32.DLL', gdi32, 'GDI32.DLL'
include 'api\user32.inc'    ; Win API, user interface
include 'api\gdi32.inc'     ; Win API, graphice 
include 'api\kernel32.inc'  ; Win API, OS standard kernel functions

