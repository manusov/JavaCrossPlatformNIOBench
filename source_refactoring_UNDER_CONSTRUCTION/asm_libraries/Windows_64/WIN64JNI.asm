;------------------------------------------------------------------------------;
;                 Native Binary Library for Windows x64                        ;
;           JNI DLL (Java Native Interface Dynamical Load Library)             ;
;                                                                              ;
;                  Updated at NIOBench v0.01.00 refactoring.                   ;
;------------------------------------------------------------------------------;
include 'win64a.inc'
format PE64 GUI 4.0 DLL
entry DllMain
;---------- Code section ------------------------------------------------------;
section '.text' code readable executable
DllMain:        ; This called by Operating System when load/unload DLL
mov eax,1       ; Return status to OS caller (actual when load)
ret
;--- This simple entry point for debug native call mechanism ---
checkBinary:    
mov eax,64
ret
;--- Entry point for binary services, Java Native Interface (JNI) ---
; Parm#1 = RCX = JNI Environment  
; Parm#2 = RDX = JNI This Object reference (not used by this routine)
; Parm#3 = R8  = Object IPB array of qwords (long) reference or NULL
; Parm#4 = R9  = Object OPB array of qwords (long) reference or NULL
; Parm#5 = [RSP+40] = IPB size, qwords, or function code if IPB=NULL
; Parm#6 = [RSP+48] = OPB size, qwords, or reserved if OPB=NULL
; Return = RAX = JNI Status: 0=Error, 1=IA32 OK, 2=x64 OK
;---
entryBinary:
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
;---------- Data section ------------------------------------------------------;
section '.data' data readable writeable
;--- Functions pointers, for IPB absent ---
FunctionCount      =   3
FunctionSelector   DQ  GetLibraryName    ; 0 = Get native library ASCII name
                   DQ  GetLibraryInfo    ; 1 = Get native library information   
                   DQ  0                 ; Reserved unused  
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
LibraryName        DB  'NIOBench native library v0.03.00 for Windows x64.',0  
;---------- Export section ----------------------------------------------------;
section '.edata' export data readable
export 'WIN64JNI.dll' ,\
checkBinary  , 'Java_niobenchrefactoring_resources_PAL_checkBinary', \
entryBinary  , 'Java_niobenchrefactoring_resources_PAL_entryBinary'
;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library kernel32 , 'KERNEL32.DLL' , advapi32 , 'ADVAPI32.DLL'
include 'api\kernel32.inc'
include 'api\advapi32.inc'
;---------- Relocations section -----------------------------------------------; 
data fixups
end data

