;------------------------------------------------------------------------------;
; Part of PAL (Platform Abstraction Layer) for Windows x64                     ;
; JNI DLL (Java Native Interface Dynamical Load Library)                       ;
;------------------------------------------------------------------------------;

include 'win64a.inc'
format PE64 GUI 4.0 DLL
entry DllMain

;---------- Code section -------------------------------------------------------
section '.text' code readable executable

DllMain:        ; This called by Operating System when load/unload DLL
mov eax,1       ; Return status to OS caller (actual when load)
ret

checkPAL:       ; This simple entry for debug native call mechanism
mov eax,64
ret

;---------- Entry point for PAL services, Java Native Interface (JNI) ---------;
; Parm#1 = RCX = JNI Environment                                               ;                                                 
; Parm#2 = RDX = JNI This Object reference (not used by this routine)          ;
; Parm#3 = R8  = Object IPB array of qwords (long) reference or NULL           ;
; Parm#4 = R9  = Object OPB array of qwords (long) reference or NULL           ;
; Parm#5 = [RSP+40] = IPB size, qwords, or function code if IPB=NULL           ;
; Parm#6 = [RSP+48] = OPB size, qwords, or reserved if OPB=NULL                ;
; Return = RAX = JNI Status: 0=Error, 1=IA32 OK, 2=x64 OK                      ;
;------------------------------------------------------------------------------;

entryPAL:
push rbx rsi rdi rbp r12 r13 r14 r15   ; Save non-volatile registers
mov rbp,rsp                            ; Save RSP because stack alignment
xor eax,eax
push rax rax                           ; Storage for variable
mov rbx,rcx                            ; RBX = Environment
mov r12,r8                             ; R12 = Object: Input Parm. Block
mov r13,r9                             ; R13 = Object: Output Parm. Block 
mov r14,[rbp+64+8+32+0]                ; R14 = Length of IPB (parm#5)
mov r15,[rbp+64+8+32+8]                ; R15 = Length of OPB (parm#6)
and rsp,0FFFFFFFFFFFFFFF0h             ; Stack alignment by calling convention
sub rsp,32                             ; Parm. shadow by calling convention
xor esi,esi                            ; Pre-blank IPB pointer
xor edi,edi                            ; Pre-blank OPB pointer
;--- Check IPB presence ---
test r12,r12
jz @f                                  ; Go skip IPB extraction if IPB=null
mov rdx,r12
lea r8,[rbp-8]
mov rax,[rbx]                          ; RAX = Pointer to functions table
;--- Get IPB, parms: RCX=env, RDX=IPB Object, R8=Pointer to flag ---
call qword [rax+188*8]                 ; JNI call [GetLongArrayElements]
test rax,rax
jz StatusRet                           ; Go skip if error = NULL pointer
xchg rsi,rax                           ; RSI = Pointer to IPB
@@:
;--- Check OPB presence ---
test r13,r13
jz @f                                  ; Go skip IPB extraction if OPB=null
mov rcx,rbx
mov rdx,r13
lea r8,[rbp-16]
mov rax,[rbx]                          ; RAX = Pointer to functions table
;--- Get OPB, parms: RCX=env, RDX=OPB Object, R8=Pointer to flag ---
call qword [rax+188*8]                 ; JNI call [GetLongArrayElements]
test rax,rax
jz StatusRet                           ; Go skip if error = NULL pointer
xchg rdi,rax                           ; RSI = Pointer to OPB
@@: 
;--- Target operation ---
test rsi,rsi
jz IPB_null                            ; Go special case, IPB size = function
;--- Handling IPB present ---
xor eax,eax
mov r10d,[rsi]                         ; DWORD IPB[0] = Function selector 
cmp r10d,iFunctionCount
jae @f
call qword [iFunctionSelector+r10*8]
@@:
;--- Return point ---
ReleaseRet:
;--- Check IPB release requirement flag and IPB presence ---
cmp qword [rbp-8],0
je @f                                  ; Go skip if IPB release not required
test r12,r12
jz @f                                  ; Go skip IPB extraction if IPB=null
mov rcx,rbx
mov rdx,r12
mov r8,rsi
xor r9d,r9d
mov rax,[rbx]                          ; RAX = Pointer to functions table
;--- Release IPB, parms: RCX=env, RDX=obj, R8=Pointer, R9=Release mode --- 
call qword [rax+196*8]                 ; call [ReleaseLongArrayElements]
@@:
;--- Check OPB release requirement flag and OPB presence ---
cmp qword [rbp-16],0
je @f                                  ; Go skip if OPB release not required
test r13,r13
jz @f                                  ; Go skip OPB extraction if OPB=null
mov rcx,rbx
mov rdx,r13
mov r8,rdi
xor r9d,r9d
mov rax,[rbx]                          ; RAX = Pointer to functions table
;--- Release OPB, parms: RCX=env, RDX=obj, R8=Pointer, R9=Release mode --- 
call qword [rax+196*8]                 ; call [ReleaseLongArrayElements]
@@:
;--- Return with status = RAX ---
mov eax,2                              ; RAX=2 (true) means OK from Win64 DLL 
StatusRet:                             ; Entry point with RAX=0 (error)
mov rsp,rbp                            ; Restore RSP after alignment
pop r15 r14 r13 r12 rbp rdi rsi rbx    ; Restore non-volatile registers
ret                                    ; Return to Java JNI service caller 
;--- Special fast case, no Input Parameters Block ---
IPB_null:
xor eax,eax
cmp r14,FunctionCount                  ; QWORD R14 = Function selector 
jae @f
call qword [FunctionSelector+r14*8]
@@:
jmp ReleaseRet

;---------- Check for RDRAND instruction supported by CPU ---------------------;
; Input:   RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK                           ;
;                set externally from this subroutine                           ;
;          QWORD OPB[0] = Status flag: 0 = RDRAND not supported, 1 = Supported ; 
;------------------------------------------------------------------------------; 

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

;---------- Get Random Numbers array ------------------------------------------;
; Input:   RSI = Pointer to IPB (Input Parameters Block)                       ;
;                DWORD [RSI+00] = Function code, decoded externally            ;
;                DWORD [RSI+04] = Reserved                                     ;
;                DWORD [RSI+08] = Block length, qwords                         ;
;                DWORD [RSI+12] = Reserved                                     ;
;          RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK                           ;
;                set externally from this subroutine                           ;
;          OPB[] = Output buffer                                               ;
;------------------------------------------------------------------------------; 

GET_RANDOM_DATA:
mov rdx,rdi         ; RDX = Base address of destination array
mov ecx,[rsi+08]    ; RCX = Length of destination array, units = QWORDS
jrcxz .L1           ; Skip if length = 0 
.L0:
rdrand rax          ; RAX = Random number
jnc .L0             ; Wait for RNG ready
mov [rdx],rax       ; Store random number to array
add rdx,8
dec ecx
jnz .L0             ; Cycle for required length
.L1:
ret

;--- This fragment shared with NativeBench application ---
include 'NativeBenchHandler.inc'

;---------- Data section -------------------------------------------------------
section '.data' data readable writeable
;--- Functions pointers, for IPB absent ---
FunctionCount      =   1
FunctionSelector   DQ  CHECK_RDRAND       ; Check RDRAND instruction support
;--- Functions pointers, for IPB present ---
iFunctionCount     =   2
iFunctionSelector  DQ  GET_RANDOM_DATA              ; Get array of random data
                   DQ  API_WRITE_READ_COPY_DELETE   ; Benchmarks by OS API   

;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library kernel32 , 'KERNEL32.DLL'
include 'api\kernel32.inc'

;---------- Export section -----------------------------------------------------
section '.edata' export data readable
export 'WIN64JNI.dll' ,\
checkPAL  , 'Java_niobench_PAL_checkPAL', \
entryPAL  , 'Java_niobench_PAL_entryPAL'

;---------- Relocations section ------------------------------------------------ 
data fixups
end data

