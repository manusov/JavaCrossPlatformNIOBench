;------------------------------------------------------------------;
; Part of PAL (Platform Abstraction Layer) for Linux32             ;
; JNI ELF (Java Native Interface Executable Linkable Format 32     ;
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

format ELF
;--- Debug entry points ---
public checkPAL  as  'Java_JNITESTLINUX32_checkPAL'
public entryPAL  as  'Java_JNITESTLINUX32_entryPAL'
;--- Production entry points ---
public checkPAL  as  'Java_niobench_PAL_checkPAL'
public entryPAL  as  'Java_niobench_PAL_entryPAL'

;--- native int checkPAL(); ---
checkPAL:
mov eax,32
ret

;--- Entry point for PAL services, Java Native Interface (JNI) ------------;
; Parm#1 = DWORD [esp+04] = JNI Environment                                ;
; Parm#2 = DWORD [esp+08] = JNI This Object reference (not used here)      ;
; Parm#3 = DWORD [esp+12] = Object IPB array of qwords (long) ref. or NULL ;
; Parm#4 = DWORD [esp+16] = Object OPB array of qwords (long) ref. or NULL ;
; Parm#5 = QWORD [esp+20] = IPB size, qwords, or function code if IPB=NULL ;
; Parm#6 = QWORD [esp+28] = OPB size, qwords, or reserved if OPB=NULL      ;
; Return = EAX = JNI Status: 0=Error, 1=IA32 OK, 2=x64 OK                  ;
;--------------------------------------------------------------------------;
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

entryPAL:
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
;---
@@:
jmp ReleaseRet

;--- Check for RDRAND instruction supported by CPU ----------------;
; Input:   EDI = Pointer to OPB (Output Parameters Block)          ;
; Output:  EAX = JNI Status: 0=Error, 2=Win64 JNI OK               ;
;                set externally from this subroutine               ;
;          QWORD OPB[0] = Status flag:                             ;
;          0 = RDRAND not supported, 1 = Supported                 ;
;------------------------------------------------------------------; 

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

;--- Get Random Numbers array -------------------------------------;
; Input:   ESI = Pointer to IPB (Input Parameters Block)           ;
;                DWORD [ESI+00] = Fnc. code, decoded externally    ;
;                DWORD [ESI+04] = Reserved                         ;
;                DWORD [ESI+08] = Block length, qwords             ;
;                DWORD [ESI+12] = Reserved                         ;
;          EDI = Pointer to OPB (Output Parameters Block)          ;
; Output:  EAX = JNI Status: 0=Error, 2=Win64 JNI OK               ;
;                set externally from this subroutine               ;
;          OPB[] = Output buffer                                   ;
;------------------------------------------------------------------; 

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

;--- This fragment shared with NativeBench application ---
include 'NativeBenchHandler.inc'

;--- Functions pointers, for IPB absent ---
FunctionCount      =   1
FunctionSelector   DD  CHECK_RDRAND                ; Check RND
;--- Functions pointers, for IPB present ---
iFunctionCount     =   2
iFunctionSelector  DD  GET_RANDOM_DATA             ; Get RND
                   DD  API_WRITE_READ_COPY_DELETE  ; Bench. OS API

