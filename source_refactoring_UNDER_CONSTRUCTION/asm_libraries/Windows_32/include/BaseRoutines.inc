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
; Input:   ESI = Pointer to IPB, used for this routine                ;
;          EDI = Pointer to OPB, used for this routine                ;
;                                                                     ;
; Output:  EBX = Aligned pointer to data transfer area                ;
;          EBP = Size of data transfer area, bytes                    ;  
;          variables updated:                                         ;
;           OPB_BUFFER_BASE                                           ;
;           OPB_BUFFER_SIZE                                           ;  
;---------------------------------------------------------------------;
HelperDTA:
lea ebx,OPB_DATA_ARRAY
test ebx,0FFFh
jz @f
and ebx,0FFFFF000h
add ebx,000001000h
@@:
mov OPB_BUFFER_BASE,ebx        ; EBX = Work buffer base
mov ebp,IPB_BLOCK_SIZE
mov OPB_BUFFER_SIZE,ebp        ; EBP = Work buffer size
ret

;--- Helpers for open and create files -------------------------------;
;                                                                     ;
; Input:   ESI = Pointer to IPB, used for this routine                ;  
;          EDI = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           IPB_SRC_PATH, IPB_DST_PATH                                ;
;           IPB_SRC_ATTRIBUTES, IPB_DST_ATTRIBUTES                    ;
;                                                                     ;
; Output:  EAX = Opened file handle, or 0 if error                    ;
;          variables:                                                 ;
;           OPB_SRC_HANDLE                                            ;
;           OPB_DST_HANDLE                                            ;  
;---------------------------------------------------------------------;

;--- Entry #1 = open Source file for Read ---
HelperOpenSrcRead:
mov ecx,OPEN_EXISTING       ; ECX = value for Parm#5 = Creation disposition
mov edx,GENERIC_READ        ; EDX = value for Parm#2 = Desired access 
jmp L0

;--- Entry #2 = create Source file for Write ---
HelperOpenSrcWrite:
mov ecx,CREATE_ALWAYS       ; ECX = value for Parm#5 = Creation disposition
mov edx,GENERIC_WRITE       ; EDX = value for Parm#2 = Desired access

;--- Common branch ---
L0:
push 0                         ; Parm#7 = 0 = Template file handle, not used
mov eax,FILE_ATTRIBUTE_NORMAL
test IPB_SRC_ATTRIBUTES,00000001b
jz @f
or eax,FILE_FLAG_NO_BUFFERING
@@:
test IPB_SRC_ATTRIBUTES,00000010b
jz @f
or eax,FILE_FLAG_WRITE_THROUGH
@@:
push eax                       ; Parm#6 = File attribute and flags
push ecx                       ; Parm#5 = Creation disposition
xor eax,eax                    ; This for compact push zeroes
push eax                       ; Parm#4 = Security attributes = 0
push eax                       ; Parm#3 = Share mode = 0  
push edx                       ; Parm#2 = Desired access
lea eax,IPB_SRC_PATH
push eax                       ; Parm#1 = Pointer to file path  
call [CreateFileA]
mov OPB_SRC_HANDLE,eax
ret

;--- Entry #3 = create Destination file for Write ---
HelperCreateDstWrite:
xor eax,eax                    ; This for compact push zeroes
push eax                       ; Parm#7 = 0 = Template file handle, not used
mov ecx,FILE_ATTRIBUTE_NORMAL
test IPB_DST_ATTRIBUTES,00000001b
jz @f
or ecx,FILE_FLAG_NO_BUFFERING
@@:
test IPB_DST_ATTRIBUTES,00000010b
jz @f
or ecx,FILE_FLAG_WRITE_THROUGH
@@:
push ecx                       ; Parm#6 = File attribute and flags
push CREATE_ALWAYS             ; Parm#5 = Creation disposition
push eax                       ; Parm#4 = Security attributes = 0
push eax                       ; Parm#3 = Share mode = 0  
push GENERIC_WRITE             ; Parm#2 = Desired access
lea eax,IPB_DST_PATH
push eax                       ; Parm#1 = Pointer to file path  
call [CreateFileA]
mov OPB_DST_HANDLE,eax
ret

;--- Helper for calculation time interval by timer results -----------;
;                                                                     ;
; Input:   ESI = Pointer to IPB, reserved, not used by this routine   ;
;          EDI = Pointer to OPB, used for this routine                ;
;          variables:                                                 ;
;           OPB_TRANSIT_T1, OPB_TRANSIT_T2                            ;
;                                                                     ;
; Output:  variables:                                                 ;
;           OPB_TIMER_DELTA                                           ;
;---------------------------------------------------------------------;
HelperDeltaTime:
mov eax,OPB_TRANSIT_T2_L
mov ecx,OPB_TRANSIT_T2_H
sub eax,OPB_TRANSIT_T1_L
sbb ecx,OPB_TRANSIT_T1_H
mov ebx,100
mul ebx
mov OPB_TIMER_DELTA_L,eax
mov OPB_TIMER_DELTA_H,edx
mov eax,ecx
mul ebx
add OPB_TIMER_DELTA_H,eax
ret
