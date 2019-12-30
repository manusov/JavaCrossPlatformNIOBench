;---------- Get Random Numbers array ------------------------------------------;
; Input:   RSI = Pointer to IPB (Input Parameters Block)                       ;
;                DWORD [RSI+00] = Function code, decoded externally            ;
;                DWORD [RSI+04] = Reserved                                     ;
;                DWORD [RSI+08] = Block length, qwords                         ;
;                DWORD [RSI+12] = Reserved                                     ;
;          RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK                           ;
;                set externally from this subroutine                           ;
;          OPB[] = Output buffer                                               ;
;------------------------------------------------------------------------------; 
GetRandomData:
mov rdx,rdi         ; RDX = Base address of destination array
mov ecx,[rsi+08]    ; RCX = Length of destination array, units = QWORDS
jrcxz .Done         ; Skip if length = 0 
.WaitQword:
rdrand rax          ; RAX = Random number
jnc .WaitQword      ; Wait for RNG ready
mov [rdx],rax       ; Store random number to array
add rdx,8
dec ecx
jnz .WaitQword      ; Cycle for required length
.Done:
ret

