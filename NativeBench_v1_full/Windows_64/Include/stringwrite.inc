;---------- Copy selected text string terminated by 00h -------;
; Note last byte 00h not copied                                ;
;                                                              ;
; INPUT:   RSI = Source address                                ;
;          RDI = Destination address                           ;
;          AL  = Selector                                      ;
;          AH  = Limit  (if Selector>Limit, set Selector=0)    ; 
; OUTPUT:  RSI = Modified by copy                              ;
;          RDI = Modified by copy                              ;
;          Memory at [Input RDI] modified                      ; 
;--------------------------------------------------------------;
StringWriteSelected:
test al,al
jz StringWrite   ; Go if required first string, skip find operation
cmp al,ah
ja StringWrite   ; Go if wrong selector above limit
mov ah,al
;--- Skip AH strings ---
cld
@@:
lodsb       ; AL = current char, RSI+1
cmp al,0
jne @b      ; Repeat cycle if non-zero, skip current string in the strings pool
dec ah
jnz @b      ; Repeat cycle for skip required number of strings
;--- No RET continue in the next subroutine ---

;---------- Copy text string terminated by 00h ----------------;
; Note last byte 00h not copied                                ;
;                                                              ;
; INPUT:   RSI = Source address                                ;
;          RDI = Destination address                           ;
; OUTPUT:  RSI = Modified by copy                              ;
;          RDI = Modified by copy                              ;
;          Memory at [Input RDI] modified                      ; 
;--------------------------------------------------------------;
StringWrite:
cld
@@:
lodsb
cmp al,0
je @f
stosb
jmp @b
@@:
ret

