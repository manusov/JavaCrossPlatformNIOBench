;------------------------------------------------------------------;
; Part of PAL (Platform Abstraction Layer) for Linux64             ;
; JNI ELF64 (Java Native Interface Executable Linkable Format 64   ;
;------------------------------------------------------------------;

SYS_READ         = 0      ; Linux API functions (syscall numbers)
SYS_WRITE        = 1
SYS_OPEN         = 2
SYS_CLOSE        = 3
SYS_LSEEK        = 8
SYS_MMAP         = 9
SYS_MUNMAP       = 11
SYS_NANOSLEEP    = 35
SYS_EXIT         = 60
SYS_UNLINK       = 87
SYS_GETTIME      = 228
SYS_GETRES       = 229
SYS_SETAFFINITY  = 203
SYS_GETAFFINITY  = 204
SYS_SETMEMPOLICY = 238    ; Note alternative codes
SYS_GETMEMPOLICY = 239    ; Note alternative codes

format ELF64
;--- Debug entry points ---
public checkPAL  as  'Java_JNITESTLINUX64_checkPAL'
public entryPAL  as  'Java_JNITESTLINUX64_entryPAL'
;--- Production entry points ---
public checkPAL  as  'Java_niobench_PAL_checkPAL'
public entryPAL  as  'Java_niobench_PAL_entryPAL'

;--- native int checkPAL(); ---
checkPAL:
mov eax,64
ret

;--- Entry point for PAL services, Java Native Interface (JNI) ----;
; Parm#1 = RDI = JNI Environmet                                    ;
; Parm#2 = RSI = JNI This Object reference (not used here)         ;
; Parm#3 = RDX = Object IPB array of qwords (long) ref. or NULL    ;
; Parm#4 = RCX = Object OPB array of qwords (long) ref. or NULL    ;
; Parm#5 = R8  = IPB size, qwords, or function code if IPB=NULL    ;
; Parm#6 = R9  = OPB size, qwords, or reserved if OPB=NULL         ;
; Return = RAX = JNI Status: 0=Error, 1=IA32 OK, 2=x64 OK          ;
;------------------------------------------------------------------;

entryPAL:
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
;---
@@:
jmp ReleaseRet

;--- Check for RDRAND instruction supported by CPU ----------------;
; Input:   RDI = Pointer to OPB (Output Parameters Block)          ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK               ;
;                set externally from this subroutine               ;
;          QWORD OPB[0] = Status flag:                             ;
;          0 = RDRAND not supported, 1 = Supported                 ;
;------------------------------------------------------------------; 

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


;--- Get Random Numbers array -------------------------------------;
; Input:   RSI = Pointer to IPB (Input Parameters Block)           ;
;                DWORD [RSI+00] = Fnc. code, decoded externally    ;
;                DWORD [RSI+04] = Reserved                         ;
;                DWORD [RSI+08] = Block length, qwords             ;
;                DWORD [RSI+12] = Reserved                         ;
;          RDI = Pointer to OPB (Output Parameters Block)          ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK               ;
;                set externally from this subroutine               ;
;          OPB[] = Output buffer                                   ;
;------------------------------------------------------------------; 

GET_RANDOM_DATA:
mov rdx,rdi        ; RDX = Base address of destination array
mov ecx,[rsi+08]   ; RCX = Length of dest. array, units = QWORDS
jrcxz .L1          ; Skip if length = 0 
.L0:
rdrand rax         ; RAX = Random number
jnc .L0            ; Wait for RNG ready
mov [rdx],rax      ; Store random number to array
add rdx,8
dec ecx
jnz .L0            ; Cycle for required length
.L1:
ret

;--- This fragment shared with NativeBench application ---
include 'NativeBenchHandler.inc'

;--- Functions pointers, for IPB absent ---
FunctionCount      =   1
FunctionSelector   DQ  CHECK_RDRAND                ; Check RND
;--- Functions pointers, for IPB present ---
iFunctionCount     =   2
iFunctionSelector  DQ  GET_RANDOM_DATA             ; Get RND
                   DQ  API_WRITE_READ_COPY_DELETE  ; Bench. OS API

