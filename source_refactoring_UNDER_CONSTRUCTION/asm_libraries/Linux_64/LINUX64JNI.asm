;------------------------------------------------------------------------------;
;                    Native Binary Library for Linux x64                       ;
;       JNI ELF (Java Native Interface Executable Linkable Format 64)          ;
;                                                                              ;
;                  Updated at NIOBench v0.01.00 refactoring.                   ;
;------------------------------------------------------------------------------;

format ELF64

;--- Binary services entry points ---

public checkBinary  as  'Java_niobenchrefactoring_resources_PAL_checkBinary'
public entryBinary  as  'Java_niobenchrefactoring_resources_PAL_entryBinary'  

;--- This simple entry point for debug native call mechanism ---

checkBinary:
mov eax,64
ret

;--- Entry point for binary services, Java Native Interface (JNI) -------------;
; Parm#1 = RDI = JNI Environmet                                                ;
; Parm#2 = RSI = JNI This Object reference (not used here)                     ;
; Parm#3 = RDX = Object IPB array of qwords (long) ref. or NULL                ;
; Parm#4 = RCX = Object OPB array of qwords (long) ref. or NULL                ;
; Parm#5 = R8  = IPB size, qwords, or function code if IPB=NULL                ;
; Parm#6 = R9  = OPB size, qwords, or reserved if OPB=NULL                     ;
; Return = RAX = JNI Status: 0=Error, 1=IA32 OK, 2=x64 OK                      ;
;------------------------------------------------------------------------------;

entryBinary:
push rbp rbx r12 r13 r14 r15
mov rbp,rsp                     ; Save RSP because alignment
xor eax,eax
push rax rax                    ; Reserve stack space for variables
mov rbx,rdi                     ; RBX = Environment
mov r12,rdx                     ; R12 = Object: Input Parm. Block
mov r13,rcx                     ; R13 = Object: Output Parm. Block
mov r14,r8                      ; R14 = Length of IPB (parm#5)
mov r15,r9                      ; R15 = Length of OPB (parm#6)
and rsp,0FFFFFFFFFFFFFFF0h      ; Stack alignment by calling conv.
xor esi,esi                     ; Pre-blank IPB pointer
xor edi,edi                     ; Pre-blank OPB pointer
;--- Check IPB presence ---
test r12,r12
jz @f                           ; Go skip IPB extract. if IPB=null
mov rdi,rbx                     ; Parm#1 = Environment
mov rsi,r12                     ; Parm#2 = Array reference
lea rdx,[rbp-8]                 ; Parm#3 = isCopyAddress
mov rax,[rbx]                   ; RAX = Pointer to functions table
;--- Get IPB, parms: RDI=env, RSI=IPB Object, RDX=Pointer to flag --
call qword [rax+188*8]          ; JNI call [GetLongArrayElements]
test rax,rax
jz StatusRet                    ; Go skip if error = NULL pointer
xchg rsi,rax                    ; RSI = Pointer to IPB
@@:
;--- Check OPB presence ---
test r13,r13
jz @f                           ; Go skip OPB extraction if OPB=null
push rsi rsi                    ; Store IPB, twice for align RSP
mov rdi,rbx                     ; Parm#1 = Environment
mov rsi,r13                     ; Parm#2 = Array reference
lea rdx,[rbp-16]                ; Parm#3 = isCopyAddress 
mov rax,[rbx]                   ; RAX = Pointer to functions table
;--- Get OPB, parms: RDI=env, RSI=OPB Object, RDX=Pointer to flag --
call qword [rax+188*8]          ; JNI call [GetLongArrayElements]
pop rsi rsi
test rax,rax
jz StatusRet                    ; Go skip if error = NULL pointer
xchg rdi,rax                    ; RSI = Pointer to OPB
@@: 
;--- Target operation ---
test rsi,rsi
jz IPB_null                     ; Go spec. case, IPB size = function
;--- Handling IPB present ---
xor eax,eax
mov r10d,[rsi]               ; DWORD IPB[0] = Function selector 
cmp r10d,iFunctionCount
jae @f
lea rcx,[iFunctionSelector]  ; RCX must be adjustable by *.SO maker
call qword [rcx+r10*8]
@@:
;--- Return point ---
ReleaseRet:
;--- Check IPB release requirement flag and IPB presence ---
cmp qword [rbp-8],0
je @f                           ; Go skip if IPB release not req.
test r12,r12
jz @f                           ; Go skip IPB extract. if IPB=null
push rdi rdi                    ; Store OPB, twice for align RSP
mov rdi,rbx                     ; Parm#1 = Environment 
mov rdx,rsi                     ; Parm#3 = Copy address, note RSI
mov rsi,r12                     ; Parm#2 = Object reference
xor ecx,ecx                     ; Parm#4 = Release mode
mov rax,[rbx]                   ; RAX = Pointer to functions table
;--- Release IPB, parms: RDI=env, RSI=obj, RDX=P, RCX=Mode --- 
call qword [rax+196*8]          ; call [ReleaseLongArrayElements]
pop rdi rdi
@@:
;--- Check OPB release requirement flag and OPB presence ---
cmp qword [rbp-16],0
je @f                           ; Go skip if OPB release not req.
test r13,r13
jz @f                           ; Go skip OPB extract. if OPB=null
mov rdx,rdi                     ; Parm#3 = Copy address, note RDI
mov rdi,rbx                     ; Parm#1 = Environment 
mov rsi,r13                     ; Parm#2 = Object reference
xor ecx,ecx                     ; Parm#4 = Release mode
mov rax,[rbx]                   ; RAX = Pointer to functions table
;--- Release OPB, parms: RDI=env, RSI=obj, RDX=P, RCX=Mode --- 
call qword [rax+196*8]          ; call [ReleaseLongArrayElements]
@@:
;--- Return with status = RAX ---
mov eax,2                       ; RAX=2 (true) means OK from JNI
StatusRet:
mov rsp,rbp                     ; Restore stack
pop r15 r14 r13 r12 rbx rbp
ret
;--- Special fast case, no Input Parameters Block ---
IPB_null:
xor eax,eax
cmp r14,FunctionCount        ; QWORD R14 = Function selector 
jae @f
lea rcx,[FunctionSelector]   ; RCX must be adjustable by *.SO maker
call qword [rcx+r14*8]
@@:
jmp ReleaseRet

;---------- Get native library name -------------------------------------------;
; Parm#1 = RDI = Pointer to buffer for return data                             ;
; Output = RAX = Status: 0=Error, Non-Zero=OK, set external. at transit caller ;
;          Return null-terminated string, ASCII bytes.                         ;
;------------------------------------------------------------------------------;
GetLibraryName:
push rsi rdi
cld
lea rsi,[LibraryName]
@@:
lodsb
cmp al,0
je @f
stosb
jmp @b
@@:
pop rdi rsi
ret

;---------- Get native library information ------------------------------------;
; Note. Get name and Get info is separate procedures for easy debug reasons.   ;
; Parm#1 = RDI = Pointer to buffer for return data                             ;
; Output = RAX = Status: 0=Error, Non-Zero=OK, set external. at transit caller ;
;          Return:                                                             ;
;             dword[0] bit[0] = Hardware Random Number Generator supported     ;
;             ... next data reserved yet ...                                   ; 
;------------------------------------------------------------------------------;
GetLibraryInfo:
push rbx
mov dword [rdi],0
;--- Check CPUID instruction is supported and not locked ---
mov ebx,21
pushf                     ; In the 64-bit mode, push RFLAGS
pop rax
bts eax,ebx               ; Set EAX.21=1
push rax
popf                      ; Load RFLAGS with RFLAGS.21=1
pushf                     ; Store RFLAGS
pop rax                   ; Load RFLAGS to RAX
btr eax,ebx               ; Check EAX.21=1, Set EAX.21=0
jnc .Done                 ; Go error branch if cannot set EFLAGS.21=1
push rax
popf                      ; Load RFLAGS with RFLAGS.21=0
pushf                     ; Store RFLAGS
pop rax                   ; Load RFLAGS to RAX
btr eax,ebx               ; Check EAX.21=0
jc .Done                  ; Go if cannot set EFLAGS.21=0
;--- Check maximum supported standard CPUID function ---
xor eax,eax
cpuid                     ; CPUID function 0, here used output EAX = max. std.
cmp eax,1
jb .Done                  ; Go if CPUID function 1 not supported or locked
;--- Check CPUID function=1 , register=ECX , bit=30 , means RDRAND support ---
mov eax,1
cpuid
bt ecx,30
jnc .Done
;--- Update result if RDRAND supported, means set bit 0 --- 
inc dword [rdi]
;--- Exit points ---
.Done:                   ; This point for errors handling
pop rbx
ret
;---------- Library main functionality ----------------------------------------;
include 'include\Equations.inc'
include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureMixedIO.inc'
include 'include\MeasureDeleteFile.inc'
;--- Functions pointers, for IPB absent ---
FunctionCount      =   3
FunctionSelector   DQ  GetLibraryName    ; 0 = Get native library ASCII name
                   DQ  GetLibraryInfo    ; 1 = Get native library information  
                   DQ  0  
;--- Functions pointers, for IPB present ---
iFunctionCount     =   7
iFunctionSelector  DQ  GetRandomData      ; 0 = Get array of random data
                   DQ  MeasureReadFile    ; 1 = Read file
                   DQ  MeasureWriteFile   ; 2 = Write file
                   DQ  MeasureCopyFile    ; 3 = Copy file
                   DQ  MeasureMixedIO     ; 4 = Mixed read/write
                   DQ  MeasureDeleteFile  ; 5 = Delete file                   
                   DQ  0                  ; Reserved unused
;--- Native library name string ---
LibraryName        DB  'NIOBench native library v0.01.00 for Linux x64.',0  

