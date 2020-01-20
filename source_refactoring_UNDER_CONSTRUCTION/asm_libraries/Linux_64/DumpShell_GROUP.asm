;---------------------------------------------------------------------------;
;                          Registers Dump Sample                            ;
;                            Linux x64 version                              ;
;                       Customized for debug file I/O.                      ;
;---------------------------------------------------------------------------;
; syscall numbers: /usr/src/linux/include/asm-x86_64/unistd.h
; parameters order:
; r9    ; 6th param
; r8    ; 5th param
; r10   ; 4th param
; rdx   ; 3rd param
; rsi   ; 2nd param
; rdi   ; 1st param
; eax   ; syscall_number
; syscall

; Note 32-bit operations (example: MOV EDX,3) also clear bits D[63-32] of
; target registers and can be used instead 64-operations (example: MOV RDX,3)
; for save space.
;---
; BUG WITH FILE_FLAGS AND RW FLAGS
; MUST RETURN ERROR WHEN NOT EXISTED, 
; VERIFY ALL BITMAPS FOR OPEN/CREATE FILES FLAGS
;--- Equations ---
SYS_WRITE     = 1    ; Linux API functions (syscall numbers)
SYS_NANOSLEEP = 35
SYS_EXIT      = 60
include 'include\BaseEquations.inc'
;---
DUMP_REGION = OPB
DUMP_TYPE   = 4      ; 0=None, 1=8bit, 2=16bit, 3=32bit, 
                     ; 4=64bit (as hex), 5=64bit (as double precision)
;---
format ELF64 executable 3
segment readable executable
entry $
;---------- Code.Initialization --------------------------------------------;
EntryPoint:
;--- Fragment for debug file I/O ---
lea rsi,[IPB]
lea rdi,[OPB]
mov r14,rsi
mov r15,rdi
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


;---------- Code.Memory dump -----------------------------------------------;
call WriteLF                 ; Console output: Line Feed
IF DUMP_TYPE > 0
;--- Prepare for dump write ---
mov rbx,DUMP_REGION          ; RBX = Base address
xor ebp,ebp                  ; EBP = 0, Counter for dump address write
;--- Dump write: address and data ---
MemoryDumpStrings:           ; Start cycle for write strings of dump
mov rdi,StringBuffer
mov rsi,rdi
mov eax,ebp
call HexPrint16              ; Print address
mov edx,4
call StringWrite1
mov rsi,StringInterval
mov edx,3
call StringWrite1
;---
IF DUMP_TYPE = 1             ; Dump for 8-bit, hex
mov ecx,16
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsb
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint8
mov al,' '
stosb
mov edx,3
call StringWrite1
loop MemoryDump              ; End cycle for write bytes at dump string
END IF
;---
IF DUMP_TYPE = 2             ; Dump for 16-bit, hex
mov ecx,8
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsw
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint16
mov al,' '
stosb
mov edx,5
call StringWrite1
loop MemoryDump              ; End cycle for write bytes at dump string
END IF
;---
IF DUMP_TYPE = 3             ; Dump for 32-bit, hex
mov ecx,4
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsd
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint32
mov al,' '
stosb
mov edx,9
call StringWrite1
loop MemoryDump              ; End cycle for write bytes at dump string
END IF
;---
IF DUMP_TYPE = 4             ; Dump for 64-bit, hex
mov ecx,2
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsq
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint64
mov al,' '
stosb
mov edx,17
call StringWrite1
loop MemoryDump              ; End cycle for write bytes at dump string
END IF
;---
IF DUMP_TYPE = 5             ; Dump for double precision
mov ecx,4
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rdi,StringBuffer
mov rsi,rdi
push rcx rdi
mov cl,16                    ; At this point RCX.[63-08]=0
mov al,' '
rep stosb
pop rdi rcx
mov rax,[rbx]
add rbx,8
call FloatingPrint64
mov edx,16
call StringWrite1
loop MemoryDump              ; End cycle for write bytes at dump string
END IF
;---
call WriteLF
;---
IF DUMP_TYPE < 5
add ebp,0010h
cmp ebp,0100h
ELSE
add ebp,0020h
cmp ebp,0100h
END IF
;---
jb MemoryDumpStrings         ; End cycle for write strings of dump
call WriteLF
;--- End of memory dump write ---
END IF
;---------- Exit to Operating System ---------------------------------------;
xor edi,edi                  ; exit code 0
mov eax,SYS_EXIT             ; EAX = Linux API function (syscall number)
syscall
;---------- Code.Subroutines -----------------------------------------------;

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

;---------------------------------------------------;
; Write decimal parameter with validity checks      ;
; Invalid if zero and if 32-bit overflow            ;
; INPUT:   RSI = Pointer to parameter name string   ;
;          RAX = Parameter value                    ;
; OUTPUT:  None                                     ;
;---------------------------------------------------;
ParameterWrite:
push rax
call StringWrite1
pop rax
test eax,eax
jz BadParameter
mov rdx,0FFFFFFFFh
cmp rax,rdx
ja BadParameter
mov rdi,StringBuffer
push rdi
xor esi,esi
call DecimalPrint32_1
pop rsi
mov rdx,rdi
sub rdx,rsi
jmp ValidParameter
BadParameter:
mov rsi,StringUnknown
mov edx,1
ValidParameter:
; No RET, continue in the next subroutine
jmp StringWrite1

;---------------------------------------------------;
; Text string console output with LF = Line Feed    ;
; INPUT:  RSI = String base address                 ;
;         RDX = String size                         ;
; OUTPUT: None                                      ;
;---------------------------------------------------;
StringWriteLF:
call StringWrite1
; No RET, continue in the next subroutine
;---------------------------------------------------;
; LF = Line Feed console output                     ;
; INPUT:  None                                      ;
; OUTPUT: None                                      ;
;---------------------------------------------------;
WriteLF:
mov rsi,StringLF
mov edx,1
; No RET, continue in the next subroutine
;---------------------------------------------------;
; Text string console output                        ;
; INPUT:  RSI=String base address                   ;
;         RDX=String size                           ;
; OUTPUT: None                                      ;
;---------------------------------------------------;
StringWrite1:
mov edi,1                  ; STDOUT
mov eax,SYS_WRITE          ; EAX = Linux API function (syscall number)
push rbx rcx rsi rbp       ; RBX, RCX, RSI, RBP non-volatile for this program
syscall
pop rbp rsi rcx rbx
ret
;---------- Print 32-bit Decimal Number ---------------;
; INPUT:   EAX = Number value                          ;
;          SI = Template size, chars. 0=No template    ;
;          RDI = Destination Pointer (flat)            ;
; OUTPUT:  RDI = New Destination Pointer (flat)        ;
;                modified because string write         ;
;------------------------------------------------------;
DecimalPrint32_1:
cld
push rbx rcx                 ; Note RAX, RDX is volatile
mov ebx,esi
mov bh,80h-10
add bh,bl
mov ecx,1000000000
DP32_00:
xor edx,edx
div ecx                      ; Produce current digit
and al,0Fh
test bh,bh
js DP32_02
cmp ecx,1
je DP32_02
cmp al,0                     ; Not actual left zero ?
jz DP32_03
DP32_02:
mov bh,80h                   ; Flag = 1
or al,30h
stosb                        ; Store char
DP32_03:
push rdx
xor edx,edx
mov eax,ecx
mov ecx,10
div ecx
mov ecx,eax
pop rax
inc bh
test ecx,ecx
jnz DP32_00
pop rcx rbx
DP32_Ret:
ret
;---------- Print 64-bit Hex Number ----------------;
; INPUT:  RAX = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint64:
push rax
ror rax,32
call HexPrint32
pop rax
; no RET, continue at next subroutine
;---------- Print 32-bit Hex Number ----------------;
; INPUT:  EAX = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint32:
push rax
ror eax,16
call HexPrint16
pop rax
; no RET, continue at next subroutine
;---------- Print 16-bit Hex Number ----------------;
; INPUT:  AX  = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint16:
push rax
xchg al,ah
call HexPrint8
pop rax
; no RET, continue at next subroutine
;---------- Print 8-bit Hex Number -----------------;
; INPUT:  AL  = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint8:
push rax
ror al,4
call HexPrint4
pop rax
; no RET, continue at next subroutine
;---------- Print 4-bit Hex Number -----------------;
; INPUT:  AL  = Number (bits 0-3)                   ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint4:
cld
push rax
and al,0Fh
cmp al,9
ja HP4_AF
add al,'0'
jmp HP4_Store
HP4_AF:
add al,'A'-10
HP4_Store:
stosb
pop rax
ret
;---------- Library main functionality ----------------------------------------;
include 'include\BaseRoutines.inc'
include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureDeleteFile.inc'
include 'include\PrecisionLinear.inc'
;---------- Data --------------------------------------------------------------;
segment readable writeable
;--- Text output support ---
StringInterval:
DB  '   '
StringSeparator:
DB  ' | '
StringEqual:
DB  ' = '
StringH:
DB  'h'
StringUnknown:
DB  '?'
StringLF:
DB  0Ah
StringBuffer:
DB  32 DUP (?)
DataBuffer:
DB  1024 DUP (?)
;--- File I/O debug support ---
ReadPrefix    DB  'src',0
ReadPostfix   DB  '.bin',0
WritePrefix   DB  'dst',0
WritePostfix  DB  '.bin',0
align 4096
IPB           DB  4096      DUP (?)
OPB           DB  4096      DUP (?)
BUFFER        DB  1024*1024 DUP (?)  ; 16384 DUP (?)
BUFALIGN      DB  4096      DUP (?)
