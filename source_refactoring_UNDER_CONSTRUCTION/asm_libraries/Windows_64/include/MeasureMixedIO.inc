; === UNDER CONSTRUCTION ===


MeasureMixedIO:
push rbx rsi rdi rbp r12 r13 r14 r15
;--- Clear output variables ---
push rdi
cld
xor eax,eax
mov ecx,OPB_CLEAR_QWORDS
rep stosq
pop rdi
;--- Alignment stack and reserve stack space for WinAPI, 32 bytes ---
mov rbp,rsp                   ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h    ; align 16
push rbp          ; must be aligned by 16 with next operation
sub rsp,32+24     ; make parameter shadow plus >4 parameters for Win API
;
;
jmp ExitFileContext

