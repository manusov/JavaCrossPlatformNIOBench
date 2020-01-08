;---------- Delete file with measurement iterations ---------------------------;
;                                                                              ;
; Input:   RSI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Linux64 JNI OK                         ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O allocated by Java Code,                           ;
; base address of this buffer = OPB+4096                                       ;
; size of this buffer = I/O block size + 4096, for alignment reservation       ;  
; For Delete: source file parameters used, destination file parameters ignored ;
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
; QWORD [IPB+00] = Required function ID                                        ;
; QWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; QWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; QWORD [IPB+24] = Source file performance attributes,                         ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ;  
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; QWORD [IPB+32] = Destination file performance attributes,                    ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ;  
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; QWORD [IPB+40] = Number of measurement repeats iterations                    ;
; Max. 976 BYTES [IPB+48] = Reserved                                           ; 
; 1024 BYTES [IPB+1024] = Source file path string, null terminated             ;
; 1024 BYTES [IPB+2048] = Destination file path string, null terminated        ;
; 1024 BYTES [IPB+3072] = Reserved                                             ;  
;                                                                              ;
; OPB layout (4096 bytes):                                                     ;
; QWORD [OPB+00] = Memory buffer base address, available after alignment       ;
; QWORD [OPB+08] = Memory buffer size, bytes, available after alignment        ;       
; QWORD [OPB+16] = Source file handle, available after open file               ;
; QWORD [OPB+24] = Destination file handle, available after open file          ;
; QWORD [OPB+32] = Executed read/write/copy operation size,                    ; 
;                  available after operation execute                           ;
; QWORD [OPB+40] = Timer delta for measurement repeats, units = 1 ns           ;
; QWORD [OPB+48] = Last called OS API status, see Linux Sys Call Reference     ;
; QWORD [OPB+56] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
; 4024 BYTES [IPB+72] = Reserved                                               ;
;                                                                              ;
;------------------------------------------------------------------------------;
MeasureDeleteFile:
push rbx rbx rsi rdi rbp r12 r13 r14 r15       ; RBX push twice for RSP align

;--- Copy parameters to non-volatile registers ---
mov r14,rsi                 ; R14 = Pointer to IPB
mov r15,rdi                 ; R15 = Pointer to OPB

;--- Clear output variables ---
cld
xor eax,eax
mov ecx,OPB_CLEAR_QWORDS
rep stosq

lea rdi,IPB_SRC_PATH        ; Parm#1 = Pointer to source file path
mov eax,SYS_UNLINK
syscall
mov ecx,STEP_DELETE
test rax,rax
jnz ErrorFileContext

;--- Go to restore stack after alignment and done ---
jmp ExitFileContext 
