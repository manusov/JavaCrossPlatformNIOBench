;---------- Print 32-bit Decimal Number -----------------------;
;                                                              ;
; INPUT:   EAX = Number value                                  ;
;          BL  = Template size, chars. 0=No template           ;
;          EDI = Destination Pointer (flat)                    ;
; OUTPUT:  EDI = New Destination Pointer (flat)                ;
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

