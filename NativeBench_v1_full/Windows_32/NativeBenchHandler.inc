;--- File operations benchmarks: Write, Read, Copy, Delete --------------------; 
; Input:  ESI = Pointer to IPB (Input Parameters Block)                        ;
;         EDI = Pointer to OPB (Output Parameters Block)                       ;
;         Data at IPB                                                          ;
;                                                                              ;
; Output: RAX = Reserved for status                                            ;
;         Data at OPB                                                          ;
;                                                                              ;
; IPB:    DWORD IPB[00] = Reserved for subfunction code                        ;
;         DWORD IPB[04] = Reserved                                             ;
;         QWORD IPB[08] = Block size                                           ;
;         QWORD IPB[16] = Blocks count                                         ;
;         QWORD IPB[24] = Source file attributes                               ;
;         QWORD IPB[32] = Destination file attributes                          ;
;         QWORD IPB[40] = Measurement iterations count
;         976 bytes IPB[48-1023] = Source file path string                     ;
;         1024 bytes IPB[1024-2047] = Destination file path string             ;
;         BLOCK_SIZE [800h...X] = Source file I/O buffer                       ;
;                                                                              ;
; OPB:    QWORD OPB[00] = File write result, 0=Error, otherwise dt units 100 ns;
;         QWORD OPB[08] = File read result, 0=Error, otherwise dt units 100ns  ; 
;         QWORD OPB[16] = File copy result, 0=Error, otherwise dt units 100 ns ; 
;         QWORD OPB[24] = File delete result, 0=Error, otherwise > 0           ;
;         QWORD OPB[32] = Used for API function return size                    ;
;         QWORD OPB[40] = Used for API function return time, start point       ;
;         QWORD OPB[48] = Used for API function return time, stop point        ;
;         1992 bytes OPB [56-2047] = Reserved                                  ;
;         DWORD [56] = Temporary storage for Block Size (replace R12)          ;
;         DWORD [60] = Temporary storage for Blocks Count (Replace R13)        ;
;         DWORD [64] = Temporary storage for Measurements Repeats              ;
;         BLOCK_SIZE [800h...X] = Destination file I/O buffer                  ;
;                                                                              ;
;------------------------------------------------------------------------------;

API_WRITE_READ_COPY_DELETE:
push ebx esi edi ebp
;--- Clear output variables ---
cld
xor eax,eax
mov ecx,7*2
rep stosd
;--- Create source file ---
xor eax,eax
push eax                            ; Parm#7 = Template file handle, not used 
push dword [esi+24]                 ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                  ; Parm#5 = Creation disposition
push eax                            ; Parm#4 = Security attributes = 0
push eax                            ; Parm#3 = Share mode = 0
push GENERIC_READ OR GENERIC_WRITE  ; Parm#2 = Desired access
lea eax,[esi+48]
push eax                            ; Parm#1 = Pointer to file path
call [CreateFileA]
test eax,eax                        ; Check EAX = Handle
jz .Skip_Operation                  ; Go if error create file
xchg ebx,eax                        ; EBX = Source file handle, XCHG compact
;--- Get time at file WRITE start ---
lea eax,[edi-16]
push eax
call [GetSystemTimeAsFileTime]
;--- Measurement cycle for WRITE ---
mov eax,[esi+40]
mov [edi+08],eax
.WriteMeasurement:
;--- Prepare parameters for WRITE operation cycle ---
mov eax,[esi+08]              ; Block size
mov [edi+00],eax
mov eax,[esi+16]              ; Block count
mov [edi+04],eax
;--- Write source file ---
.WriteBlock:
xor eax,eax
push eax                      ; Parm#5 = Pointer to overlapped
lea ecx,[edi-24]                   
push ecx                      ; Parm#4 = Pointer for size return
mov [ecx+0],eax
mov [ecx+4],eax
push dword [edi+00]           ; Parm#3 = Block size
lea ecx,[esi+2048]
push ecx                      ; Parm#2 = Pointer to buffer
push ebx                      ; Parm#1 = Source file handle
call [WriteFile]
test eax,eax                  ; Check EAX = Status
jz .Skip_Operation            ; Go if error write file
dec dword [edi+04]            ; Decrement temporary variable = blocks count
jnz .WriteBlock               ; Cycle for blocks
;--- Positioning pointer to position=0 ---
xor eax,eax
push eax                      ; Parm#4 = Move method = FILE_BEGIN
push eax                      ; Parm#3 = Distance High = 0
push eax                      ; Parm#2 = Distance Low = 0
push ebx                      ; Parm#1 = Source file handle
call [SetFilePointer]
;--- Measurement cycle for WRITE ---
dec dword [edi+08]           ; Decrement temporary variable = measurement count
jnz .WriteMeasurement
;--- Get time at file WRITE stop, calculate delta ---
lea eax,[edi-08]
push eax
call [GetSystemTimeAsFileTime]     ; Return time stamp, units = 100 ns 
mov eax,[edi-08+00]
mov edx,[edi-08+04]
sub eax,[edi-16+00]
sbb edx,[edi-16+04]
mov [edi-56+00],eax
mov [edi-56+04],edx
;--- Get time at file READ start ---
lea eax,[edi-16]
push eax
call [GetSystemTimeAsFileTime]
;--- Measurement cycle for READ ---
mov eax,[esi+40]
mov [edi+08],eax
.ReadMeasurement:
;--- Prepare parameters for READ operation cycle ---
mov eax,[esi+08]              ; Block size
mov [edi+00],eax
mov eax,[esi+16]              ; Block count
mov [edi+04],eax
;--- Read source file ---
.ReadBlock:
xor eax,eax
push eax                      ; Parm#5 = Pointer to overlapped
lea ecx,[edi-24]                   
push ecx                      ; Parm#4 = Pointer for size return
mov [ecx+0],eax
mov [ecx+4],eax
push dword [edi+00]           ; Parm#3 = Block size
lea ecx,[edi+2048-56]
push ecx                      ; Parm#2 = Pointer to buffer
push ebx                      ; Parm#1 = Source file handle
call [ReadFile]
test eax,eax                  ; Check EAX = Status
jz .Skip_Operation            ; Go if error write file
dec dword [edi+04]            ; Decrement temporary variable = blocks count
jnz .ReadBlock                ; Cycle for blocks
;--- Positioning pointer to position=0 ---
xor eax,eax
push eax                      ; Parm#4 = Move method = FILE_BEGIN
push eax                      ; Parm#3 = Distance High = 0
push eax                      ; Parm#2 = Distance Low = 0
push ebx                      ; Parm#1 = Source file handle
call [SetFilePointer]
;--- Measurement cycle for READ ---
dec dword [edi+08]           ; Decrement temporary variable = measurement count
jnz .ReadMeasurement
;--- Get time at file READ stop, calculate delta ---
lea eax,[edi-08]
push eax
call [GetSystemTimeAsFileTime]     ; Return time stamp, units = 100 ns 
mov eax,[edi-08+00]
mov edx,[edi-08+04]
sub eax,[edi-16+00]
sbb edx,[edi-16+04]
mov [edi-48+00],eax
mov [edi-48+04],edx
;--- Create destination file ---
xor eax,eax                                     
push eax                            ; Parm#7 = Template file handle, not used 
push dword [esi+32]                 ; Parm#6 = File attribute and flags
push CREATE_ALWAYS                  ; Parm#5 = Creation disposition
push eax                            ; Parm#4 = Security attributes = 0
push eax                            ; Parm#3 = Share mode = 0
push GENERIC_READ OR GENERIC_WRITE  ; Parm#2 = Desired access
lea ecx,[esi+1024]
push ecx                            ; Parm#1 = Pointer to destination file path
call [CreateFileA]
test eax,eax                        ; Check EAX = Handle
jz .Skip_Operation                  ; Go if error create file
xchg ebp,eax                        ; EBP = Destination file handle
;--- Get time at file COPY start ---
lea eax,[edi-16]
push eax
call [GetSystemTimeAsFileTime]
;--- Measurement cycle for COPY ---
mov eax,[esi+40]
mov [edi+08],eax
.CopyMeasurement:
;--- Prepare parameters for COPY operation cycle ---
mov eax,[esi+08]              ; Block size
mov [edi+00],eax
mov eax,[esi+16]              ; Block count
mov [edi+04],eax
;--- Copy source file ---
.CopyBlock:
;--- Read phase, source file ---
xor eax,eax
push eax                      ; Parm#5 = Pointer to overlapped
lea ecx,[edi-24]                   
push ecx                      ; Parm#4 = Pointer for size return
mov [ecx+0],eax
mov [ecx+4],eax
push dword [edi+00]           ; Parm#3 = Block size
lea ecx,[edi+2048-56]
push ecx                      ; Parm#2 = Pointer to buffer
push ebx                      ; Parm#1 = Source file handle
call [ReadFile]
test eax,eax                  ; Check EAX = Status
jz .Skip_Operation            ; Go if error write file
;--- Write phase, destination cycle ---
xor eax,eax
push eax                      ; Parm#5 = Pointer to overlapped
lea ecx,[edi-24]                   
push ecx                      ; Parm#4 = Pointer for size return
mov [ecx+0],eax
mov [ecx+4],eax
push dword [edi+00]           ; Parm#3 = Block size
lea ecx,[edi+2048-56]
push ecx                      ; Parm#2 = Pointer to buffer
push ebp                      ; Parm#1 = Destination file handle
call [WriteFile]
test eax,eax                  ; Check EAX = Status
jz .Skip_Operation            ; Go if error write file
;--- Cycle for Read/Write phases ---
dec dword [edi+04]            ; Decrement temporary variable = blocks count
jnz .CopyBlock                ; Cycle for blocks
;--- Positioning pointer to position=0 ---
;--- Source ---
xor eax,eax
push eax                      ; Parm#4 = Move method = FILE_BEGIN
push eax                      ; Parm#3 = Distance High = 0
push eax                      ; Parm#2 = Distance Low = 0
push ebx                      ; Parm#1 = Source file handle
call [SetFilePointer]
;--- Destination ---
xor eax,eax
push eax                      ; Parm#4 = Move method = FILE_BEGIN
push eax                      ; Parm#3 = Distance High = 0
push eax                      ; Parm#2 = Distance Low = 0
push ebp                      ; Parm#1 = Destination file handle
call [SetFilePointer]
;--- Measurement cycle for COPY ---
dec dword [edi+08]           ; Decrement temporary variable = measurement count
jnz .CopyMeasurement
;--- Get time at file COPY stop, calculate delta ---
lea eax,[edi-08]
push eax
call [GetSystemTimeAsFileTime]     ; Return time stamp, units = 100 ns 
mov eax,[edi-08+00]
mov edx,[edi-08+04]
sub eax,[edi-16+00]
sbb edx,[edi-16+04]
mov [edi-40+00],eax
mov [edi-40+04],edx
;--- Exit point with files delete ---
.Skip_Operation:
;--- Close and delete source file ---
push ebx
call [CloseHandle]
test eax,eax
jz @f
lea eax,[esi+48]                       
push eax                               ; Parm#1 = Pointer to file path
call [DeleteFileA]
@@:
;--- Close and delete destination file ---
push ebp
call [CloseHandle]
test eax,eax
jz @f
lea eax,[esi+1024]
push eax                               ; Parm#1 = Pointer to file path
call [DeleteFileA]
mov [edi-32],eax
@@:
;--- Exit ---
pop ebp edi esi ebx
ret
