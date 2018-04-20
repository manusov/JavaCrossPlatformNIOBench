;------------------------------------------------------------------------;
; Measure CPU TSC (Time Stamp Counter) clock frequency, store results    ;
; F=Frequency=[Hz], T=Period=[fs]. 1 femtosecond = 10^-15 seconds.       ;
; Primary measured parameter is FREQUENCY, parameter PERIOD is result    ;
; of integer division, means approximation.                              ;
; If precision required, caller must calculate 1/FREQUENCY by x87 FPU,   ;
; but don't use PERIOD.                                                  ; 
;                                                                        ;
; INPUT:   None                                                          ;
;                                                                        ;
; OUTPUT:  CF flag = Status: 0(NC)=Measured OK, 1(C)=Measurement error	 ;
;          Output EAX, EBX, ECX, EDX valid only if CF=0(NC)              ;
;          ECX:EBX = TSC Frequency, Hz, F = Delta TSC per 1 second       ;
;          EDX:EAX = TSC Period, Fs, T=1/F                               ;
;------------------------------------------------------------------------;
MeasureCpuClk:
cld                             ; Clear direction, because STOS used
xor eax,eax
push esi edi ebp eax eax        ; Last EAX = Variable
;--- Start measure frequency, wait toggle ---
push ebp
call [GetSystemTimeAsFileTime]  ; Get current count
mov esi,[ebp]
@@:
push ebp
call [GetSystemTimeAsFileTime]  ; Get next count for wait 100 ns
cmp esi,[ebp]
je @b
;--- Start time point ---
mov esi,[ebp+0]
mov edi,[ebp+4]
add esi,10000000                
adc edi,0                       ; EDI:ESI = 10^7 * 100ns = 1 second
rdtsc
push eax edx                    ; Stack qword = 64-bit TSC at operation start
;--- Delay 1 second ---
@@:
push ebp
call [GetSystemTimeAsFileTime]  ; Get count for wait 1 second
cmp edi,[ebp+4]                 ; Compare high: target=edi and returned=[ebp+4] 
ja @b                           ; Go wait if target > returned, must 1 second
jb @f
cmp esi,[ebp+0]                 ; Compare low: target=esi and returned=[ebp+0] 
ja @b                           ; Go wait if target > returned, must 1 second
@@:
;--- Stop time point ---
rdtsc                           ; EDX:EAX = 64-bit TSC at operation end
xchg ebx,eax
mov ecx,edx                     ; ECX:EBX = 64-bit TSC at operation end
pop edx eax
sub ebx,eax
sbb ecx,edx                     ; ECX:EBX = Delta TSC = frequency (1 second) 
;--- Check minimum frequency errors ---
test ecx,ecx
jnz @f
cmp ebx,1000000                 ; Minimum = 1MHz
jb ExitCpuClk                   ; Go skip if error: frequency too low 
@@:
;--- Check maximum or negative frequency errors ---
cmp ecx,8                       ; Approx. 32GHz, without low dword
cmc
jc ExitCpuClk                   ; Go skip if error: frequency too high
;--- Calculate Period = 1/Frequency ---
X = 1000000000000000
XLOW  = X AND 0FFFFFFFFh
XHIGH = X SHR 32 
;---
mov esi,XLOW
mov edi,XHIGH                    ; EDI:ESI = Femtoseconds per second, 10^-15
xor eax,eax 
cdq                              ; EDX:EAX = Count femtoseconds per TSC clock  
.L0:
inc eax
jnz .L1
inc edx
.L1:
sub esi,ebx
sbb edi,ecx
jnc .L0
;--- Restore stack and registers, exit ---
; EDX:EAX = Period, ECX:EBX = Frequency, high:low
clc                              ; CF=0 (NC) means CPU clock measured OK
ExitCpuClk:
pop ebp ebp ebp edi esi          ; First POP EBP for RSP-8 only 
ret

