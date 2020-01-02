;------------------------------------------------------------------------------;
;                    Native Binary Library for Linux ia32                      ;
;        JNI ELF (Java Native Interface Executable Linkable Format 32)         ;
;                                                                              ;
;                  Updated at NIOBench v0.01.00 refactoring.                   ;
;------------------------------------------------------------------------------;

format ELF

;--- Binary services entry points ---

public checkBinary  as  'Java_niobenchrefactoring_resources_PAL_checkBinary'
public entryBinary  as  'Java_niobenchrefactoring_resources_PAL_entryBinary'  

;--- This simple entry point for debug native call mechanism ---

checkBinary:
mov eax,32
ret

;--- Entry point for binary services, Java Native Interface (JNI) -------------;
; Parm#1 = DWORD [esp+04] = JNI Environment                                    ;
; Parm#2 = DWORD [esp+08] = JNI This Object reference (not used here)          ;
; Parm#3 = DWORD [esp+12] = Object IPB array of qwords (long) ref. or NULL     ;
; Parm#4 = DWORD [esp+16] = Object OPB array of qwords (long) ref. or NULL     ;
; Parm#5 = QWORD [esp+20] = IPB size, qwords, or function code if IPB=NULL     ;
; Parm#6 = QWORD [esp+28] = OPB size, qwords, or reserved if OPB=NULL          ;
; Return = EAX = JNI Status: 0=Error, 1=IA32 OK, 2=x64 OK                      ;
;------------------------------------------------------------------------------;
; Stack layout at function call, as DWORDS.  ;
; [esp+00] = IP for return from subroutine   ;
; [esp+04] = JNI Environment                 ;
; [esp+08] = JNI This Object reference       ;
; [esp+12] = Object IPB array of QWORDs      ;
; [esp+16] = Object OPB array of QWORDs      ;
; [esp+20] = IPB size, low dword             ;
; [esp+24] = IPB size, high dword, usually 0 ;
; [esp+28] = OPB size, low dword             ;
; [esp+32] = OPB size, high dword, usually 0 ;
;--------------------------------------------;

entryBinary:
push ebx ecx edx esi edi ebp
mov ebp,esp
xor eax,eax
push eax eax             ; Reserve stack space for variables
and esp,0FFFFFFF0h       ; Align stack
xor esi,esi              ; Pre-blank IPB pointer
xor edi,edi              ; Pre-blank OPB pointer
;--- Check IPB presence ---
mov ecx,[ebp+12+24]             ; Parm#2 = Array reference
jecxz @f                        ; Go skip IPB extract. if IPB=null
mov ebx,[ebp+04+24]             ; Parm#1 = Environment
lea edx,[ebp-4]                 ; Parm#3 = isCopyAddress
mov eax,[ebx]                   ; RAX = Pointer to functions table
;--- Get IPB, parms: EBX=env, ECX=IPB Object, EDX=Pointer to flag --
push edx ecx ebx
call dword [eax+188*4]          ; JNI call [GetLongArrayElements]
add esp,12
test eax,eax
jz StatusRet                    ; Go skip if error = NULL pointer
xchg esi,eax                    ; RSI = Pointer to IPB
@@:
;--- Check OPB presence ---
mov ecx,[ebp+16+24]             ; Parm#2 = Array reference
jecxz @f                        ; Go skip OPB extract. if OPB=null
mov ebx,[ebp+04+24]             ; Parm#1 = Environment
lea edx,[ebp-8]                 ; Parm#3 = isCopyAddress
mov eax,[ebx]                   ; RAX = Pointer to functions table
;--- Get OPB, parms: EBX=env, ECX=IPB Object, EDX=Pointer to flag --
push ebp ebp esi esi            ; Push twice for alignment 16
push edx ecx ebx
call dword [eax+188*4]          ; JNI call [GetLongArrayElements]
add esp,12
pop esi esi ebp ebp
test eax,eax
jz StatusRet                    ; Go skip if error = NULL pointer
xchg edi,eax                    ; RSI = Pointer to IPB
@@:
;--- Target operation ---
test esi,esi
jz IPB_null                     ; Go spec. case, IPB size = function
;--- Handling IPB present ---
xor eax,eax
mov edx,[esi]                ; DWORD IPB[0] = Function selector 
cmp edx,iFunctionCount
jae @f
lea ecx,[iFunctionSelector]  ; RCX must be adjustable by *.SO maker
call dword [ecx+edx*4]
@@:
;--- Return point ---
ReleaseRet:
;--- Check IPB release requirement flag and IPB presence ---
cmp dword [ebp-4],0
je @f                           ; Go skip if IPB release not req.
mov ecx,[ebp+12+24]             ; Parm#2 = Array reference
jecxz @f                        ; Go skip IPB extract. if IPB=null
mov ebx,[ebp+04+24]             ; Parm#1 = Environment
mov edx,esi                     ; Parm#3 = Copy address, note RSI
xor esi,esi                     ; Parm#4 = Release mode
mov eax,[ebx]                   ; EAX = Pointer to functions table
;--- Release IPB, parms: EBX=env, ECX=obj, EDX=P, ESI=Mode --- 
push ebp ebp edi edi            ; Twice for align ESP
push esi edx ecx ebx 
call dword [eax+196*4]          ; call [ReleaseLongArrayElements]
add esp,16
pop edi edi ebp ebp
@@:
;--- Check OPB release requirement flag and OPB presence ---
cmp dword [ebp-8],0
je @f                           ; Go skip if OPB release not req.
mov ecx,[ebp+16+24]             ; Parm#2 = Array reference
jecxz @f                        ; Go skip IPB extract. if IPB=null
mov ebx,[ebp+04+24]             ; Parm#1 = Environment
mov edx,edi                     ; Parm#3 = Copy address, note RSI
xor esi,esi                     ; Parm#4 = Release mode
mov eax,[ebx]                   ; EAX = Pointer to functions table
;--- Release OPB, parms: EBX=env, ECX=obj, EDX=P, ESI=Mode --- 
push esi edx ecx ebx 
call dword [eax+196*4]          ; call [ReleaseLongArrayElements]
add esp,16
@@:
;--- Return with status = EAX ---
mov eax,1                    ; EAX=1 (true) means OK from JNI
StatusRet:
mov esp,ebp                  ; Restore stack
pop ebp edi esi edx ecx ebx
ret
;--- Special fast case, no Input Parameters Block ---
IPB_null:
xor eax,eax
mov edx,[ebp+20+24]          ; EDX = Selector / IPB size place
cmp edx,FunctionCount        ; DWORD EDX = Function selector 
jae @f
lea ecx,[FunctionSelector]   ; RCX must be adjustable by *.SO maker
call dword [ecx+edx*4]
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
LibraryName        DB  'NIOBench native library v0.01.00 for Linux ia32.',0


  
