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
push rdi
cld
xor eax,eax
mov ecx,OPB_CLEAR_QWORDS
rep stosq
pop rdi
ret

;--- Initializing data transfer area aligned base address and size ---;
;                                                                     ;
; Input:   RSI = Pointer to IPB, used for this routine                ;
;          RDI = Pointer to OPB, used for this routine                ;
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
mov OPB_BUFFER_BASE,rbx        ; RBX = Work buffer base
mov rbp,IPB_BLOCK_SIZE
mov OPB_BUFFER_SIZE,rbp        ; RBP = Work buffer size
ret

;--- Helpers for open and create files -------------------------------;
;                                                                     ;
; Input:   RSI = Pointer to IPB, used for this routine                ;
;          RDI = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           IPB_SRC_PATH, IPB_DST_PATH                                ;
;           IPB_SRC_ATTRIBUTES, IPB_DST_ATTRIBUTES                    ;
;                                                                     ;
; Output:  RAX = Opened file handle, or 0 if error                    ;
;          variables:                                                 ;
;           OPB_SRC_HANDLE                                            ;
;           OPB_DST_HANDLE                                            ;
;---------------------------------------------------------------------;

;--- Entry #4 = open Source file for Read, use complex path string ---
; HelperOpenSrcGroup:
; lea rcx,OPB_COMPLEX_PATH                ; RCX = Parm#1 = Pointer to file path
; mov edx,GENERIC_READ OR GENERIC_WRITE   ; RDX = Parm#2 = Desired access
; jmp L1

;--- Entry #5 = create Source file for Write, use complex path string ---
HelperCreateSrcGroup:
lea rcx,OPB_COMPLEX_PATH                ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ OR GENERIC_WRITE   ; RDX = Parm#2 = Desired access
jmp L2

;--- Entry #1 = open Source file for Read ---
HelperOpenSrcRead:
lea rcx,IPB_SRC_PATH           ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_READ           ; RDX = Parm#2 = Desired access
; L1:
mov eax,OPEN_EXISTING          ; value for Parm#5 = Creation disposition
jmp L0

;--- Entry #2 = create Source file for Write ---
HelperCreateSrcWrite:
lea rcx,IPB_SRC_PATH           ; RCX = Parm#1 = Pointer to file path
mov edx,GENERIC_WRITE          ; RDX = Parm#2 = Desired access
L2:
mov eax,CREATE_ALWAYS          ; value for Parm#5 = Creation disposition

;--- Common branch ---
L0:
push rbp
mov rbp,rsp                    ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h     ; align 16, required for Win API
push rbp                       ; note care about alignment after this push
sub rsp,32+24                  ; make aligned parameter shadow, plus >4 params
;--- Prepare parameters and call WinAPI ---
xor r8d,r8d                    ; R8     = Parm#3 = Share mode = 0
xor r9d,r9d                    ; R9     = Parm#4 = Security attributes = 0
mov PARM_5,rax                 ; Parm#5 = Creation disposition
mov eax,FILE_ATTRIBUTE_NORMAL
test IPB_SRC_ATTRIBUTES,00000001b
jz @f
or eax,FILE_FLAG_NO_BUFFERING
@@:
test IPB_SRC_ATTRIBUTES,00000010b
jz @f
or eax,FILE_FLAG_WRITE_THROUGH
@@:
mov PARM_6,rax                 ; Parm#6 = File attribute and flags 
mov PARM_7,r8                  ; Parm#7 = 0 = Template file handle, not used
call [CreateFileA]
;--- Update file handle ---
mov OPB_SRC_HANDLE,rax
;--- Return from subroutine ---
HelperReturn:
add rsp,24+32  ; remove parameters shadow plus >4 parameters for Win API
pop rbp        ; restore callee RBP with original RSP backup
mov rsp,rbp    ; restore RSP after align 16
pop rbp        ; restore caller RBP 
ret

;--- Entry #6 = open Destination file for Write, use complex path string --- 
HelperCreateDstGroup:
lea rcx,OPB_COMPLEX_PATH       ; RCX    = Parm#1 = Pointer to file path
jmp L3

;--- Entry #3 = create Destination file for Write ---
HelperCreateDstWrite:
lea rcx,IPB_DST_PATH           ; RCX    = Parm#1 = Pointer to file path
L3:

push rbp
mov rbp,rsp                    ; save RSP
and rsp,0FFFFFFFFFFFFFFF0h     ; align 16, required for Win API
push rbp                       ; note care about alignment after this push
sub rsp,32+24                  ; make aligned parameter shadow, plus >4 params
;--- Prepare parameters and call WinAPI ---
mov edx,GENERIC_WRITE          ; RDX    = Parm#2 = Desired access
xor r8d,r8d                    ; R8     = Parm#3 = Share mode = 0
xor r9d,r9d                    ; R9     = Parm#4 = Security attributes = 0
mov PARM_5,CREATE_ALWAYS       ; Parm#5 = Creation disposition
mov eax,FILE_ATTRIBUTE_NORMAL
test IPB_DST_ATTRIBUTES,00000001b
jz @f
or eax,FILE_FLAG_NO_BUFFERING
@@:
test IPB_DST_ATTRIBUTES,00000010b
jz @f
or eax,FILE_FLAG_WRITE_THROUGH
@@:
mov PARM_6,rax                 ; Parm#6 = File attribute and flags 
mov PARM_7,r8                  ; Parm#7 = 0 = Template file handle, not used
call [CreateFileA]
;--- Update file handle ---
mov OPB_DST_HANDLE,rax
;--- Return from subroutine ---
jmp HelperReturn

;--- Helper for calculation time interval by timer results -----------;
;                                                                     ;
; Input:   RSI = Pointer to IPB, reserved, not used by this routine   ;
;          RDI = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           OPB_TRANSIT_T1, OPB_TRANSIT_T2                            ;
;                                                                     ;
; Output:  RAX = Timer delta value, units = 1 ns                      ;  
;          variables:                                                 ;
;           OPB_TIMER_DELTA                                           ;
;---------------------------------------------------------------------;
HelperDeltaTime:
mov rax,OPB_TRANSIT_T2
sub rax,OPB_TRANSIT_T1
imul rax,rax,100
mov OPB_TIMER_DELTA,rax
ret

