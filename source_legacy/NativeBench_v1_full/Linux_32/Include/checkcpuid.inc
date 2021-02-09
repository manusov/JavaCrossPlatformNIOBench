;--- Detect CPUID support, execute CPUID function #0 --------------------;
; Note CPUID can be supported by CPU but locked by Virtual Monitor.      ;
; Note check bit EFLAGS.21 toggleable, it is CPUID support indicator.    ;
; Note probably wrong result if trace this subroutine code.              ;
;                                                                        ;
; INPUT:   None                                                          ;
;                                                                        ;
; OUTPUT:  CF flag = Status: 0(NC)=Support OK, 1(C)=Not supported        ;
;          EAX = Largest standard CPUID function supported,              ;
;                valid only if CF=0.                                     ;
;------------------------------------------------------------------------;
CheckCpuId:
push ebx ecx edx
;--- Check for ID bit writeable for "1" ---
mov ebx,21                ; EFLAGS bit number = 21
pushf                     ; In the 64-bit mode, push RFLAGS
pop eax
bts eax,ebx               ; Set EAX.21=1
push eax
popf                      ; Load EFLAGS with EFLAGS.21=1
pushf                     ; Store EFLAGS
pop eax                   ; Load EFLAGS to EAX
btr eax,ebx               ; Check EAX.21=1, Set EAX.21=0
jnc NoCpuId               ; Go error branch if cannot set EFLAGS.21=1
;--- Check for ID bit writeable for "0" ---
push eax
popf                      ; Load EFLAGS with EFLAGS.21=0
pushf                     ; Store EFLAGS
pop eax                   ; Load EFLAGS to EAX
btr eax,ebx               ; Check EAX.21=0
jc NoCpuId                ; Go if cannot set EFLAGS.21=0
;--- Execute CPUID function 0, returned result in EAX ---
xor eax,eax               ; EAX = Function number for CPUID instruction
cpuid                     ; Execute CPUID function 0
;--- Exit points ---
ExitCpuId:
pop edx ecx ebx
ret                       ; Return, at this point CF=0(NC) after XOR EAX,EAX
NoCpuId:
stc                       ; CF=1(C) means error
jmp ExitCpuId 

