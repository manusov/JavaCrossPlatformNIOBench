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
GetRandomData:
mov edx,edi
mov ecx,[esi+08]
jecxz .Done 
;--- Low dword ---
.WaitQword1:
rdrand eax
jnc .WaitQword1
mov [edx],eax
;--- High dword ---
.WaitQword2:
rdrand eax
jnc .WaitQword2
mov [edx+4],eax
;--- Cycle ---
add edx,8
dec ecx
jnz .WaitQword1
.Done:
ret
