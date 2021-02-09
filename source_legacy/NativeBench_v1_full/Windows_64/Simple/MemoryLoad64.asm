;------------------------------------------------------------------------------;
;                           Test caller for Win64                              ;
;------------------------------------------------------------------------------;

include 'win64a.inc'
format PE64 GUI 5.0
entry start

;---------- Code section -------------------------------------------------------
section '.text' code readable executable
start:
;--- Initializing context, test call ---
sub rsp,8*5                 ; Create and align stack frame


;--- Memory load pattern ---

xor ecx,ecx
mov rdx,1024*1024*1024 * 4
mov rbx,rdx
mov r8d,MEM_COMMIT
mov r9d,PAGE_READWRITE
call [VirtualAlloc]     ; Occupy memory
test rax,rax
jz Error

L1:              ; Cycle for repeat blocks
mov rdi,rax
mov rbp,rbx 

L0:              ; Cycle for pages in the block
mov cl,[rdi]
add rdi,4096
sub rbp,4096
ja L0

jmp L1

mov rax,-1  ; This code means no error
jmp AfterError
Error:
call [GetLastError]
AfterError:


;--- Prepare for visual, push registers ---
push r15 r14 r13 r12 r11 r10 r9 r8
push rdi rsi rbp
lea r8,[rsp+8*13]           ; R8 as RSP
push r8 rdx rcx rbx rax     
;--- Pop registers and built ASCII strings ---
cld
mov ecx,16
lea rsi,[_Message]
lea rdi,[_Buffer]
DumpRegs:
movsw
movsb
mov eax,' =  '
stosd
pop rax
call HexPrint64
mov ax,0D0Ah
stosw
loop DumpRegs
mov al,0
stosb
;--- Visualization ---
push rbp                    ; Save RBP
mov rbp,rsp                 ; Save RSP 
sub rsp,32                  ; Create parameters shadow
and rsp,0FFFFFFFFFFFFFFF0h  ; Align RSP required for API Call
xor ecx,ecx                 ; RCX = Parm #1 = Parent window
lea rdx,[_Buffer]           ; RDX = Parm #2 = Message
lea r8,[_Caption]           ; R8  = Parm #3 = Caption (upper message)
xor r9,r9                   ; R9  = Parm #4 = Message flags
call [MessageBoxA]          ; Call target function - show window
mov rsp,rbp                 ; Restore RSP
pop rbp                     ; Restore RBP
;--- Exit ---
xor ecx,ecx                   ; RCX=0, exit code
call [ExitProcess]            ; Exit from application

;--- Subroutines ---
;---------- Print 64-bit Hex Number ---------;
; INPUT:  RAX = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint64:
push rax
ror rax,32
call HexPrint32
pop rax
; no RET, continue at next subroutine
;---------- Print 32-bit Hex Number ---------;
; INPUT:  EAX = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint32:
push rax
ror eax,16
call HexPrint16
pop rax
; no RET, continue at next subroutine
;---------- Print 16-bit Hex Number ---------;
; INPUT:  AX  = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint16:
push rax
xchg al,ah
call HexPrint8
pop rax
; no RET, continue at next subroutine
;---------- Print 8-bit Hex Number ----------;
; INPUT:  AL  = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint8:
push rax
ror al,4
call HexPrint4
pop rax
; no RET, continue at next subroutine
;---------- Print 4-bit Hex Number ----------;
; INPUT:  AL  = Number (bits 0-3)            ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint4:
cld
push rax
and al,0Fh
cmp al,9
ja HP4AF
add al,'0'
jmp HP4Store
HP4AF:
add al,'A'-10
HP4Store:
stosb
pop rax
ret

;---------- Data section -------------------------------------------------------
section '.data' data readable writeable
;--- Dump support ---
_Caption  DB '  GPR dump',0
_Message  DB 'RAXRBXRCXRDXRSPRBPRSIRDI'
          DB 'R8 R9 R10R11R12R13R14R15'
_Buffer   DB 1024 DUP (?)

;---------- Import section -----------------------------------------------------
section '.idata' import data readable writeable
library kernel32, 'KERNEL32.DLL', user32, 'USER32.DLL'
include 'api\kernel32.inc'
include 'api\user32.inc'


