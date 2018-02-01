;---------- Copy text string terminated by 00h ----------------;
; CR, LF added before string                                   ;
; Spaces added after string                                    ;
; Note last byte 00h not copied                                ;
;                                                              ;
; INPUT:   ESI = Source address                                ;
;          EDI = Destination address                           ;
; OUTPUT:  ESI = Modified by copy                              ;
;          EDI = Modified by copy                              ;
;          Memory at [Input EDI] modified                      ; 
;--------------------------------------------------------------;
ItemWrite_CRLF:
push eax
cld
mov ax,0A0Dh
stosw             ; CR, LF before string
pop eax
ItemWrite:
push eax          ; CLD repeat because can use entry point ItemWrite
cld
@@:
movsb
cmp byte [esi],0
jne @b            ; Cycle for copy null-terminated string
inc esi           ; Skip byte 00h
mov ax,'  '
stosw             ; Two spaces after string
pop eax
ret
