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
;          Output RAX,RDX valid only if CF=0(NC)                         ;
;          RAX = TSC Frequency, Hz, F = Delta TSC per 1 second           ;
;          RDX = TSC Period, Fs, T=1/F                                   ;
;------------------------------------------------------------------------;
MeasureCpuClk:
cld                         ; Clear direction, because STOSQ used
push rbx rcx rsi rbp r8 r9 r10 r11 rax	 ; R8-R11 because API, RAX = Variable
mov rbp,rsp                 ; RBP used for restore RSP and addressing variables
and rsp,0FFFFFFFFFFFFFFF0h  ; Align stack (16)
sub rsp,32                  ; Make parameters shadow
;--- Start measure frequency, wait toggle ---
mov rcx,rbp
call [GetSystemTimeAsFileTime]  ; Get current count
mov rsi,[rbp]
@@:
mov rcx,rbp
call [GetSystemTimeAsFileTime]  ; Get next count for wait 100 ns
cmp rsi,[rbp]
je @b
;--- Start time point ---
mov rsi,[rbp]
add rsi,10000000                ; RSI = 10^7 * 100ns = 1 second
rdtsc
shl rdx,32
lea rbx,[rax+rdx]               ; RBX = 64-bit TSC at operation start
;--- Delay 1 second ---
@@:
mov rcx,rbp
call [GetSystemTimeAsFileTime]  ; Get count for wait 1 second
cmp rsi,[rbp]                   ; Compare target=rsi and returned=[rbp] 
ja @b                           ; Go wait if target > returned, must 1 second
;--- Stop time point ---
rdtsc
shl rdx,32
or rax,rdx                      ; RAX = 64-bit TSC at operation end
sub rax,rbx                     ; RAX = Delta TSC = frequency (1 second)
;--- Calculate Period ---
stc                             ; Set CF=1(C), error indicator
js ExitCpuClk                   ; Go skip if error: negative dTSC 
mov rcx,rax                     ; RCX = Divisor
jrcxz ExitCpuClk                ; Go skip if error: divisor=0
;--- Store Frequency ---
push rax
mov rax,1000000000000000        ; RAX = Femtoseconds per one second
cqo                             ; RDX = 0, dividend high 64 bits
div rcx                         ; T=1/F, because RAX=10^15 here T=Femtoseconds
;--- RDX = Period, RAX = Frequency ---
xchg rdx,rax
pop rax
;--- Restore RSP, pop extra registers, exit ---
clc                             ; CF=0 (NC) means CPU clock measured OK
ExitCpuClk:
mov rsp,rbp                            ; Restore RSP after alignment and shadow
pop rbx r11 r10 r9 r8 rbp rsi rcx rbx  ; First POP RBX for RSP-8 only 
ret

