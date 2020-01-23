;------------------------------------------------------------------------------;
;           Library with base IO functionality routines (helpers).             ;
;   Subroutines used for non time critical fragments, outside of measurement.  ;
;      Note about additional clocks for call-return execution before use       ;
;                subroutines inside time-critical fragments.                   ;
;------------------------------------------------------------------------------;

;--- Clear Output Parameters Block at functions entry -----------------;
;                                                                      ;
; Input:   ESI = Pointer to IPB, reserved, not used for this routine   ;
;          EDI = Pointer to OPB, used for this routine                 ;
;                                                                      ;
; Output:  None                                                        ;
;          Clears direction flag                                       ;
;----------------------------------------------------------------------;
HelperClearOPB:
push edi
cld
xor eax,eax
mov ecx,OPB_CLEAR_DWORDS
rep stosd
pop edi
ret

;--- Build path string as prefix + number + postfix -----------;
; Input:   ECX = Pointer to path prefix string                 ;
;          EDX = Pointer to path postfix string                ;
;          EAX = File number                                   ;
;          EBX = Pointer to destination string for build path  ;
;                                                              ;
; Output:  Destination string at input R8 updated              ; 
;                                                              ;
; For example, "C:\TEMP\src05.bin" build as:                   ;
; prefix string  = "C:\TEMP\src", 0                            ;
; postfix string = ".bin", 0                                   ;
; file number = 5                                              ; 
;--------------------------------------------------------------;
HelperComplexPath:
push ebx esi edi      ; this non-volatile registers used here
mov esi,ecx
mov edi,ebx
call StringWrite      ; write part 1 = prefix string
dec edi               ; remove terminator byte 0
mov bl,0              ; BL = template selector for decimal print
call DecimalPrint32   ; write part 2 = file number
mov esi,edx
call StringWrite      ; write part 3 = postfix string  
pop edi esi ebx
ret

;--- Copy null-terminated string, include last byte = 0 -------;
; At context of file I/O library, this subroutine used for     ;
; generating file names src0.bin, src1.bin ... srcNNNN.bin     ;
;                                                              ; 
; Input:   ESI = Pointer to source string                      ;
;          EDI = Pointer to destination string                 ;
;                                                              ;
; Output:  ESI = Updated by string copy                        ;
;          EDI = Updated by string copy                        ;    
;--------------------------------------------------------------;
StringWrite:
push eax
cld
@@:
lodsb
stosb
cmp al,0
jne @b
pop eax
ret

;---------- Print 32-bit Decimal Number -----------------------;
; At context of file I/O library, this subroutine used for     ;
; generating file names src0.bin, src1.bin ... srcNNNN.bin     ; 
;                                                              ;
; Input:   EAX = Number value                                  ;
;          BL  = Template size, chars. 0=No template           ;
;          EDI = Destination Pointer (flat)                    ;
;                                                              ;
; Output:  EDI = New Destination Pointer (flat)                ;
;                modified because string write                 ;
;--------------------------------------------------------------;
DecimalPrint32:
cld
push eax ebx ecx edx
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
push edx        ; Push mod after division
xor edx,edx     ; EDX = Dividend, high bits
mov eax,ecx     ; EAX = Dividend, low bits
mov ecx,10      ; ECX = Divisor
div ecx 
xchg ecx,eax    ; ECX = service divisor, XCHG instead MOV, for compact encoding
pop eax         ; Pop dividend for next operation
inc bh
test ecx,ecx    ; If service divisor = 0 , operation done
jnz .MainCycle
pop edx ecx ebx eax
ret

;--- Initializing data transfer area aligned base address and size ---;
;                                                                     ;
; Input:   EDI = Pointer to IPB, used for this routine                ; 
;          EDI = Pointer to OPB, used for this routine                ;
;                                                                     ;
; Output:  EAX = Work buffer base                                     ;
;          ECX = Work buffer size                                     ;            
;          variables updated:                                         ;
;           OPB_BUFFER_BASE                                           ;
;           OPB_BUFFER_SIZE                                           ;
;---------------------------------------------------------------------;
HelperDTA:
lea eax,OPB_DATA_ARRAY
test eax,0FFFh
jz @f
and eax,0FFFFF000h
add eax,000001000h
@@:
mov OPB_BUFFER_BASE,eax     ; Setup Work buffer base
mov ecx,IPB_BLOCK_SIZE
mov OPB_BUFFER_SIZE,ecx     ; Setup Work buffer size
ret

;--- Helpers for open and create files -------------------------------;
;                                                                     ;
; Input:   ESI = Pointer to IPB, used for this routine                ;
;          EDI = Pointer to OPB, used for this routine                ;
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
lea ebx,OPB_COMPLEX_PATH
jmp L1

;--- Entry #1 = open Source file for Read ---
HelperOpenSrcRead:
; This place reserved for customize open/create

;--- Entry #2 = create Source file for Write ---
HelperOpenSrcWrite:
; This place reserved for customize open/create

;--- Common branch ---
L0:
lea ebx,IPB_SRC_PATH          ; EBX = Parm#1 = Pointer to source file path
L1:
push esi edi
mov ecx,FILE_FLAGS_BLANK      ; ECX = Parm#2 = File operation mode flags
test IPB_SRC_ATTRIBUTES,00000011b
jz @f
or ecx,FILE_FLAG_O_DIRECT
@@:
mov edx,FILE_SHARE_MODE       ; EDX = Parm#3 = File access read-write mode flags
mov eax,5                     ; EAX = Function code = SYS_OPEN
int 80h
pop edi esi
mov OPB_SRC_HANDLE,eax        ; Store source file handle
ret

;--- Entry #6 = create Destination file for Write, use complex path string ---
HelperCreateDstGroup:
lea ebx,OPB_COMPLEX_PATH
jmp L2

;--- Entry #3 = create Destination file for Write ---
HelperCreateDstWrite:
lea ebx,IPB_DST_PATH          ; EBX = Parm#1 = Pointer to destination file path
L2:
push esi edi
mov ecx,FILE_FLAGS_BLANK      ; ECX = Parm#2 = File operation mode flags
test IPB_DST_ATTRIBUTES,00000011b
jz @f
or ecx,FILE_FLAG_O_DIRECT
@@:
mov edx,FILE_SHARE_MODE       ; EDX = Parm#3 = File access read-write mode flags
mov eax,5                     ; EAX = Function code = SYS_OPEN
int 80h
pop edi esi
mov OPB_DST_HANDLE,eax        ; Store destination file handle
ret

;--- Helper for calculation time interval by timer results -----------;
;                                                                     ;
; Input:   ESI = Pointer to IPB, reserved, not used by this routine   ;
;          EDI = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           OPB_TRANSIT_T1, OPB_TRANSIT_T2                            ;
;                                                                     ;
; Output:  EDX:EAX = Timer delta value, units = 1 ns                  ;
;          EDX = high dword, EAX = low dword                          ;
;          variables:                                                 ;
;           OPB_TIMER_DELTA                                           ;
;---------------------------------------------------------------------;
HelperDeltaTime:
mov eax,OPB_TRANSIT_T2_S
sub eax,OPB_TRANSIT_T1_S       ; EAX = Delta seconds
mov ebx,1000000000             ; EBX = Nanoseconds per second
mul ebx                        ; EDX:EAX = Delta nanoseconds, addend #1 by seconds
xchg ebx,eax
mov ecx,edx                    ; ECX:EBX = Delta nanoseconds, addend #1 by seconds
mov eax,OPB_TRANSIT_T2_NS
sub eax,OPB_TRANSIT_T1_NS      ; EDX:EAX = Delta nanoseconds, addend #2 by nanoseconds
cdq
add eax,ebx
adc edx,ecx                   ; EDX:EAX = sum of addends #1, #2, total delta in nanoseconds 
mov OPB_TIMER_DELTA_L,eax
mov OPB_TIMER_DELTA_H,edx
ret
