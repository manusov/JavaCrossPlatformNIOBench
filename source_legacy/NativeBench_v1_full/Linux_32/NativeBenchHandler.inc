;--- File operations benchmarks: Write, Read, Copy, Delete --------------------; 
; Input:  ESI = Pointer to IPB (Input Parameters Block)                        ;
;         EDI = Pointer to OPB (Output Parameters Block)                       ;
;         Data at IPB                                                          ;
;                                                                              ;
; Output: EAX = Reserved for status                                            ;
;         Data at OPB                                                          ;
;                                                                              ;
; IPB:    DWORD IPB[00] = Reserved for subfunction code                        ;
;         DWORD IPB[04] = Reserved                                             ;
;         QWORD IPB[08] = Block size                                           ;
;         QWORD IPB[16] = Blocks count                                         ;
;         QWORD IPB[24] = Source file attributes                               ;
;         QWORD IPB[32] = Destination file attributes                          ;
;         QWORD IPB[40] = Measurement iterations count                         ;
;         976 bytes IPB[48-1023] = Source file path string                     ;
;         1024 bytes IPB[1024-2047] = Destination file path string             ;
;         BLOCK_SIZE [800h...X] = Source file I/O buffer                       ;
;                                                                              ;
; OPB:    QWORD OPB[00] = File write result, 0=Error, otherwise dt units 1 ns  ;
;         QWORD OPB[08] = File read result, 0=Error, otherwise dt units 1 ns   ; 
;         QWORD OPB[16] = File copy result, 0=Error, otherwise dt units 1 ns   ; 
;         QWORD OPB[24] = File delete result, 0=Error, otherwise > 0           ;
;         QWORD OPB[32] = Used for API function return size or position        ;
;         QWORDs OPB[40-55] = Used for API function return time, start point   ;
;         QWORDs OPB[56-71] = Used for API function return time, stop point    ;
;         1976 bytes OPB [72-2047] = Reserved                                  ;
;         BLOCK_SIZE [800h...X] = Destination file I/O buffer                  ;
;                                                                              ;
;------------------------------------------------------------------------------;

;--- Parameters part 1 ---
V_IO_SIZE            EQU  [esp+00]    ; Parameter #1
V_IO_COUNT           EQU  [esp+04]    ; #2
V_SRC_ATTRIBUTES     EQU  [esp+08]    ; #3
V_DST_ATTRIBUTES     EQU  [esp+12]    ; #4
V_MEASUREMENT_COUNT  EQU  [esp+16]    ; #5
P_SRC_PATH           EQU  [esp+20]    ; #6
P_DST_PATH           EQU  [esp+24]    ; #7
;--- Parameters part 2 ---
P_RESULT_WRITE       EQU  [esp+28]    ; #8
P_RESULT_READ        EQU  [esp+32]    ; #9
P_RESULT_COPY        EQU  [esp+36]    ; #10
P_RESULT_DELETE      EQU  [esp+40]    ; #11
P_API_TEMP           EQU  [esp+44]    ; #12
P_TIMER_START        EQU  [esp+48]    ; #13
P_TIMER_STOP         EQU  [esp+52]    ; #14
;--- Parameters part 3 ---
P_SRC_POINTER        EQU  [esp+56]    ; #15
P_DST_POINTER        EQU  [esp+60]    ; #16
;--- Parameters part 4 ---
V_SRC_HANDLE         EQU  [esp+64]    ; #17
V_DST_HANDLE         EQU  [esp+68]    ; #18

API_WRITE_READ_COPY_DELETE:

push ebx ecx edx esi edi ebp
;--- Parameters part 4 ---
xor eax,eax
push eax            ; Value, Placeholder for source handle, parameter #18 
push eax            ; Value, Placeholder for destination handle, #17
;--- Parameters part 3 ---
lea eax,[edi+2048]  ; Pointer, Destination I/O buffer pointer, #16
push eax
lea eax,[esi+2048]  ; Pointer, Source I/O buffer pointer, #15
push eax
;--- Parameters part 2 ---
lea eax,[edi+56]
push eax            ; Pointer, API return time stop, #14
sub eax,16
push eax            ; Pointer, API return time start, #13
sub eax,8
push eax            ; Pointer, API return size/position values, #12
sub eax,8
push eax            ; Pointer, Delete result, #11
sub eax,8
push eax            ; Pointer, Copy result, #10
sub eax,8
push eax            ; Pointer, Read result, #9
sub eax,8
push eax            ; Pointer, Write result, #8
;--- Parameters part 1 ---
lea eax,[esi+1024]
push eax            ; Pointer, Destination path, #7
lea eax,[esi+48]
push eax            ; Pointer, Source path, #6
sub eax,8
push dword [eax]    ; Value, Measurement count, #5
sub eax,8
push dword [eax]    ; Value, Destination attributes, #4
sub eax,8
push dword [eax]    ; Value, Source attributes, #3
sub eax,8
push dword [eax]    ; Value, IO count, #2
sub eax,8
push dword [eax]    ; Value, IO size, #1
;--- Clear output variables ---
cld
xor eax,eax
mov ecx,9*2
rep stosd
;--- Create source file ---
mov ebx,P_SRC_PATH            ; Parm#1 = Pointer to source file path
mov ecx,V_SRC_ATTRIBUTES      ; Parm#2 = File operation mode flags
mov edx,00000C00h             ; Parm#3 = File access read-write mode flags
mov eax,5                     ; Function code = SYS_OPEN
int 80h
test eax,eax
jz .Skip_Operation
cmp eax,7FFFFFFFh
ja .Skip_Operation
mov V_SRC_HANDLE,eax
;--- Get time at file WRITE start ---
xor ebx,ebx                   ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
mov ecx,P_TIMER_START         ; Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; Function code = SYS_GETTIME
int 80h
test eax,eax
jnz .Skip_Operation
;--- Measurement cycle for WRITE ---
mov ebp,V_MEASUREMENT_COUNT   ; EBP = Measurement counter
.WriteMeasurement:
;--- Prepare parameters for WRITE operation cycle ---
mov edi,V_IO_COUNT            ; EDI = Block count
;--- Write source file ---
.WriteBlock:
mov ebx,V_SRC_HANDLE           ; Parm#1 = File descriptor, handle
mov ecx,P_SRC_POINTER          ; Parm#2 = Pointer to buffer
and ecx,0FFFFF000h             ; Page alignment
add ecx,000001000h             ; Alignment possible by input size
mov edx,V_IO_SIZE              ; Parm#3 = Buffer size, byte count per call
mov eax,4                      ; Function code = Write to file = SYS_WRITE 
push edi ebp
int 80h
pop ebp edi
test eax,eax                   ; Check EAX = Status
jz .Skip_Operation             ; Go if error write file, length = 0
js .Skip_Operation             ; Go if error write file, length < 0, status
dec edi
jnz .WriteBlock                ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov ebx,V_SRC_HANDLE           ; Parm#1 = File descriptor, handle
xor ecx,ecx                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
mov esi,P_API_TEMP             ; Parm#4 = Pointer to variable return position (extra for this function)
xor edi,edi                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin (extra for this function)
mov eax,19                     ; Function code = seek = SYS_LSEEK
push ebp
int 80h
pop ebp
test eax,eax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Measurement cycle for WRITE ---
dec ebp
jnz .WriteMeasurement
;--- Get time at file WRITE stop, calculate delta ---
xor ebx,ebx                   ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
mov ecx,P_TIMER_STOP          ; Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; Function code = SYS_GETTIME
int 80h
test eax,eax
jnz .Skip_Operation
mov esi,P_TIMER_START
mov edi,P_TIMER_STOP
mov eax,[edi+0]
sub eax,[esi+0]               ; EAX = Delta seconds
mov ebx,1000000000            ; EBX = Nanoseconds per second
mul ebx                       ; EDX:EAX = Delta nanoseconds, addend #1 by seconds
xchg ebx,eax
mov ecx,edx                   ; ECX:EBX = Delta nanoseconds, addend #1 by seconds
mov eax,[edi+4]
sub eax,[esi+4]               ; EDX:EAX = Delta nanoseconds, addend #2 by nanoseconds
cdq
add eax,ebx
adc edx,ecx                   ; EDX:EAX = sum of addends #1, #2, total delta in nanoseconds 
mov ecx,P_RESULT_WRITE
mov [ecx+0],eax
mov [ecx+4],edx
;--- Get time at file READ start ---
xor ebx,ebx                   ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
mov ecx,P_TIMER_START         ; Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; Function code = SYS_GETTIME
int 80h
test eax,eax
jnz .Skip_Operation
;--- Measurement cycle for READ ---
mov ebp,V_MEASUREMENT_COUNT   ; EBP = Measurement counter
.ReadMeasurement:
;--- Prepare parameters for READ operation cycle ---
mov edi,V_IO_COUNT            ; EDI = Block count
;--- Read source file ---
.ReadBlock:
mov ebx,V_SRC_HANDLE           ; Parm#1 = File descriptor, handle
mov ecx,P_DST_POINTER          ; Parm#2 = Pointer to buffer
and ecx,0FFFFF000h             ; Page alignment
add ecx,000001000h             ; Alignment possible by input size
mov edx,V_IO_SIZE              ; Parm#3 = Buffer size, byte count per call
mov eax,3                      ; Function code = Read from file = SYS_READ
push edi ebp
int 80h
pop ebp edi
test eax,eax                   ; Check RAX = Status
jz .Skip_Operation             ; Go if error read file, length = 0
js .Skip_Operation             ; Go if error read file, length < 0, status
dec edi
jnz .ReadBlock                 ; Cycle for blocks
;--- Positioning pointer to position=0 ---
mov ebx,V_SRC_HANDLE           ; Parm#1 = File descriptor, handle
xor ecx,ecx                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
mov esi,P_API_TEMP             ; Parm#4 = Pointer to variable return position (extra for this function)
xor edi,edi                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin (extra for this function)
mov eax,19                     ; Function code = seek = SYS_LSEEK
push ebp
int 80h
pop ebp
test eax,eax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Measurement cycle for READ ---
dec ebp
jnz .ReadMeasurement
;--- Get time at file READ stop, calculate delta ---
xor ebx,ebx                   ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
mov ecx,P_TIMER_STOP          ; Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; Function code = SYS_GETTIME
int 80h
test eax,eax
jnz .Skip_Operation
mov esi,P_TIMER_START
mov edi,P_TIMER_STOP
mov eax,[edi+0]
sub eax,[esi+0]               ; EAX = Delta seconds
mov ebx,1000000000            ; EBX = Nanoseconds per second
mul ebx                       ; EDX:EAX = Delta nanoseconds, addend #1 by seconds
xchg ebx,eax
mov ecx,edx                   ; ECX:EBX = Delta nanoseconds, addend #1 by seconds
mov eax,[edi+4]
sub eax,[esi+4]               ; EDX:EAX = Delta nanoseconds, addend #2 by nanoseconds
cdq
add eax,ebx
adc edx,ecx                   ; EDX:EAX = sum of addends #1, #2, total delta in nanoseconds 
mov ecx,P_RESULT_READ
mov [ecx+0],eax
mov [ecx+4],edx
;--- Create destination file ---
mov ebx,P_DST_PATH            ; Parm#1 = Pointer to source file path
mov ecx,V_DST_ATTRIBUTES      ; Parm#2 = File operation mode flags
mov edx,00000C00h             ; Parm#3 = File access read-write mode flags
mov eax,5                     ; Function code = SYS_OPEN
int 80h
test eax,eax
jz .Skip_Operation
cmp eax,7FFFFFFFh
ja .Skip_Operation
mov V_DST_HANDLE,eax
;--- Get time at file COPY start ---
xor ebx,ebx                   ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
mov ecx,P_TIMER_START         ; Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; Function code = SYS_GETTIME
int 80h
test eax,eax
jnz .Skip_Operation
;--- Measurement cycle for COPY ---
mov ebp,V_MEASUREMENT_COUNT   ; EBP = Measurement counter
.CopyMeasurement:
;--- Prepare parameters for COPY operation cycle ---
mov edi,V_IO_COUNT            ; EDI = Block count
;--- Copy source file to destination cycle ---
.CopyBlock:
;--- Read phase, source file ---
mov ebx,V_SRC_HANDLE           ; Parm#1 = File descriptor, handle
mov ecx,P_DST_POINTER          ; Parm#2 = Pointer to buffer
and ecx,0FFFFF000h             ; Page alignment
add ecx,000001000h             ; Alignment possible by input size
mov edx,V_IO_SIZE              ; Parm#3 = Buffer size, byte count per call
mov eax,3                      ; Function code = Read from file = SYS_READ
push edi ebp
int 80h
pop ebp edi
test eax,eax                   ; Check RAX = Status
jz .Skip_Operation             ; Go if error read file, length = 0
js .Skip_Operation             ; Go if error read file, length < 0, status
;--- Write phase, destination cycle ---
mov ebx,V_DST_HANDLE           ; Parm#1 = File descriptor, handle
mov ecx,P_DST_POINTER          ; Parm#2 = Pointer to buffer
and ecx,0FFFFF000h             ; Page alignment
add ecx,000001000h             ; Alignment possible by input size
mov edx,V_IO_SIZE              ; Parm#3 = Buffer size, byte count per call
mov eax,4                      ; Function code = Write to file = SYS_WRITE 
push edi ebp
int 80h
pop ebp edi
test eax,eax                   ; Check EAX = Status
jz .Skip_Operation             ; Go if error write file, length = 0
js .Skip_Operation             ; Go if error write file, length < 0, status
;--- Cycle for Read/Write phases ---
dec edi
jnz .CopyBlock                 ; Cycle for blocks
;--- Positioning pointer to position=0 ---
;--- Source ---
mov ebx,V_SRC_HANDLE           ; Parm#1 = File descriptor, handle
xor ecx,ecx                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
mov esi,P_API_TEMP             ; Parm#4 = Pointer to variable return position (extra for this function)
xor edi,edi                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin (extra for this function)
mov eax,19                     ; Function code = seek = SYS_LSEEK
push ebp
int 80h
pop ebp
test eax,eax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Destination ---
mov ebx,V_DST_HANDLE           ; Parm#1 = File descriptor, handle
xor ecx,ecx                    ; Parm#2 = File offset, high = 0
xor edx,edx                    ; Parm#3 = File offset, low = 0
mov esi,P_API_TEMP             ; Parm#4 = Pointer to variable return position (extra for this function)
xor edi,edi                    ; Parm#5 = Whence, 0 means SEEK_SET = from file begin (extra for this function)
mov eax,19                     ; Function code = seek = SYS_LSEEK
push ebp
int 80h
pop ebp
test eax,eax                   ; Check RAX = Status
jnz .Skip_Operation            ; Go if error seek file
;--- Measurement cycle for COPY ---
dec ebp
jnz .CopyMeasurement
;--- Get time at file COPY stop, calculate delta ---
xor ebx,ebx                   ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
mov ecx,P_TIMER_STOP          ; Parm#2 = Pointer to output DQ sec, ns
mov eax,265                   ; Function code = SYS_GETTIME
int 80h
test eax,eax
jnz .Skip_Operation
mov esi,P_TIMER_START
mov edi,P_TIMER_STOP
mov eax,[edi+0]
sub eax,[esi+0]               ; EAX = Delta seconds
mov ebx,1000000000            ; EBX = Nanoseconds per second
mul ebx                       ; EDX:EAX = Delta nanoseconds, addend #1 by seconds
xchg ebx,eax
mov ecx,edx                   ; ECX:EBX = Delta nanoseconds, addend #1 by seconds
mov eax,[edi+4]
sub eax,[esi+4]               ; EDX:EAX = Delta nanoseconds, addend #2 by nanoseconds
cdq
add eax,ebx
adc edx,ecx                   ; EDX:EAX = sum of addends #1, #2, total delta in nanoseconds 
mov ecx,P_RESULT_COPY
mov [ecx+0],eax
mov [ecx+4],edx
;--- Exit point with files delete ---
.Skip_Operation:
;--- Close and delete source file ---
mov ebx,V_SRC_HANDLE           ; Parm#1 = Source file descriptor, handle
mov eax,6                      ; Function code = SYS_CLOSE
int 80h
test eax,eax
jnz @f
mov ebx,P_SRC_PATH             ; Parm#1 = Pointer to source file path
mov eax,10                     ; Function code = SYS_UNLINK
int 80h
test eax,eax
jnz @f
@@:
;--- Close and delete destination file ---
mov ebx,V_DST_HANDLE           ; Parm#1 = Source file descriptor, handle
mov eax,6                      ; Function code = SYS_CLOSE
int 80h
test eax,eax
jnz @f
mov ebx,P_DST_PATH             ; Parm#1 = Pointer to source file path
mov eax,10                     ; Function code = SYS_UNLINK
int 80h
test eax,eax
jnz @f
mov ecx,P_RESULT_DELETE
inc dword [ecx]
@@:
;--- Exit ---
.Exit_Operation:
add esp,72
pop ebp edi esi edx ecx ebx
ret


