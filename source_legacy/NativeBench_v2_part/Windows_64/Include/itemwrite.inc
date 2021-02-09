;---------- Copy text string terminated by 00h ----------------;
; CR, LF added before string                                   ;
; Spaces added after string                                    ;
; Note last byte 00h not copied                                ;
;                                                              ;
; INPUT:   RSI = Source address                                ;
;          RDI = Destination address                           ;
; OUTPUT:  RSI = Modified by copy                              ;
;          RDI = Modified by copy                              ;
;          Memory at [Input RDI] modified                      ; 
;--------------------------------------------------------------;
ItemWrite_CRLF:
push rax
cld
mov ax,0A0Dh
stosw             ; CR, LF before string
pop rax
ItemWrite:
push rax
cld
@@:
movsb
cmp byte [rsi],0
jne @b            ; Cycle for copy null-terminated string
inc rsi
mov ax,'  '
stosw             ; Two spaces after string
pop rax
ret
