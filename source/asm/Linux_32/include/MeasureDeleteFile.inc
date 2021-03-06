;---------- Delele file -------------------------------------------------------;
;                                                                              ;
; Input:   ESI = Pointer to IPB (Input Parameters Block)                       ;
;                IPB[] = Input buffer                                          ;
;          EDI = Pointer to OPB (Output Parameters Block)                      ;
; Output:  EAX = JNI Status: 0=Error, 1=Linux32 JNI OK                         ;
;                set externally from this subroutine                           ;
;                OPB[] = Output buffer                                         ;
;                                                                              ;
; Memory buffer for file I/O allocated by Java Code,                           ;
; base address of this buffer = OPB+4096                                       ;
; size of this buffer = I/O block size + 4096, for alignment reservation       ;  
; For Delete: source file parameters used, destination file parameters ignored ;
;                                                                              ;
; IPB layout (4096 bytes):                                                     ;
; DWORD [IPB+00] = Required function ID                                        ;
; DWORD [IPB+04] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+08] = Required read/write/copy operation size, bytes              ;
; DWORD [IPB+12] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+16] = I/O block size, per OS API function call, bytes             ;
; DWORD [IPB+20] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+24] = Source file performance attributes,                         ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; DWORD [IPB+28] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+32] = Destination file performance attributes,                    ; 
;                  see Linux Sys Call Reference, OPEN function, parm#2         ;
; DWORD [IPB+36] = Reserved for x64 layout compatibility                       ;
; DWORD [IPB+40] = Number of measurement repeats iterations                    ;
; DWORD [IPB+44] = Reserved for x64 layout compatibility                       ;
; Max. 976 BYTES [IPB+48] = Reserved                                           ; 
; 1024 BYTES [IPB+1024] = Source file path string, null terminated             ;
; 1024 BYTES [IPB+2048] = Destination file path string, null terminated        ;
; 1024 BYTES [IPB+3072] = Reserved                                             ;  
;                                                                              ;
; OPB layout (4096 bytes):                                                     ;
; DWORD [OPB+00] = Memory buffer base address, available after alignment       ;
; DWORD [OPB+04] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+08] = Memory buffer size, bytes, available after alignment        ;       
; DWORD [OPB+12] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+16] = Source file handle, available after open file               ;
; DWORD [OPB+20] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+24] = Destination file handle, available after open file          ;
; DWORD [OPB+28] = Reserved for x64 layout compatibility                       ;
; DWORD [OPB+32] = Executed read/write/copy operation size,                    ; 
;                  available after operation execute                           ;
;                  This is QWORD even for 32-bit version                       ;
; QWORD [OPB+40] = Timer delta for measurement repeats, units = 1 ns           ;
; DWORD [OPB+48] = Last called OS API status, see Linux Sys Call Reference     ;
; DWORD [OPB+52] = Reserved for x64 layout compatibility                       ;
; QWORD [OPB+56] = Last called OS API type:                                    ;
;    0=None,  1=Open for read,  2=Read,  3=Open for write,  4=Write,           ;
;    5=Close, 6=Delete                                                         ;
; DWORD [OPB+60] = Reserved for x64 layout compatibility                       ;
; 4024 BYTES [OPB+72] = Reserved                                               ;
;                                                                              ;
;------------------------------------------------------------------------------;
MeasureDeleteFile:
push ebx esi edi ebp

;--- Clear output variables ---
call HelperClearOPB

;--- Delete file ---
lea ebx,IPB_SRC_PATH        ; EBX = Parm#1 = Pointer to source file path
mov eax,10                  ; EAX = Function code = SYS_UNLINK
int 80h
mov ecx,STEP_DELETE
test eax,eax
jnz ErrorFileContext

;--- Go to restore stack after alignment and done ---
jmp ExitFileContext 




