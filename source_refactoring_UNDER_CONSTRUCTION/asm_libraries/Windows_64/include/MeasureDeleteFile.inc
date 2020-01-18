;---------- Delete file -------------------------------------------------------;
;                                                                              ;
; Input:   RSI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          RDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  RAX = JNI Status: 0=Error, 2=Win64 JNI OK                           ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O allocated by Java Code,                           ;
; base address of this buffer = OPB+4096                                       ;
; size of this buffer = I/O block size + 4096, for alignment reservation       ;  
; For Copy: both source and destination files parameters used                  ;
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
; QWORD [IPB+00] = Required function ID                                        ;
; QWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; QWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; QWORD [IPB+24] = Source file performance attributes,                         ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;                  see MSDN, CreateFileA API parm#6                            ;
; QWORD [IPB+32] = Destination file performance attributes,                    ;
;                  D0 = Read sync, D1 = Write Sync, other reserved             ; 
;                  see MSDN, CreateFileA API parm#6                            ;
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
; QWORD [OPB+48] = Last called OS API status, see MSDN                         ;
; QWORD [OPB+56] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
;                                                                              ;
; DATA AREA from [OPB+64] to [OPB+128] can be used for                         ; 
; OS-scefific undocumented scratch pads                                        ;                                                                   
; Use OS-specific scratch pad at OPB[80-95], 16 bytes (for timer API)          ;
; 3968 BYTES [OPB+128] = Reserved                                              ;
;                                                                              ;
;------------------------------------------------------------------------------;
MeasureDeleteFile:
push rbx rsi rdi rbp r12 r13 r14 r15
;--- Clear output variables ---
call HelperClearOPB
;--- Alignment stack and reserve stack space for WinAPI, 32 bytes ---
mov rbp,rsp                   ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h    ; align 16
push rbp          ; must be aligned by 16 with next operation
sub rsp,32+24     ; make parameter shadow plus >4 parameters for Win API
;--- Delete file ---
lea rcx,IPB_SRC_PATH           ; RCX    = Parm#1 = Pointer to file path
call [DeleteFileA]
test rax,rax
mov eax,STEP_DELETE 
jz ErrorFileContext
jmp ExitFileContext

