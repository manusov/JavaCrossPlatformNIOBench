;------------------------------------------------------------------------------;
;              Test for Win64 PAL (Platform Abstraction Layer)                 ;
;------------------------------------------------------------------------------;

include 'win64a.inc'
format PE64 GUI 5.0
entry start

;---------- Code section -------------------------------------------------------
section '.text' code readable executable
start:
;--- Initializing context ---
sub rsp,8*5                 ; Create and align stack frame

;========== For test ==========================================================;
; mov rax,1234h

; lea rdi,[_Buffer]
; mov qword [rdi],055AAh
; call CHECK_RDRAND
; mov rax,[rdi]

; lea rsi,[_Buffer]
; mov rdi,rsi
; mov dword [rsi+8],3
; call GET_RANDOM_DATA

;==============================================================================;

; File performance flags
FILE_FLAGS EQU \
FILE_ATTRIBUTE_NORMAL + FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH

lea rcx,[FilePath]                     ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ OR GENERIC_WRITE  ; RDX = Parm#2 = Desired access
xor r8d,r8d                            ; R8  = Parm#3 = Share mode = 0
xor r9d,r9d                            ; R9  = Parm#4 = Security attributes = 0
push r9                                ; This for stack alignment
push r9                                ; Parm#7 = Template file handle, not used 
mov eax,FILE_FLAGS
push rax                               ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                     ; Parm#5 = Creation disposition
sub rsp,32
call [CreateFileA]
add rsp,32+32
test rax,rax                           ; Check RAX = Handle

jmp Skip_Parms
FilePath  DB  'mytestfile64.tmp',0
Skip_Parms:

;==============================================================================;

jmp Skip_Sub

CHECK_RDRAND:
push rbx rbp
xor ebp,ebp
;--- Check for ID bit writeable for "1" ---
mov ebx,21
pushf           ; In the 64-bit mode, push RFLAGS
pop rax
bts eax,ebx     ; Set EAX.21=1
push rax        
popf            ; Load RFLAGS with RFLAGS.21=1
pushf           ; Store RFLAGS
pop rax         ; Load RFLAGS to RAX
btr eax,ebx     ; Check EAX.21=1, Set EAX.21=0
jnc @f          ; Go error branch if cannot set EFLAGS.21=1
;--- Check for ID bit writeable for "0" ---
push rax
popf            ; Load RFLAGS with RFLAGS.21=0
pushf           ; Store RFLAGS
pop rax         ; Load RFLAGS to RAX
btr eax,ebx     ; Check EAX.21=0
jc @f           ; Go if cannot set EFLAGS.21=0
;--- Check maximum supported function number ---
xor eax,eax
cpuid
cmp eax,1
jb @f           ; Go if CPUID function #1 not supported
;--- Execute CPUID function #1 ---
mov eax,1
cpuid
bt ecx,30
jnc @f          ; Go if RDRAND not supported
;--- Exit points ---
inc ebp
@@:
mov [rdi],rbp
pop rbp rbx
ret

GET_RANDOM_DATA:
mov rdx,rdi
mov ecx,[rsi+08]
jrcxz .L1 
.L0:
rdrand rax
jnc .L0
mov [rdx],rax
add rdx,8
dec ecx
jnz .L0
.L1:
ret

Skip_Sub:

;==============================================================================;

;--- Push registers ---
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
library kernel32,'KERNEL32.DLL',user32,'USER32.DLL'
;--- OS functions ---
include 'api\kernel32.inc'    ; KERNEL32.DLL required because ExitProcess used
include 'api\user32.inc'      ; USER32.DLL required because MessageBoxA used

