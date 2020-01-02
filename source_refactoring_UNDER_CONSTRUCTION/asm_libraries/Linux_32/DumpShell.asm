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


; BUG WITH FILE_FLAGS AND EW FLAGS
; MUST RETURN ERROR WHEN NOT EXISTED, 
; VERIFY ALL BITMAPS FOR OPEN/CREATE FILES FLAGS


format ELF executable 3
segment readable executable
entry start

;---------- Code.Initialization --------------------------------------------;
segment readable executable
start:

;--- Message "Starting..." -------------------------------------------------;

mov eax,4
mov ebx,1
lea ecx,[MsgStarting]
mov edx,SizeStarting
int 80h

;------------ Check CPU features and measure CPU clock ----------------------;
;
;mov [ErrorStep],Step01_MeasureClock  ; Pointer to phase name for error report
;mov [ErrorSubStep],1                 ; Status code for error report
;call CheckCpuId
;jc ErrorProgram                  ; Go if CPUID not supported or locked
;inc [ErrorSubStep]
;cmp eax,1
;jc ErrorProgram                  ; Go if CPUID function 1 not sup. or locked 
;inc [ErrorSubStep]
;mov eax,1
;cpuid
;test dl,10h
;jz ErrorProgram                  ; Go if TSC not supported
;inc [ErrorSubStep]
;call MeasureCpuClk
;jc ErrorProgram                  ; Go if TSC clock measurement error
;inc [ErrorSubStep]
;push eax
;or eax,edx
;pop eax
;jz ErrorProgram                  ; Go if TSC error: frequency=0
;push ebx
;or ebx,ecx
;pop ebx
;jz ErrorProgram                  ; Go if TSC error: period=0 
;mov dword [TscFrequency+0],eax   ; Store TSC frequency, Hz
;mov dword [TscFrequency+4],edx
;mov dword [TscPeriod+0],ebx      ; Store TSC period, fs
;mov dword [TscPeriod+4],ecx
;
;----------- Check OS timer, get OS timer resolution -----------------------;
;
;mov [ErrorStep],Step02_CheckOsTimer
;xor ebx,ebx                    ; Parm#1 = Timer select, 0 means CLOCK_REALTIME
;lea ecx,[TimespecResolution]   ; Parm#2 = Pointer to output DQ sec, ns
;mov eax,266                    ; Function code = SYS_GETRES
;push ecx
;int 80h
;pop ecx
;test eax,eax
;mov [ErrorSubStep],eax
;jnz ErrorProgram               ; Go if error returned
;mov [ErrorSubStep],1
;cmp dword [ecx+00],0
;jne ErrorProgram               ; Go if seconds > 0 , time units too big
;inc [ErrorSubStep]
;mov eax,[ecx+04]
;test eax,eax
;jz ErrorProgram                ; Go if nanoseconds = 0 , time units zero
;cmp eax,1000000
;ja ErrorProgram                ; Go if nanoseconds > 1000000 , time units too big
;

;--- User-defined code, file I/O for this example ---

include 'include\Equations.inc'
FILE_FLAGS  EQU  00000042h + 00004000h

lea esi,[IPB]
lea edi,[OPB]

;---
;mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
;mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
;mov eax,FILE_FLAGS
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
;mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
;mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
;mov eax,FILE_FLAGS
;mov IPB_SRC_ATTRIBUTES,eax
;mov IPB_ITERATIONS,150               ; 50 ; 5 ; 1
;; lea ecx,[WriteFileName]
;  lea ecx,[ReadFileName]
;lea edx,IPB_SRC_PATH
;@@:
;mov al,[ecx]
;mov [edx],al
;inc ecx
;inc edx
;cmp al,0
;jne @b
;call MeasureWriteFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
mov eax,FILE_FLAGS
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


;--- Fragment for debug file I/O ---







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

;lea esi,[TextBuffer]
;call StringWrite
;
;--- Visual detected parameters ---
;
;lea esi,[Parm01_TscFrequency]
;lea edi,[TextBuffer]
;call ItemWrite_CRLF        ; CPU TSC frequency 
;mov eax,dword [TscFrequency+0]
;mov edx,dword [TscFrequency+4]
;mov ecx,100000             ; Divisor=100000 for [Hz] to [MHz/10]
;cmp edx,ecx
;jae .BadF
;div ecx                    ; RAX = Frequency, [MHz/10]
;call FloatPrintP1          ; Print Frequency as X.Y number
;mov eax,' MHz'
;stosd
;jmp .EndF
;.BadF:
;mov al,'?'
;stosb
;.EndF:
;
;call ItemWrite_CRLF          ; CPU TSC period
;mov eax,dword [TscPeriod+0]
;mov edx,dword [TscPeriod+4]
;mov ecx,100                  ; Divisor=100 for [fs] to [ps/10]
;cmp edx,ecx
;jae .BadT
;div ecx                      ; EAX = Period, [ps/10]
;call FloatPrintP1            ; Print Frequency as X.Y number
;mov eax,' ps '
;stosd
;jmp .EndT
;.BadT:
;mov al,'?'
;stosb
;.EndT:
;
;lea esi,[MsgDone]
;call StringWrite
;
;mov eax,00000A0Dh          ; CR, LF and 0 = termination sequence of strings
;stosw
;stosd


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

;--- (12) --- Exit point for error exit ------------------------------------;
; Reserved for add functionality, restore system context:
; release memory, close handles, restore affinity
; Terminology notes: MB = Message Box
; [ErrorStep] = Pointer to action phase description string
; [ErrorSubStep] = Error code, operation-specific, get from OS API or sub-step ID

ErrorProgram:

;--- Write operation phase description string ---
cld
lea esi,[ErrorPhaseString]
lea edi,[TextBuffer]
@@:
movsb
cmp byte [esi],0
jne @b
mov esi,[ErrorStep]
@@:
movsb
cmp byte [esi],0
jne @b
mov ax,0A0Dh
stosw
;--- Write status code string ---
lea esi,[ErrorStatusString]
@@:
movsb
cmp byte [esi],0
jne @b
mov eax,[ErrorSubStep]
call HexPrint32
mov eax,0D0A0D00h + 'h'
stosd
mov ax,000Ah
stosw
jmp ConsoleWrite

;---------- Code.Include ---------------------------------------------------;
;
;include 'Include\stringwrite.inc'    ; Write string, include selector-based
;include 'Include\itemwrite.inc'      ; Write string with CR,LF and spaces added 
;include 'Include\decprint.inc'       ; Built string for decimal numbers 
;include 'Include\hexprint.inc'       ; Built string for hexadecimal numbers
;include 'Include\floatprint.inc'     ; Built string for float numbers
;include 'Include\sizeprint.inc'      ; Built string for memory block size
;include 'Include\checkcpuid.inc'     ; Verify CPUID support, max. sup. function
;include 'Include\measurecpuclk.inc'  ; Measure CPU TSC clock frequency
;

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
cmp eax,256*1024*1024
ja .L0                    ; Limited functionality, maximum 256MB
;--- Check for X Bytes, not print floating part ---
xor ebx,ebx
cmp eax,1024
jb .L3                    ; Go if value < 1024, units = bytes
;--- For X.Y KB/MB/GB/TB, required print floating part ---
imul eax,eax,10           ; Prepare for one digit after point
.L2:
inc bh                    ; BH = Counter for units select
xor edx,edx
mov ecx,1024
div ecx                   ; value / 1024 for unit step down
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

;--- Detect CPUID support, execute CPUID function #0 --------------------;
; Note CPUID can be supported by CPU but locked by Virtual Monitor.      ;
; Note check bit EFLAGS.21 toggleable, it is CPUID support indicator.    ;
; Note probably wrong result if trace this subroutine code.              ;
;                                                                        ;
; INPUT:   None                                                          ;
;                                                                        ;
; OUTPUT:  CF flag = Status: 0(NC)=Support OK, 1(C)=Not supported        ;
;          EAX = Largest standard CPUID function supported,              ;
;                valid only if CF=0.                                     ;
;------------------------------------------------------------------------;
CheckCpuId:
push ebx ecx edx
;--- Check for ID bit writeable for "1" ---
mov ebx,21                ; EFLAGS bit number = 21
pushf                     ; In the 64-bit mode, push RFLAGS
pop eax
bts eax,ebx               ; Set EAX.21=1
push eax
popf                      ; Load EFLAGS with EFLAGS.21=1
pushf                     ; Store EFLAGS
pop eax                   ; Load EFLAGS to EAX
btr eax,ebx               ; Check EAX.21=1, Set EAX.21=0
jnc NoCpuId               ; Go error branch if cannot set EFLAGS.21=1
;--- Check for ID bit writeable for "0" ---
push eax
popf                      ; Load EFLAGS with EFLAGS.21=0
pushf                     ; Store EFLAGS
pop eax                   ; Load EFLAGS to EAX
btr eax,ebx               ; Check EAX.21=0
jc NoCpuId                ; Go if cannot set EFLAGS.21=0
;--- Execute CPUID function 0, returned result in EAX ---
xor eax,eax               ; EAX = Function number for CPUID instruction
cpuid                     ; Execute CPUID function 0
;--- Exit points ---
ExitCpuId:
pop edx ecx ebx
ret                       ; Return, at this point CF=0(NC) after XOR EAX,EAX
NoCpuId:
stc                       ; CF=1(C) means error
jmp ExitCpuId 

;--- Measure CPU TSC (Time Stamp Counter) clock frequency ---------------;
; Return measurement result as 2 values:                                 ;
; F=Frequency=[Hz], T=Period=[fs]. 1 femtosecond = 10^-15 seconds.       ;
; Primary measured parameter is FREQUENCY, parameter PERIOD is result    ;
; of integer division, means approximation.                              ;
; If precision required, caller must calculate 1/FREQUENCY by x87 FPU,   ;
; but don't use PERIOD.                                                  ; 
;                                                                        ;
; INPUT:   None                                                          ;
;                                                                        ;
; OUTPUT:  CF flag = Status: 0(NC)=Measured OK, 1(C)=Measurement error	 ;
;          Output RAX,RDX valid only if CF=0(NC)                         ;
;          EDX:EAX = TSC Frequency, Hz, F = Delta TSC per 1 second       ;
;          ECX:EBX = TSC Period, Fs, T=1/F (validity limited for 32-bit) ;
;------------------------------------------------------------------------;
MeasureCpuClk:
push edi esi ebp
;--- Prepare parameters, early to minimize dTSC ---
lea ebx,[TimespecWait]    ; EBX = Pointer to loaded wait time: DQ sec, ns
lea ecx,[ebx+16]          ; ECX = Pointer to stored remain time: DQ sec, ns
;--- Get TSC value before 1 second pause ---
rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low
push eax edx
;--- Wait 1 second ---
mov eax,162               ; EAX = Linux API function (syscall number) = SYS_NANOSLEEP
push ecx
int 80h
pop ecx
xchg ebx,eax
;--- Get TSC value after 1 second pause ---
rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low , BEFORE 1 second pause
pop edi esi               ; EDI:ESI = TSC, ECX = High , EBX = Low , AFTER 1 second pause
;--- Check results ---
test ebx,ebx
jnz TimerFailed           ; Go if error returned or wait interrupted
mov ebx,[ecx+00]          ; Time remain, seconds
or ebx,[ecx+04]
or ebx,[ecx+08]           ; Disjunction with Time remain, nanoseconds
or ebx,[ecx+12]
jnz TimerFailed           ; Go if remain time stored by function
;--- Calculate delta-TSC per 1 second = TSC frequency ---
sub eax,esi               ; Subtract: DeltaTSC.Low  = EndTSC.Low - StartTSC.Low
sbb edx,edi               ; Subtract: DeltaTSC.High = EndTSC.High - StartTSC.High - Borrow
test edx,edx
jnz TimerFailed           ; This debug 32-bit code not supports > 4GHz
;--- Calculate period T=1/F ---
; Femtoseconds per one second = 1000000000000000
; Low 32-bit =  A4C68000h
; High 32-bit = 00038D7Eh
mov ebx,0A4C68000h
mov ecx,000038D7Eh
cmp ecx,eax
jae TimerFailed           ; Go error if divide overflow
push eax edx
xchg eax,ebx
mov edx,ecx
div ebx                   ; Femtoseconds per second / Hz = Period in femtoseconds
xchg ebx,eax
xor ecx,ecx
pop edx eax
;--- Exit points ---
clc
TimerDone:
pop ebp esi edi
ret
TimerFailed:
stc
jmp TimerDone

;---------- Library main functionality ----------------------------------------;

include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureMixedIO.inc'
include 'include\MeasureDeleteFile.inc'

;---------- Data -----------------------------------------------------------;

segment readable writeable

TimespecWait:        ; Seconds=1 and Nanoseconds=0 values, time for wait
DQ  1, 0         

TimespecRemain:      ; Seconds and Nanoseconds, remain stored if wait interrupted
DQ  0, 0

TimespecResolution:  ; Seconds and Nanoseconds, timer resolution
DQ  0, 0 

;--- Names for operations steps ---
Step01_MeasureClock        DB  'Measure CPU clock frequency',0
Step02_CheckOsTimer        DB  'Check OS timer',0

;--- Names for parameters ---
Parm01_TscFrequency        DB  'CPU TSC frequency =',0
Parm02_TscPeriod           DB  'CPU TSC period    =',0


;--- Common text constants ---
SizeStarting               = MsgStartingEnd - MsgStarting
MsgStarting                DB  0Dh,0Ah, 'ASM Console Template v0.01 for Linux ia32'
                           DB  0Dh,0Ah, '(C)2018 IC Book Labs.'
                           ; DB  0Dh,0Ah, 'Measure TSC clock...',0
                           DB 0Dh, 0Ah, 0
MsgStartingEnd:
ErrorPhaseString           DB  0Dh,0Ah,'Failed',0Dh,0Ah
                           DB  'Error phase: ',0
ErrorStatusString          DB  'Error status: ',0

MsgDone                    DB  0Dh,0Ah,'Done.',0

;--- Memory size and speed units ---
U_B                        DB  'Bytes',0
U_KB                       DB  'KB',0
U_MB                       DB  'MB',0
U_GB                       DB  'GB',0
U_TB                       DB  'TB',0
LinuxTimePeriod            DQ  1000000   ; femtoseconds per 1 ns (Linux Time unit)

;--- Constants for calculations ---
Const1E10                  DQ  1000000000 * 10    ; Constant for fs to 10*us

;--- Common variables/constants ---
TscFrequency               DQ  ?       ; CPU TSC Frequency, Hz
TscPeriod                  DQ  ?       ; CPU TSC Period, picoseconds
ErrorStep                  DD  ?       ; Step number for error reporting
ErrorSubStep               DD  ?       ; Sub-step number for error reporting

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

;---------- End ---------------------------------------------------------------;


