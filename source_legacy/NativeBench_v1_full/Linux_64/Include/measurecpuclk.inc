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
;          RAX = TSC Frequency, Hz, F = Delta TSC per 1 second           ;
;          RDX = TSC Period, Fs, T=1/F                                   ;
;------------------------------------------------------------------------;
MeasureCpuClk:
push rcx rsi rdi r8 r9 r10 r11
;--- Prepare parameters, early to minimize dTSC ---
lea rdi,[TimespecWait]    ; RDI = Pointer to loaded wait time: DQ sec, ns
lea rsi,[rdi+16]          ; RSI = Pointer to stored remain time: DQ sec, ns
;--- Get TSC value before 1 second pause ---
rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low
push rax rdx
;--- Wait 1 second ---
mov eax,SYS_NANOSLEEP     ; EAX = Linux API function (syscall number)
push rsi
syscall
pop rsi
xchg r8,rax
;--- Get TSC value after 1 second pause ---
rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low , BEFORE 1 second pause
pop rcx rdi               ; ECX:EDI = TSC, ECX = High , EBX = Low , AFTER 1 second pause
;--- Check results ---
test r8,r8
jnz TimerFailed           ; Go if error returned or wait interrupted
mov r8,[rsi+00]           ; RAX = Time remain, seconds
or  r8,[rsi+08]           ; RAX = Disjunction with TIme remain, nanoseconds
jnz TimerFailed           ; Go if remain time stored by function
;--- Calculate delta-TSC per 1 second = TSC frequency ---
sub eax,edi               ; Subtract: DeltaTSC.Low  = EndTSC.Low - StartTSC.Low
sbb edx,ecx               ; Subtract: DeltaTSC.High = EndTSC.High - StartTSC.High - Borrow
;--- Extract TSC frequency as 64-bit value ---
shl rdx,32
lea rcx,[rax+rdx]
jrcxz TimerFailed
;--- Store frequency, calculate period T=1/F ---
push rcx
mov rax,1000000000000000        ; RAX = Femtoseconds per one second
cqo                             ; RDX = 0, dividend high 64 bits
div rcx                         ; T=1/F, because RAX=10^15 here T=Femtoseconds
;--- Make RDX = Period, RAX = Frequency ---
xchg rdx,rax
pop rax
;--- Exit points ---
clc
TimerDone:
pop r11 r10 r9 r8 rdi rsi rcx
ret
TimerFailed:
stc
jmp TimerDone


