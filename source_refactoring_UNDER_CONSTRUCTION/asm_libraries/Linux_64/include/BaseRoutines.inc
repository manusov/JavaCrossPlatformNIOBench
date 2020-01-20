;------------------------------------------------------------------------------;
;           Library with base IO functionality routines (helpers).             ;
;   Subroutines used for non time critical fragments, outside of measurement.  ;
;      Note about additional clocks for call-return execution before use       ;
;                subroutines inside time-critical fragments.                   ;
;------------------------------------------------------------------------------;

;--- Build path string as prefix + number + postfix -----------;
; Input:   RCX = Pointer to path prefix string                 ;
;          RDX = Pointer to path postfix string                ;
;          EAX = File number                                   ;
;          R8  = Pointer to destination string for build path  ;
;                                                              ;
; Output:  Destination string at input R8 updated              ; 
;                                                              ;
; For example, "C:\TEMP\src05.bin" build as:                   ;
; prefix string  = "C:\TEMP\src", 0                            ;
; postfix string = ".bin", 0                                   ;
; file number = 5                                              ; 
;--------------------------------------------------------------;
HelperComplexPath:
push rbx rsi rdi      ; this non-volatile registers used here
mov rsi,rcx
mov rdi,r8
call StringWrite      ; write part 1 = prefix string
dec rdi               ; remove terminator byte 0
mov bl,0              ; BL = template selector for decimal print
call DecimalPrint32   ; write part 2 = file number
mov rsi,rdx
call StringWrite      ; write part 3 = postfix string  
pop rdi rsi rbx
ret

;--- Copy null-terminated string, include last byte = 0 -------;
; At context of file I/O library, this subroutine used for     ;
; generating file names src0.bin, src1.bin ... srcNNNN.bin     ;
;                                                              ; 
; Input:   RSI = Pointer to source string                      ;
;          RDI = Pointer to destination string                 ;
;                                                              ;
; Output:  RSI = Updated by string copy                        ;
;          RDI = Updated by string copy                        ;    
;--------------------------------------------------------------;
StringWrite:
push rax
cld
@@:
lodsb
stosb
cmp al,0
jne @b
pop rax
ret

;---------- Print 32-bit Decimal Number -----------------------;
; At context of file I/O library, this subroutine used for     ;
; generating file names src0.bin, src1.bin ... srcNNNN.bin     ; 
;                                                              ;
; Input:   EAX = Number value                                  ;
;          BL  = Template size, chars. 0=No template           ;
;          RDI = Destination Pointer (flat)                    ;
;                                                              ;
; Output:  RDI = New Destination Pointer (flat)                ;
;                modified because string write                 ;
;--------------------------------------------------------------;
DecimalPrint32:
cld
push rax rbx rcx rdx
mov bh,80h-10       ; BH = Variable for left zeroes print control
add bh,bl
mov ecx,1000000000  ; ECX = Service divisor
.MainCycle:
xor edx,edx
div ecx         ; Produce current digit
and al,0Fh
test bh,bh      ; This for check bit BH.7
js .FirstZero   ; Go print left zero by formatted templated output
cmp ecx,1
je .FirstZero   ; If last iteration, means low digit, print zero unconditional 
cmp al,0        ; Not actual left zero ?
jz .SkipZero    ; Go skip set flag by middle zeroes logic
.FirstZero:
mov bh,80h      ; Flag = 1, after this all digis unconditionally printed,
or al,30h       ; include zeroes 
stosb           ; Store char
.SkipZero:
push rdx        ; Push mod after division
xor edx,edx     ; EDX = Dividend, high bits
mov eax,ecx     ; EAX = Dividend, low bits
mov ecx,10      ; ECX = Divisor
div ecx 
xchg ecx,eax    ; ECX = service divisor, XCHG instead MOV, for compact encoding
pop rax         ; Pop dividend for next operation
inc bh
test ecx,ecx    ; If service divisor = 0 , operation done
jnz .MainCycle
pop rdx rcx rbx rax
ret

;--- Clear Output Parameters Block at functions entry -----------------;
;                                                                      ;
; Input:   RSI = Pointer to IPB, reserved, not used for this routine   ;
;          RDI = Pointer to OPB, used for this routine                 ;
;                                                                      ;
; Output:  None                                                        ;
;          Clears direction flag                                       ;
;----------------------------------------------------------------------;
HelperClearOPB:
cld
xor eax,eax
mov ecx,OPB_CLEAR_QWORDS
rep stosq
ret

;--- Initializing data transfer area aligned base address and size ---;
;                                                                     ;
; Input:   R14 = Pointer to IPB, used for this routine                ; 
;          R15 = Pointer to OPB, used for this routine                ;
;                                                                     ;
; Output:  RBX = Aligned pointer to data transfer area                ;
;          RBP = Size of data transfer area, bytes                    ;
;          variables updated:                                         ;
;           OPB_BUFFER_BASE                                           ;
;           OPB_BUFFER_SIZE                                           ;
;---------------------------------------------------------------------;
HelperDTA:
lea rbx,OPB_DATA_ARRAY
test ebx,0FFFh
jz @f
and rbx,0FFFFFFFFFFFFF000h
add rbx,00000000000001000h
@@:
mov OPB_BUFFER_BASE,rbx     ; RBX = Work buffer base
mov rbp,IPB_BLOCK_SIZE
mov OPB_BUFFER_SIZE,rbp     ; RBP = Work buffer size
ret

;--- Helpers for open and create files -------------------------------;
;                                                                     ;
; Input:   R14 = Pointer to IPB, used for this routine                ;
;          R15 = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           IPB_SRC_PATH, IPB_DST_PATH                                ;
;           IPB_SRC_ATTRIBUTES, IPB_DST_ATTRIBUTES                    ;
;                                                                     ;
; Output:  RAX = Opened file handle, or 0 if error                    ;
;          variables:                                                 ;
;           OPB_SRC_HANDLE                                            ;
;           OPB_DST_HANDLE                                            ;
;---------------------------------------------------------------------;

;--- Entry #5 = create Source file for Write, use complex path string ---
HelperCreateSrcGroup:
lea rdi,OPB_COMPLEX_PATH
jmp L1

;--- Entry #1 = open Source file for Read ---
HelperOpenSrcRead:
; This place reserved for customize open/create

;--- Entry #2 = create Source file for Write ---
HelperOpenSrcWrite:
; This place reserved for customize open/create

;--- Common branch ---
L0:
lea rdi,IPB_SRC_PATH        ; RDI = Parm#1 = Pointer to source file path
L1:
push rcx                    ; This for RSP alignment because API used
mov esi,FILE_FLAGS_BLANK    ; RSI = Parm#2 = File operation mode flags
test IPB_SRC_ATTRIBUTES,00000011b
jz @f
or esi,FILE_FLAG_O_DIRECT
@@:
mov edx,FILE_SHARE_MODE     ; RDX = Parm#3 = File access read-write mode flags
mov eax,SYS_OPEN            ; RAX = Function code, open file
syscall
mov OPB_SRC_HANDLE,rax      ; Store source file handle
pop rcx                     ; This for RSP alignment because API used
ret

;--- Entry #6 = create Destination file for Write, use complex path string ---
HelperCreateDstGroup:
lea rdi,OPB_COMPLEX_PATH
jmp L2

;--- Entry #3 = create Destination file for Write ---
HelperCreateDstWrite:
lea rdi,IPB_DST_PATH        ; RDI = Parm#1 = Pointer to destination file path
L2:
push rcx                    ; This for RSP alignment because API used
mov esi,FILE_FLAGS_BLANK    ; RSI = Parm#2 = File operation mode flags
test IPB_DST_ATTRIBUTES,00000011b
jz @f
or esi,FILE_FLAG_O_DIRECT
@@:
mov edx,FILE_SHARE_MODE     ; RDX = Parm#3 = File access read-write mode flags
mov eax,SYS_OPEN            ; RAX = Function code, open file
syscall
mov OPB_DST_HANDLE,rax      ; Store destination file handle
pop rcx                     ; This for RSP alignment because API used
ret

;--- Helper for calculation time interval by timer results -----------;
;                                                                     ;
; Input:   R14 = Pointer to IPB, reserved, not used by this routine   ;
;          R15 = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           OPB_TRANSIT_T1, OPB_TRANSIT_T2                            ;
;                                                                     ;
; Output:  RAX = Timer delta, units = 1 ns                            ;
;          variables:                                                 ;
;           OPB_TIMER_DELTA                                           ;
;---------------------------------------------------------------------;
HelperDeltaTime:
mov rax,1000000000          ; RAX = Nanoseconds per one second
mul OPB_TRANSIT_T1_S        ; First qword = Seconds
add rax,OPB_TRANSIT_T1_NS   ; Second qword = Nanoseconds
xchg rcx,rax
mov rax,1000000000          ; RAX = Nanoseconds per one second
mul OPB_TRANSIT_T2_S        ; First qword = Seconds
add rax,OPB_TRANSIT_T2_NS   ; Second qword = Nanoseconds
sub rax,rcx                 ; RAX = After operation time, RCX = Before operation time
mov OPB_TIMER_DELTA,rax     ; Return delta = RAX-RCX nanoseconds per COPY operation 
ret
