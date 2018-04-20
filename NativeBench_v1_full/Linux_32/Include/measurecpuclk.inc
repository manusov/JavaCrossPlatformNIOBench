;--- Measure CPU TSC (Time Stamp Counter) clock frequency ---------------;
; Return measurement result as 2 values:                                 ;
; F=Frequency=[Hz], T=Period=[fs]. 1 femtosecond = 10^-15 seconds.       ;
; Primary measured parameter is FREQUENCY, parameter PERIOD is result    ;
; of integer division, means approximation.                              ;
; If precision required, caller must calculate 1/FREQUENCY by x87 FPU,   ;
; but don't use PERIOD.                                                  ; 
;                                                                        ;
; INPUT:   None                                                          ;
;                                                                        ;
; OUTPUT:  CF flag = Status: 0(NC)=Measured OK, 1(C)=Measurement error	 ;
;          Output RAX,RDX valid only if CF=0(NC)                         ;
;          EDX:EAX = TSC Frequency, Hz, F = Delta TSC per 1 second       ;
;          ECX:EBX = TSC Period, Fs, T=1/F (validity limited for 32-bit) ;
;------------------------------------------------------------------------;
MeasureCpuClk:
push edi esi ebp
;--- Prepare parameters, early to minimize dTSC ---
lea ebx,[TimespecWait]    ; EBX = Pointer to loaded wait time: DQ sec, ns
lea ecx,[ebx+16]          ; ECX = Pointer to stored remain time: DQ sec, ns
;--- Get TSC value before 1 second pause ---
rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low
push eax edx
;--- Wait 1 second ---
mov eax,162               ; EAX = Linux API function (syscall number) = SYS_NANOSLEEP
push ecx
int 80h
pop ecx
xchg ebx,eax
;--- Get TSC value after 1 second pause ---
rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low , BEFORE 1 second pause
pop edi esi               ; EDI:ESI = TSC, ECX = High , EBX = Low , AFTER 1 second pause
;--- Check results ---
test ebx,ebx
jnz TimerFailed           ; Go if error returned or wait interrupted
mov ebx,[ecx+00]          ; Time remain, seconds
or ebx,[ecx+04]
or ebx,[ecx+08]           ; Disjunction with Time remain, nanoseconds
or ebx,[ecx+12]
jnz TimerFailed           ; Go if remain time stored by function
;--- Calculate delta-TSC per 1 second = TSC frequency ---
sub eax,esi               ; Subtract: DeltaTSC.Low  = EndTSC.Low - StartTSC.Low
sbb edx,edi               ; Subtract: DeltaTSC.High = EndTSC.High - StartTSC.High - Borrow
test edx,edx
jnz TimerFailed           ; This debug 32-bit code not supports > 4GHz
;--- Calculate period T=1/F ---
; Femtoseconds per one second = 1000000000000000
; Low 32-bit =  A4C68000h
; High 32-bit = 00038D7Eh
mov ebx,0A4C68000h
mov ecx,000038D7Eh
cmp ecx,eax
jae TimerFailed           ; Go error if divide overflow
push eax edx
xchg eax,ebx
mov edx,ecx
div ebx                   ; Femtoseconds per second / Hz = Period in femtoseconds
xchg ebx,eax
xor ecx,ecx
pop edx eax
;--- Exit points ---
clc
TimerDone:
pop ebp esi edi
ret
TimerFailed:
stc
jmp TimerDone


