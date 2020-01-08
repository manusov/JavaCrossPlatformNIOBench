;------------------------------------------------------------------------------;
;                Native Binary Library for Windows ia32                        ;
;            JNI DLL (Java Native Interface Dynamical Load Library)            ;
;                                                                              ;
;                  Updated at NIOBench v0.01.00 refactoring.                   ; 
;------------------------------------------------------------------------------;
include 'win32a.inc'
format PE GUI 4.0 DLL
entry DllMain
;---------- Code section -------------------------------------------------------
section '.text' code readable executable
DllMain:        ; This called by Operating System when load/unload DLL
mov eax,1       ; Return status to OS caller (actual when load)
ret
;--- This simple entry point for debug native call mechanism ---
checkBinary:              ; also differentiate between Win32 and Win32/WOW64
;--- Detect WOW64 ---
push ebx
push LibName
call [GetModuleHandle]
test eax,eax
jz @f
push FncName eax
call [GetProcAddress]
test eax,eax
jz @f
xchg ebx,eax
call [GetCurrentProcess]
push 0
mov ecx,esp
push ecx eax
call ebx
pop eax
@@:
pop ebx
;--- Send result ---
test eax,eax
mov eax,32
jz @f
inc eax
@@:
ret
;--- Entry point for binary services, Java Native Interface (JNI) ---
; Parm#1 = [ESP+04] = JNI Environment  
; Parm#2 = [ESP+08] = JNI This Object reference (not used by this routine)
; Parm#3 = [ESP+12] = Object IPB array of qwords (long) reference or NULL
; Parm#4 = [ESP+16] = Object OPB array of qwords (long) reference or NULL
; Parm#5 = [ESP+20] = IPB size, qwords, or function code if IPB=NULL
; Parm#6 = [ESP+24] = OPB size, qwords, or reserved if OPB=NULL
; Return = EAX = JNI Status, 0=Error, 1=IA32 OK, 2=x64 OK
; Remember about 6*4=24 bytes must be removed from stack when return (RET 24),
; because required by IA32 calling convention.
;---
entryBinary:
push ebx esi edi ebp                   ; Save non-volatile registers
xor eax,eax
push eax eax                           ; Reserve space for variables
mov ebp,esp                            ; EBP=Frame, parm #1 at [ebp+28]
xor esi,esi                            ; Pre-blank IPB pointer
xor edi,edi                            ; Pre-blank OPB pointer
;--- Check IPB presence ---
mov ecx,[ebp+36]                       ; ECX = IPB object
jecxz @f                               ; Go skip IPB extraction if IPB=null
mov ebx,[ebp+28]                       ; EBX = environment
mov eax,[ebx]                          ; EAX = Pointer to functions table
push ebp ecx ebx  
;--- Get IPB, parms: env, IPB Object, Pointer to flag ---
call dword [eax+188*4]                 ; JNI call [GetLongArrayElements]
test eax,eax
jz StatusRet                           ; Go skip if error = NULL pointer
xchg esi,eax                           ; ESI = Pointer to IPB
@@:
;--- Check OPB presence ---
mov ecx,[ebp+40]                       ; ECX = OPB object
jecxz @f                               ; Go skip IPB extraction if OPB=null
mov ebx,[ebp+28]                       ; EBX = environment
mov eax,[ebx]                          ; EAX = Pointer to functions table
lea edx,[ebp+4]
push edx ecx ebx  
;--- Get OPB, parms: env, OPB Object, Pointer to flag ---
call dword [eax+188*4]                 ; JNI call [GetLongArrayElements]
test eax,eax
jz StatusRet                           ; Go skip if error = NULL pointer
xchg edi,eax                           ; EDI = Pointer to OPB
@@:
;--- Target operation ---
test esi,esi
jz IPB_null
;--- Handling IPB present ---
xor eax,eax
mov ecx,[esi]
cmp ecx,iFunctionCount
jae @f
call dword [iFunctionSelector+ecx*4]
@@:
;--- Return point ---
ReleaseRet:
;--- Check IPB release requirement flag and IPB presence ---
cmp dword [ebp],0
je @f                                  ; Go skip if IPB release not required
mov ecx,[ebp+36]                       ; ECX = IPB object
jecxz @f                               ; Go skip IPB release if IPB=null
mov ebx,[ebp+28]                       ; EBX = environment
mov eax,[ebx]                          ; EAX = Pointer to functions table
push 0 esi ecx ebx  
;--- Release IPB, parms: env, obj, Pointer, Release mode --- 
call dword [eax+196*4]                 ; call [ReleaseLongArrayElements]
@@:
;--- Check OPB release requirement flag and OPB presence ---
cmp dword [ebp+4],0
je @f                                  ; Go skip if OPB release not required
mov ecx,[ebp+40]                       ; EDX = OPB object
jecxz @f                               ; Go skip OPB release if OPB=null
mov ebx,[ebp+28]                       ; EBX = environment
mov eax,[ebx]                          ; EAX = Pointer to functions table
push 0 edi ecx ebx  
;--- Release OPB, parms: env, obj, Pointer, Release mode --- 
call dword [eax+196*4]                 ; call [ReleaseLongArrayElements]
@@:
;--- Return with status = EAX ---
mov eax,1                              ; RAX=1 (true) means OK from Win32 DLL 
StatusRet:                             ; Entry point with RAX=0 (error)
pop ecx ecx ebp edi esi ebx            ; Restore non-volatile registers
ret 24                                 ; Return to Java JNI service caller 
;--- Special fast case, no Input Parameters Block ---
IPB_null:
xor eax,eax
mov ecx,[ebp+44]
cmp ecx,FunctionCount
jae @f
call dword [FunctionSelector+ecx*4]
@@:
jmp ReleaseRet
;---------- Get native library name -------------------------------------------;
; Parm#1 = EDI = Pointer to buffer for return data                             ;
; Output = EAX = Status: 0=Error, Non-Zero=OK, set external. at transit caller ;
;          Return null-terminated string, ASCII bytes.                         ;
;------------------------------------------------------------------------------;
GetLibraryName:
push esi edi
cld
lea esi,[LibraryName]
@@:
lodsb
cmp al,0
je @f
stosb
jmp @b
@@:
pop edi esi
ret
;---------- Get native library information ------------------------------------;
; Note. Get name and Get info is separate procedures for easy debug reasons.   ;
; Parm#1 = EDI = Pointer to buffer for return data                             ;
; Output = EAX = Status: 0=Error, Non-Zero=OK, set external. at transit caller ;
;          Return:                                                             ;
;             dword[0] bit[0] = Hardware Random Number Generator supported     ;
;             ... next data reserved yet ...                                   ; 
;------------------------------------------------------------------------------;
GetLibraryInfo:
push ebx
mov dword [edi],0
;--- Check CPUID instruction is supported and not locked ---
mov ebx,21
pushf                     ; In the 64-bit mode, push RFLAGS
pop eax
bts eax,ebx               ; Set EAX.21=1
push eax
popf                      ; Load RFLAGS with RFLAGS.21=1
pushf                     ; Store RFLAGS
pop eax                   ; Load RFLAGS to RAX
btr eax,ebx               ; Check EAX.21=1, Set EAX.21=0
jnc .Done                 ; Go error branch if cannot set EFLAGS.21=1
push eax
popf                      ; Load RFLAGS with RFLAGS.21=0
pushf                     ; Store RFLAGS
pop eax                   ; Load RFLAGS to RAX
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
inc dword [edi]
;--- Exit points ---
.Done:                   ; This point for errors handling
pop ebx
ret
;---------- Library main functionality ----------------------------------------;
include 'include\Equations.inc'
include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureMixedIO.inc'
include 'include\MeasureDeleteFile.inc'
;---------- Data section -------------------------------------------------------
section '.data' data readable writeable
;--- Functions pointers, for IPB absent ---
FunctionCount      =   3
FunctionSelector   DD  GetLibraryName     ; 0 = Get native library ASCII name
                   DD  GetLibraryInfo     ; 1 = Get native library information  
                   DD  0  
;--- Functions pointers, for IPB present ---
iFunctionCount     =   7
iFunctionSelector  DD  GetRandomData      ; 0 = Get array of random data
                   DD  MeasureReadFile    ; 1 = Read file
                   DD  MeasureWriteFile   ; 2 = Write file
                   DD  MeasureCopyFile    ; 3 = Copy file
                   DD  MeasureMixedIO     ; 4 = Mixed read/write
                   DD  MeasureDeleteFile  ; 5 = Delete file                   
                   DD  0                  ; Reserved unused
;--- Native library name string ---
LibraryName        DB  'NIOBench native library v0.03.00 for Windows ia32.',0  
;--- Data for detect WOW64 ---
LibName  DB  'KERNEL32',0
FncName  DB  'IsWow64Process',0
;---------- Export section -----------------------------------------------------
section '.edata' export data readable
export 'WIN32JNI.dll' ,\
checkBinary  , 'Java_niobenchrefactoring_resources_PAL_checkBinary', \
entryBinary  , 'Java_niobenchrefactoring_resources_PAL_entryBinary'
;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library kernel32 , 'KERNEL32.DLL' , advapi32 , 'ADVAPI32.DLL'
include 'api\kernel32.inc'
include 'api\advapi32.inc'
;---------- Relocations section ------------------------------------------------ 
data fixups
end data

