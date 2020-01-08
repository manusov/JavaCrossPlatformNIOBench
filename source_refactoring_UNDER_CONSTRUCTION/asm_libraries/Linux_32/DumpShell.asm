;==============================================================================;
;                                                                              ;
;    Console application example (engineering release). Linux ia32 Edition.    ;
;                           (C)2018 IC Book Labs.                              ;
;                                                                              ;
;  This file is main module: translation object, interconnecting all modules.  ;
;                 This revision rejects Read and Write cache.                  ;
;                                                                              ;
;        Translation by Flat Assembler version 1.71.49 (Dec 06, 2015)          ;
;           Visit http://flatassembler.net/ for more information.              ;
;    For right tabulations, please edit by FASM Editor 2.0 or NoteTab Pro.     ;
;                                                                              ;
;                       Customized for file I/O test.                          ;
;                                                                              ;
;==============================================================================;

; Get information from:
; Ray Seyfarth PDF is main valid book
; http://man7.org/linux/man-pages/dir_section_2.html
; http://man7.org/linux/man-pages/man2/creat.2.html
; http://lxr.free-electrons.com/source/include/uapi/asm-generic/fcntl.h
; http://lxr.free-electrons.com/source/include/asm-x86_64/unistd.h?v=2.4.37
; https://sourcecodebrowser.com/numactl/2.0.4~rc2/syscall_8c_source.html
; http://unix.superglobalmegacorp.com/Net2/newsrc/sys/mman.h.html
; http://lxr.free-electrons.com/source/arch/arm/include/asm/unistd.h?v=2.6.32
; http://src.gnu-darwin.org/src/include/time.h.html
; http://syscalls.kernelgrok.com/
; http://www-numi.fnal.gov/offline_software/srt_public_context/WebDocs/Errors/unix_system_errors.html 
; http://lxr.free-electrons.com/source/arch/x86/include/asm/unistd_64.h?v=3.0

; Linux32 system calls notes.
; Function code = EAX
; Input parameters = EBX, ECX, EDX, ESI, EDI, EBP
; Output parameter = EAX

; BUG WITH FILE_FLAGS AND RW FLAGS
; MUST RETURN ERROR WHEN NOT EXISTED, 
; VERIFY ALL BITMAPS FOR OPEN/CREATE FILES FLAGS

include 'include\Equations.inc'

format ELF executable 3
segment readable executable
entry start

;---------- Code.Initialization --------------------------------------------;
segment readable executable
start:

;--- Test file I/O ---

lea esi,[IPB]
lea edi,[OPB]

;---
;mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
;mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
;mov eax,00000011b                    ; FILE_FLAGS
;mov IPB_SRC_ATTRIBUTES,eax
;mov IPB_ITERATIONS,500               ; 5 ; 1
;lea ecx,[ReadFileName]
;lea edx,IPB_SRC_PATH
;@@:
;mov al,[ecx]
;mov [edx],al
;inc ecx
;inc edx
;cmp al,0
;jne @b
;call MeasureReadFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
mov eax,00000011b                    ; FILE_FLAGS
mov IPB_SRC_ATTRIBUTES,eax
mov IPB_ITERATIONS,150               ; 50 ; 5 ; 1
; lea ecx,[WriteFileName]
  lea ecx,[ReadFileName]
lea edx,IPB_SRC_PATH
@@:
mov al,[ecx]
mov [edx],al
inc ecx
inc edx
cmp al,0
jne @b
call MeasureWriteFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
mov eax,00000011b                    ; FILE_FLAGS
mov IPB_SRC_ATTRIBUTES,eax
mov IPB_DST_ATTRIBUTES,eax
mov IPB_ITERATIONS,50                ; 500 ; 5 ; 1
lea ecx,[ReadFileName]
lea edx,IPB_SRC_PATH
@@:
mov al,[ecx]
mov [edx],al
inc ecx
inc edx
cmp al,0
jne @b
lea ecx,[WriteFileName]
lea edx,IPB_DST_PATH
@@:
mov al,[ecx]
mov [edx],al
inc ecx
inc edx
cmp al,0
jne @b
call MeasureCopyFile
;---
; lea ecx,[ReadFileName]
  lea ecx,[WriteFileName]
lea edx,IPB_SRC_PATH
@@:
mov al,[ecx]
mov [edx],al
inc ecx
inc edx
cmp al,0
jne @b
call MeasureDeleteFile

;--- Output OPB dump ---
cld
lea esi,[OPB]
lea edi,[TextBuffer]
mov ax,0A0Dh
stosw
mov ebx,16
@@:
lodsd
xchg edx,eax
lodsd
xchg eax,edx
call HexPrint64
mov ax,0A0Dh
stosw
dec ebx
jnz @b
mov eax,00000A0Dh          ; CR, LF and 0 = termination sequence of strings
stosw
stosd

;------------ Output parameters strings, console mode ----------------------;
ConsoleWrite:
lea esi,[TextBuffer]       ; Pointer to data for output
push esi
xor edx,edx                ; EDX = Number of chars, 0=prepare for count
@@:
lodsb
cmp al,0
je @f
inc edx                    ; EDX is input parameter for next called API
jmp @b
@@:
pop ecx
mov eax,4
mov ebx,1
int 80h

;------------ Exit program -------------------------------------------------;
ExitProgram:
mov eax,1
xor ebx,ebx
int 80h


;---------- Code.Include ---------------------------------------------------;
;---------- Copy selected text string terminated by 00h -------;
; Note last byte 00h not copied                                ;
;                                                              ;
; INPUT:   ESI = Source address                                ;
;          EDI = Destination address                           ;
;          AL  = Selector                                      ;
;          AH  = Limit  (if Selector>Limit, set Selector=0)    ; 
; OUTPUT:  ESI = Modified by copy                              ;
;          EDI = Modified by copy                              ;
;          Memory at [Input EDI] modified                      ; 
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
; INPUT:   ESI = Source address                                ;
;          EDI = Destination address                           ;
; OUTPUT:  ESI = Modified by copy                              ;
;          EDI = Modified by copy                              ;
;          Memory at [Input EDI] modified                      ; 
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
push eax
cld
@@:
movsb
cmp byte [esi],0
jne @b            ; Cycle for copy null-terminated string
inc esi
mov ax,'  '
stosw             ; Two spaces after string
pop eax
ret

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

;---------- Print 64-bit Hex Number ---------------------------;
; INPUT:  EDX:EAX = Number                                     ;
;         EDI = Destination Pointer                            ;
; OUTPUT: EDI = Modify                                         ;
;--------------------------------------------------------------;
HexPrint64:
push eax
xchg eax,edx
call HexPrint32
pop eax
; no RET, continue at next subroutine
;---------- Print 32-bit Hex Number ---------------------------;
; INPUT:  EAX = Number                                         ;
;         EDI = Destination Pointer                            ;
; OUTPUT: EDI = Modify                                         ;
;--------------------------------------------------------------;
HexPrint32:
push eax
ror eax,16
call HexPrint16
pop eax
; no RET, continue at next subroutine
;---------- Print 16-bit Hex Number ---------------------------;
; INPUT:  AX  = Number                                         ;
;         EDI = Destination Pointer                            ;
; OUTPUT: EDI = Modify                                         ;
;--------------------------------------------------------------;
HexPrint16:
push eax
xchg al,ah
call HexPrint8
pop eax
; no RET, continue at next subroutine
;---------- Print 8-bit Hex Number ----------------------------;
; INPUT:  AL  = Number                                         ;
;         EDI = Destination Pointer                            ;
; OUTPUT: EDI = Modify	                                       ;
;--------------------------------------------------------------;
HexPrint8:
push eax
ror al,4
call HexPrint4
pop eax
; no RET, continue at next subroutine
;---------- Print 4-bit Hex Number ----------------------------;
; INPUT:  AL  = Number (bits 0-3)                              ;
;         EDI = Destination Pointer                            ;
; OUTPUT: EDI = Modify                                         ;
;--------------------------------------------------------------;
HexPrint4:
cld
push eax
and al,0Fh
add al,90h
daa
adc al,40h
daa
stosb          ; Store digit to destination string [edi], modify edi+1
pop eax
ret

;---------- Library main functionality ----------------------------------------;
include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureMixedIO.inc'
include 'include\MeasureDeleteFile.inc'

;---------- Data -----------------------------------------------------------;

segment readable writeable
;--- Transit buffer for text ---
TextBuffer                 DB  1024 DUP (?)       ; Buffer for text block built
;--- File I/O debug support ---
ReadFileName   DB  'a1.bin',0     ; 'C:\TEMP\a1.bin',0
WriteFileName  DB  'a2.bin',0     ; 'C:\TEMP\a2.bin',0
align 4096
IPB       DB  4096      DUP (?)
OPB       DB  4096      DUP (?)
BUFFER    DB  1024*1024 DUP (?)  ; 16384 DUP (?)
BUFALIGN  DB  4096      DUP (?)
