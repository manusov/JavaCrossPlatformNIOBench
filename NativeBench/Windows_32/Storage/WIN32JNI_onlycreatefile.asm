;------------------------------------------------------------------------------;
; Part of PAL (Platform Abstraction Layer) for Windows IA32                    ;
; Main module for JNI DLL (Java Native Interface Dynamical Load Library)       ;
;------------------------------------------------------------------------------;

include 'win32a.inc'
format PE GUI 4.0 DLL
entry DllMain

;---------- Code section -------------------------------------------------------
section '.text' code readable executable

DllMain:        ; This called by Operating System when load/unload DLL
mov eax,1       ; Return status to OS caller (actual when load)
ret
                ; This entry for debug native call mechanism,
checkPAL:       ; also differentiate between Win32 and Win32/WOW64
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

;--- Entry point for PAL services, Java Native Interface (JNI) ----------------;
; Parm#1 = [ESP+04] = JNI Environment                                          ;  
; Parm#2 = [ESP+08] = JNI This Object reference (not used by this routine)     ;
; Parm#3 = [ESP+12] = Object IPB array of qwords (long) reference or NULL      ;
; Parm#4 = [ESP+16] = Object OPB array of qwords (long) reference or NULL      ;
; Parm#5 = [ESP+20] = IPB size, qwords, or function code if IPB=NULL           ;
; Parm#6 = [ESP+24] = OPB size, qwords, or reserved if OPB=NULL                ;
; Return = EAX = JNI Status, 0=Error, 1=IA32 OK, 2=x64 OK                      ;
; Remember about 6*4=24 bytes must be removed from stack when return (RET 24), ;
; because required by IA32 calling convention.                                 ;
;------------------------------------------------------------------------------;

entryPAL:
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

;---------- Check for RDRAND instruction supported by CPU ---------------------;
; Input:   EDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  EAX = JNI Status: 0=Error, 1=Win32 JNI OK                           ;
;                set externally from this subroutine                           ;
;          QWORD OPB[0] = Status flag: 0 = RDRAND not supported, 1 = Supported ; 
;------------------------------------------------------------------------------; 

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

;---------- Get Random Numbers array ------------------------------------------;
; Input:   ESI = Pointer to IPB (Input Parameters Block)                       ;
;                DWORD [ESI+00] = Function code, decoded externally            ;
;                DWORD [ESI+04] = Reserved                                     ;
;                DWORD [ESI+08] = Block length, qwords                         ;
;                DWORD [ESI+12] = Reserved                                     ;
;          EDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  EAX = JNI Status: 0=Error, 1=Win32 JNI OK                           ;
;                set externally from this subroutine                           ;
;          OPB[] = Output buffer                                               ;
;------------------------------------------------------------------------------; 

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

;---------- Create file by OS API ---------------------------------------------;
; Input:   ESI = Pointer to IPB (Input Parameters Block)                       ;
;                DWORD [ESI+00] = Function code, decoded externally            ;
;                DWORD [ESI+04] = Reserved                                     ;
;                Array [ESI+08] = File path ASCII string                       ;
;          EDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  EAX = JNI Status: 0=Error, 1=Win32 JNI OK                           ;
;                set externally from this subroutine                           ;
;          OPB[] = Output buffer                                               ;
;          QWORD OPB[0] = EAX after OS API function call (CreateFile)          ;
;                         0 = Error, otherwise file handle                     ;
;                         EAX located at low DWORD, high dword blanked to 0    ;
;------------------------------------------------------------------------------; 

; File performance flags
FILE_FLAGS EQU \
FILE_ATTRIBUTE_NORMAL + FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH

API_CREATE_FILE:
xor eax,eax                         ; EAX=0 for compact next instructions
push eax                            ; Parm#7 = Template file handle, not used 
push FILE_FLAGS                     ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                  ; Parm#5 = Creation disposition
push eax                            ; Parm#4 = Security attributes = 0
push eax                            ; Parm#3 = Share mode = 0
push GENERIC_READ OR GENERIC_WRITE  ; Parm#2 = Desired access
lea eax,[esi+8]
push eax                            ; Parm#1 = Pointer to file path
call [CreateFileA]
mov dword [edi+0],eax               ; Save status to OPB
mov dword [edi+4],0                 ; Blank high dword
ret

;---------- Data section -------------------------------------------------------
section '.data' data readable writeable
;--- Functions pointers, for IPB absent ---
FunctionCount      =   1
FunctionSelector   DD  CHECK_RDRAND       ; Check RDRAND instruction support
;--- Functions pointers, for IPB present ---
iFunctionCount     =   2
iFunctionSelector  DD  GET_RANDOM_DATA    ; Get array of random data
                   DD  API_CREATE_FILE    ; Create file by OS API   
;--- Data for detect WOW64 ---
LibName  DB  'KERNEL32',0
FncName  DB  'IsWow64Process',0

;---------- Import section ----------------------------------------------------;
section '.idata' import data readable writeable
library kernel32 , 'KERNEL32.DLL'
include 'api\kernel32.inc'

;---------- Export section -----------------------------------------------------
section '.edata' export data readable
export 'WIN32JNI.dll' ,\
checkPAL  , 'Java_niobench_PAL_checkPAL', \
entryPAL  , 'Java_niobench_PAL_entryPAL'

;---------- Relocations section ------------------------------------------------ 
data fixups
end data

