;---------- Print memory block size as Integer.Float ----------;
; Float part is 1 char for P1-version                          ;
;                                                              ;
; INPUT:   EAX = Number value, units = Bytes                   ;
;          EDI = Destination Pointer (flat)                    ;
; OUTPUT:  EDI = New Destination Pointer (flat)                ;
;                modified because string write                 ;
;--------------------------------------------------------------;
SizePrint:
push eax ebx ecx edx esi
;--- Check for X Bytes, not print floating part ---
xor ebx,ebx
cmp eax,1024
jb .L3                    ; Go if value < 1024, units = bytes
;--- For X.Y KB/MB/GB/TB, required print floating part ---
mov ecx,10
mul ecx                   ; Prepare for one digit after point
.L2:
inc bh                    ; BH = Counter for units select
mov ecx,1024
div ecx                   ; value / 1024 for unit step down
xor edx,edx
cmp eax,1024 * 10
jae .L2                   ; Iterational select KB/MB/GB
mov ecx,10
xor edx,edx
div ecx                   ; EAX = Integer result , EDX = Mod
call DecimalPrint32       ; Print integer part 
mov al,'.'
stosb                     ; Print decimal point
xchg eax,edx              ; EAX = Mod after division
.L3:
call DecimalPrint32       ; Print float part, or for bytes: single integer part
mov al,' '
stosb
lea esi,[U_B]             ; ESI = Pointer to table with units names
mov al,bh                 ; AL = String selector
mov ah,4                  ; AH = Selector limit (inclusive)
call StringWriteSelected  ; Print units: KB/MB/GB
;--- Exit points ---
jmp .L1                   ; Normal entry point
.L0:                      ; Error entry point
mov al,'?'
stosb
.L1:                      ; Common point for exit subroutine
pop esi edx ecx ebx eax
ret
