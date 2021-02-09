;---------- Print Number Integer.Float ------------------------;
; Float part is 1 char for P1-version                          ;
;                                                              ;
; INPUT:   EAX = Number value * 10 (for visual float part)     ;
;          EDI = Destination Pointer (flat)                    ;
; OUTPUT:  EDI = New Destination Pointer (flat)                ;
;                modified because string write                 ;
;--------------------------------------------------------------;
FloatPrintP1:
push eax ebx ecx edx
mov ecx,10
xor edx,edx
div ecx
mov bl,0              ; BL = Template size, 0 means not formatted
call DecimalPrint32   ; Print Integer part = INPUT DIV 10
FloatEntry:
mov al,'.'
stosb                 ; Print decimal point
xchg eax,edx          ; EAX = mod after division 
call DecimalPrint32   ; Print Float part = INPUT MOD 10
pop edx ecx ebx eax
ret


; This subroutine reserved.

;---------- Print Number Integer.Float ------------------------;
; Float part is 3 char for P3-version                          ;
;                                                              ;
; INPUT:   EAX = Number value * 1000 (for visual float part)   ;
;          EDI = Destination Pointer (flat)                    ;
; OUTPUT:  EDI = New Destination Pointer (flat)                ;
;                modified because string write                 ;
;--------------------------------------------------------------;
; FloatPrintP3:
; push eax ebx ecx edx
; mov ecx,1000
; xor edx,edx
; div ecx
; mov bl,0
; call DecimalPrint32   ; Print Integer part = INPUT DIV 10
; mov bl,3
; jmp FloatEntry
