;------------------------------------------------------------------------------;
;              Test for Win32 PAL (Platform Abstraction Layer)                 ;
;------------------------------------------------------------------------------;

include 'win32a.inc'
format PE GUI 4.0
entry start

section '.text' code readable executable
start:

;========== For test ==========================================================;
; mov eax,1234h

; lea edi,[_Buffer]
; mov dword [edi],055AAh
; call CHECK_RDRAND
; mov eax,[edi]

; lea esi,[_Buffer]
; mov edi,esi
; mov dword [esi+8],3
; call GET_RANDOM_DATA

;==============================================================================;

; File performance flags
FILE_FLAGS EQU \
FILE_ATTRIBUTE_NORMAL + FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH

xor eax,eax                         ; EAX=0 for compact next instructions
push eax                            ; Parm#7 = Template file handle, not used 
push FILE_FLAGS                     ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                  ; Parm#5 = Creation disposition
push eax                            ; Parm#4 = Security attributes = 0
push eax                            ; Parm#3 = Share mode = 0
push GENERIC_READ OR GENERIC_WRITE  ; Parm#2 = Desired access
push FilePath                       ; Parm#1 = Pointer to file path
call [CreateFileA]
test eax,eax                        ; Check EAX = Handle

jmp Skip_Parms
FilePath  DB  'mytestfile32.tmp',0
Skip_Parms:

;==============================================================================;

jmp Skip_Sub

CHECK_RDRAND:
push ebx ebp
xor ebp,ebp
;--- Check for ID bit writeable for "1" ---
mov ebx,21
pushf           ; In the 64-bit mode, push RFLAGS
pop eax
bts eax,ebx     ; Set EAX.21=1
push eax        
popf            ; Load EFLAGS with EFLAGS.21=1
pushf           ; Store EFLAGS
pop eax         ; Load EFLAGS to RAX
btr eax,ebx     ; Check EAX.21=1, Set EAX.21=0
jnc @f          ; Go error branch if cannot set EFLAGS.21=1
;--- Check for ID bit writeable for "0" ---
push eax
popf            ; Load EFLAGS with EFLAGS.21=0
pushf           ; Store EFLAGS
pop eax         ; Load EFLAGS to EAX
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
mov dword [edi+00],ebp
mov dword [edi+04],0
pop ebp ebx
ret

GET_RANDOM_DATA:
mov edx,edi
mov ecx,[esi+08]
jecxz .L2 
;--- Low dword ---
.L0:
rdrand eax
jnc .L0
mov [edx],eax
;--- High dword ---
.L1:
rdrand eax
jnc .L1
mov [edx+4],eax
;--- Cycle ---
add edx,8
dec ecx
jnz .L0
.L2:
ret

Skip_Sub:

;==============================================================================;

;--- Push registers ---
push edi esi ebp
lea ebp,[esp+4*3]            ; EBP as ESP
push ebp edx ecx ebx eax     
;--- Pop registers and built ASCII strings ---
cld
mov ecx,8
lea esi,[_Message]
lea edi,[_Buffer]
DumpRegs:
movsw
movsb
mov eax,' =  '
stosd
pop eax
call HexPrint32
mov ax,0D0Ah
stosw
loop DumpRegs
mov al,0
stosb
;--- Visualization ---
push 0                       ; Parm #4 = Message flags
push _Caption                ; Parm #3 = Caption (upper message)
push _Buffer                 ; Parm #2 = Message
push 0                       ; Parm #1 = Parent window
call [MessageBoxA]           ; Call target function - show window
;--- Exit ---
push 0                       ; Parm#1 = 0, exit code
call [ExitProcess]           ; Exit from application

;--- Subroutines ---
;---------- Print 32-bit Hex Number ---------;
; INPUT:  EAX = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint32:
push eax
ror eax,16
call HexPrint16
pop eax
; no RET, continue at next subroutine
;---------- Print 16-bit Hex Number ---------;
; INPUT:  AX  = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint16:
push eax
xchg al,ah
call HexPrint8
pop eax
; no RET, continue at next subroutine
;---------- Print 8-bit Hex Number ----------;
; INPUT:  AL  = Number                       ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint8:
push eax
ror al,4
call HexPrint4
pop eax
; no RET, continue at next subroutine
;---------- Print 4-bit Hex Number ----------;
; INPUT:  AL  = Number (bits 0-3)            ;
;         RDI = Destination Pointer          ;
; OUTPUT: RDI = Modify                       ;
;--------------------------------------------;
HexPrint4:
cld
push eax
and al,0Fh
cmp al,9
ja HP4AF
add al,'0'
jmp HP4Store
HP4AF:
add al,'A'-10
HP4Store:
stosb
pop eax
ret

;---------- Data section -------------------------------------------------------
section '.data' data readable writeable
_Caption  DB '  GPR dump',0
_Message  DB 'EAXEBXECXEDXESPEBPESIEDI'
_Buffer   DB 1024 DUP (?)

;---------- Import section -----------------------------------------------------
section '.idata' import data readable writeable
library kernel32,'KERNEL32.DLL',user32,'USER32.DLL'
;--- OS functions ---
include 'api\kernel32.inc'    ; KERNEL32.DLL required because ExitProcess used
include 'api\user32.inc'      ; USER32.DLL required because MessageBoxA used

