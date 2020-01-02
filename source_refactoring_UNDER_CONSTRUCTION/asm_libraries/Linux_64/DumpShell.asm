
;---------------------------------------------------------------------------;
;                          Registers Dump Sample                            ;
;                            Linux x64 version                              ;
;---------------------------------------------------------------------------;

; syscall numbers: /usr/src/linux/include/asm-x86_64/unistd.h
; parameters order:
; r9    ; 6th param
; r8    ; 5th param
; r10   ; 4th param
; rdx   ; 3rd param
; rsi   ; 2nd param
; rdi   ; 1st param
; eax   ; syscall_number
; syscall

; Note 32-bit operations (example: MOV EDX,3) also clear bits D[63-32] of
; target registers and can be used instead 64-operations (example: MOV RDX,3)
; for save space.

; RBX,RCX,RSI,RBP used as non-volatile at this program sample for subroutines


;MEASURE_ITERATIONS = 1000000000     ; Number of iterations for time measurement
;DUMP_REGION        = DoubleNumbers  ; Region for dump
;DUMP_TYPE          = 5              ; 0=None, 1=8bit, 2=16bit, 3=32bit, 
;                                    ; 4=64bit (as hex), 5=64bit (as double precision)


; BUG WITH FILE_FLAGS AND EW FLAGS
; MUST RETURN ERROR WHEN NOT EXISTED, 
; VERIFY ALL BITMAPS FOR OPEN/CREATE FILES FLAGS


DUMP_REGION = OPB
DUMP_TYPE   = 4


SYS_WRITE     = 1                   ; Linux API functions (syscall numbers)
SYS_NANOSLEEP = 35
SYS_EXIT      = 60

format ELF64 executable 3

segment readable executable
entry $

;---------- Code.Initialization --------------------------------------------;
EntryPoint:


;--- Fragment for debug file I/O ---

include 'include\Equations.inc'
FILE_FLAGS  EQU  00000042h + 00004000h

lea rsi,[IPB]
lea rdi,[OPB]
mov r14,rsi
mov r15,rdi

;---
;mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
;mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
;mov eax,FILE_FLAGS
;mov IPB_SRC_ATTRIBUTES,rax
;mov IPB_ITERATIONS,500               ; 5 ; 1
;lea rcx,[ReadFileName]
;lea rdx,IPB_SRC_PATH
;@@:
;mov al,[rcx]
;mov [rdx],al
;inc rcx
;inc rdx
;cmp al,0
;jne @b
;call MeasureReadFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
mov eax,FILE_FLAGS
mov IPB_SRC_ATTRIBUTES,rax
mov IPB_ITERATIONS,50                ; 150 ; 5 ; 1
; lea rcx,[WriteFileName]
  lea rcx,[ReadFileName]
lea rdx,IPB_SRC_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
call MeasureWriteFile
;---
mov IPB_REQUEST_SIZE, 10*1024*1024   ; 1310720   ; 16384
mov IPB_BLOCK_SIZE,   1024*1024      ; 131072    ; 4096
mov eax,FILE_FLAGS
mov IPB_SRC_ATTRIBUTES,rax
mov IPB_DST_ATTRIBUTES,rax
mov IPB_ITERATIONS,50                ; 500 ; 5 ; 1
lea rcx,[ReadFileName]
lea rdx,IPB_SRC_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
lea rcx,[WriteFileName]
lea rdx,IPB_DST_PATH
@@:
mov al,[rcx]
mov [rdx],al
inc rcx
inc rdx
cmp al,0
jne @b
call MeasureCopyFile
;---
; lea rcx,[ReadFileName]
;  lea rcx,[WriteFileName]
;lea rdx,IPB_SRC_PATH
;@@:
;mov al,[rcx]
;mov [rdx],al
;inc rcx
;inc rdx
;cmp al,0
;jne @b
;call MeasureDeleteFile
;

;--- Fragment for debug file I/O ---


;---
;;--- Get TSC value before 1 second pause ---
;rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low
;push rax rdx
;;--- Wait 1 second ---
;mov eax,SYS_NANOSLEEP     ; EAX = Linux API function (syscall number)
;mov rdi,TimespecWait      ; RDI = Pointer to loaded wait time: DQ sec, ns
;lea rsi,[rdi+16]          ; RSI = Pointer to stored remain time: DQ sec, ns
;push rsi
;syscall
;pop rsi
;test rax,rax
;jnz TimerFailed           ; Go if error returned or wait interrupted
;mov rax,[rsi+00]          ; RAX = Time remain, seconds
;or  rax,[rsi+08]          ; RAX = Disjunction with TIme remain, nanoseconds
;jnz TimerFailed           ; Go if remain time stored by function
;;--- Get TSC value after 1 second pause ---
;rdtsc                     ; EDX:EAX = TSC, EDX = High , EAX = Low , BEFORE 1 second pause
;pop rcx rbx               ; ECX:EBX = TSC, ECX = High , EBX = Low , AFTER 1 second pause
;;--- Calculate delta-TSC per 1 second = TSC frequency ---
;sub eax,ebx               ; Subtract: DeltaTSC.Low  = EndTSC.Low - StartTSC.Low
;sbb edx,ecx               ; Subtract: DeltaTSC.High = EndTSC.High - StartTSC.High - Borrow
;;--- Save TSC frequency ---
;; If this step skipped, variable TscFrequencyHz=0
;mov rbx,TscFrequencyHz
;mov [rbx+00],eax
;mov [rbx+04],edx
;;--- Read TSC at performance pattern start ---
;; If this step skipped, variable TscAtStart=0
;rdtsc
;mov [rbx+08],eax
;mov [rbx+12],edx
;TimerFailed:
;---

;---------- Code.Target fragment for debug ---------------------------------;


;mov rbp,MEASURE_ITERATIONS
;L0:
;dec rbp
;jnz L0


;---------- Code.Timings measurement ---------------------------------------;

;push rax rdx
;rdtsc
;push rbx
;mov rbx,TscAtStart
;sub eax,[rbx+00]          ; Subtract: DeltaTSC.Low  = EndTSC.Low - StartTSC.Low
;sbb edx,[rbx+04]          ; Subtract: DeltaTSC.High = EndTSC.High - StartTSC.High - Borrow
;mov [rbx+00],eax
;mov [rbx+04],edx
;pop rbx rdx rax

;---------- Code.Registers store -------------------------------------------;

call WriteLF                 ; Console output: Line Feed
jmp DEBUG_SKIP

;--- Reserve stack frame for registers ---
sub rsp,128+64+512

;--- Save CPU state for visual, x86-64 registers, 128 bytes ---
mov [rsp+000+000],rax
mov [rsp+008+000],rbx
mov [rsp+016+000],rcx
mov [rsp+024+000],rdx
lea rax,[rsp+128+64+512]
mov [rsp+032+000],rax
mov [rsp+040+000],rbp
mov [rsp+048+000],rsi
mov [rsp+056+000],rdi
mov [rsp+064+000],r8
mov [rsp+072+000],r9
mov [rsp+080+000],r10
mov [rsp+088+000],r11
mov [rsp+096+000],r12
mov [rsp+104+000],r13
mov [rsp+112+000],r14
mov [rsp+120+000],r15
;--- Save CPU state for visual, x87 FPU registers, 64 bytes ---
fstp qword [rsp+00+128]
fstp qword [rsp+08+128]
fstp qword [rsp+16+128]
fstp qword [rsp+24+128]
fstp qword [rsp+32+128]
fstp qword [rsp+40+128]
fstp qword [rsp+48+128]
fstp qword [rsp+56+128]
;--- Save CPU state for visual, XMM SSE registers, total 256 bytes ---
movupd [rsp+16*00+192],xmm0
movupd [rsp+16*01+192],xmm1
movupd [rsp+16*02+192],xmm2
movupd [rsp+16*03+192],xmm3
movupd [rsp+16*04+192],xmm4
movupd [rsp+16*05+192],xmm5
movupd [rsp+16*06+192],xmm6
movupd [rsp+16*07+192],xmm7
movupd [rsp+16*08+192],xmm8
movupd [rsp+16*09+192],xmm9
movupd [rsp+16*10+192],xmm10
movupd [rsp+16*11+192],xmm11
movupd [rsp+16*12+192],xmm12
movupd [rsp+16*13+192],xmm13
movupd [rsp+16*14+192],xmm14
movupd [rsp+16*15+192],xmm15

;---------- Code.Registers parameters dump ---------------------------------;
;--- Write first Line Feed ---
call WriteLF                 ; Console output: Line Feed
call WriteLF
;--- Write x86-64 registers ---
mov rbp,rsp                  ; RBP = Base address of registers store area
mov rbx,StringsRegisters     ; RBX = Base address of registers names string
mov ecx,16                   ; ECX = Number of strings for x86-64 registers
CycleX86: 
mov rsi,rbx                  ; RSI=Base address of source string
mov edx,3                    ; RDX=Length of source string
add rbx,rdx                  ; Registers names pointer + 3 chars
call StringWrite             ; Print register name
mov rsi,StringEqual
mov edx,3
call StringWrite             ; Print " = "
mov rdi,StringBuffer
mov rsi,rdi
mov rax,[rbp]                ; RAX = Data
add rbp,8                    ; Registers data pointer + 8 bytes
call HexPrint64              ; Built ASCII string
mov edx,16
call StringWrite             ; Print number
mov rsi,StringH
mov edx,1
call StringWriteLF           ; Print "h"
loop CycleX86                ; Cycle for print x86 registers
call WriteLF
;--- Write x87 registers ---
mov cl,8                     ; Here RCX=0, can load CL only for compact code
CycleX87:
mov rsi,rbx                  ; RSI=Base address of source string
mov edx,3                    ; RDX=Length of source string
add rbx,rdx                  ; Registers names pointer + 3 chars
call StringWrite             ; Print register name
mov rsi,StringEqual
mov edx,3
call StringWrite             ; Print " = "
mov rdi,StringBuffer
mov rsi,rdi
mov rax,[rbp]                ; RAX = Data
add rbp,8                    ; Registers data pointer + 8 bytes
call FloatingPrint64
mov rdx,rdi
sub rdx,rsi                  ; RSI=Base, RDX=Size
call StringWriteLF
loop CycleX87
call WriteLF
;--- Write SSE registers ---
mov cl,16                    ; Here RCX=0, can load CL only for compact code
CycleSSE:
mov rsi,rbx                  ; RSI=Base address of source string
mov edx,5                    ; RDX=Length of source string
add rbx,rdx                  ; Registers names pointer + 3 chars
call StringWrite             ; Print register name
mov rsi,StringEqual
mov edx,3
call StringWrite             ; Print " = "
;--- Start internal cycle for 2 numbers at XMM register ---
mov ch,2                     ; Parallel usage CH and CL, ext.cycle<255
CycleXMM:
mov rdi,StringBuffer
mov rsi,rdi
mov rax,[rbp]
add rbp,8
call FloatingPrint64         ; Built ASCII string
push rcx
mov edx,10
mov ecx,edx
mov rax,rdi
sub rax,rsi
cmp rax,rdx
ja BadLength
sub rcx,rax
mov al,' '
rep stosb                    ; Blank for align X-position
BadLength:
pop rcx
call StringWrite             ; Print number
dec ch
jz ExitXMM
mov rsi,StringSeparator
mov edx,3
call StringWrite
jmp CycleXMM
ExitXMM:
;--- End internal cycle for 4 numbers at XMM register ---
call WriteLF
loop CycleSSE
;--- Write last Line Feed ---
call WriteLF
;--- Release stack frame (include 256 bytes reserved) ---
add rsp,128+64+512

DEBUG_SKIP:

;---------- Code.Memory dump -----------------------------------------------;

IF DUMP_TYPE > 0

;--- Prepare for dump write ---
mov rbx,DUMP_REGION          ; RBX = Base address
xor ebp,ebp                  ; EBP = 0, Counter for dump address write
;--- Dump write: address and data ---
MemoryDumpStrings:           ; Start cycle for write strings of dump
mov rdi,StringBuffer
mov rsi,rdi
mov eax,ebp
call HexPrint16              ; Print address
mov edx,4
call StringWrite
mov rsi,StringInterval
mov edx,3
call StringWrite

IF DUMP_TYPE = 1             ; Dump for 8-bit, hex
mov ecx,16
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsb
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint8
mov al,' '
stosb
mov edx,3
call StringWrite
loop MemoryDump              ; End cycle for write bytes at dump string
END IF

IF DUMP_TYPE = 2             ; Dump for 16-bit, hex
mov ecx,8
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsw
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint16
mov al,' '
stosb
mov edx,5
call StringWrite
loop MemoryDump              ; End cycle for write bytes at dump string
END IF

IF DUMP_TYPE = 3             ; Dump for 32-bit, hex
mov ecx,4
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsd
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint32
mov al,' '
stosb
mov edx,9
call StringWrite
loop MemoryDump              ; End cycle for write bytes at dump string
END IF

IF DUMP_TYPE = 4             ; Dump for 64-bit, hex
mov ecx,2
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rsi,rbx
lodsq
mov rbx,rsi
mov rdi,StringBuffer
mov rsi,rdi
call HexPrint64
mov al,' '
stosb
mov edx,17
call StringWrite
loop MemoryDump              ; End cycle for write bytes at dump string
END IF

IF DUMP_TYPE = 5             ; Dump for double precision
mov ecx,4
MemoryDump:                  ; Start cycle for write bytes at dump string
mov rdi,StringBuffer
mov rsi,rdi
push rcx rdi
mov cl,16                    ; At this point RCX.[63-08]=0
mov al,' '
rep stosb
pop rdi rcx
mov rax,[rbx]
add rbx,8
call FloatingPrint64
mov edx,16
call StringWrite
loop MemoryDump              ; End cycle for write bytes at dump string
END IF

call WriteLF

IF DUMP_TYPE < 5
add ebp,0010h
cmp ebp,0100h
ELSE
add ebp,0020h
cmp ebp,0100h
END IF

jb MemoryDumpStrings         ; End cycle for write strings of dump

call WriteLF

;--- End of memory dump write ---

END IF

;---------- Code.Timings parameters dump -----------------------------------;
;mov rbx,TscFrequencyHz
;;--- Write TSC frequency ---
;mov rax,[rbx+00]             ; RAX = Delta TSC per 1 second, frequency, Hz
;xor edx,edx
;mov ecx,1000000
;div rcx
;mov rsi,StringDTSC
;mov edx,18
;call ParameterWrite
;call WriteLF
;;--- Write TSC clocks per measurement iteration ---
;mov rax,[rbx+08]             ; RAX = Delta TSC per target operation
;test rax,rax                 ; Go if clocks per iteration = 0
;jz BadTscp
;mov ecx,MEASURE_ITERATIONS/100
;xor edx,edx
;div rcx
;mov rcx,100
;xor edx,edx
;div rcx
;mov rcx,0FFFFFFFFh
;cmp rax,rcx
;ja BadTscp                   ; Go if overflow 32-bit value
;push rdx rax                 ; RDX=Floating, RAX=Integer
;mov rsi,StringDTSCP
;mov edx,18
;call StringWrite
;mov rdi,StringBuffer
;mov rbp,rdi
;pop rax
;xor esi,esi
;call DecimalPrint32          ; Print INTEGER part, X for X.Y
;mov al,'.'
;stosb
;pop rax
;mov si,2
;call DecimalPrint32          ; Print FLOATING part, Y for X.Y
;mov rsi,rbp
;mov rdx,rdi
;sub rdx,rbp
;call StringWriteLF
;jmp AfterTscp
;BadTscp:
;xor eax,eax
;call ParameterWrite          ; This branch used if measurement error
;AfterTscp:
;;--- Write Delta T (ms) ---
;mov rax,1000000000000000     ; 10^15 for calculate femtoseconds
;mov rcx,[rbx+00]             ; RCX = Delta TSC per 1 second, frequency, Hz
;jrcxz BadTime
;xor edx,edx
;div rcx                      ; Tclk[fs] = 10^15 / Fclk[hz]
;mul qword [rbx+08]           ; dT[fs] = Tclk[fs] * dTSC[clks]
;mov rcx,1000000000000        ; 10^12 for convert from [fs] to [ms]
;xor edx,edx
;div rcx                      ; dT[ms] = dT[fs] / 10^12
;BadTime:
;mov rsi,StringDT
;mov edx,18
;call ParameterWrite
;mov rsi,StringComment
;mov edx,38
;call StringWriteLF
;call WriteLF
;---

;---------- Exit to Operating System ---------------------------------------;

xor edi,edi                  ; exit code 0
mov eax,SYS_EXIT             ; EAX = Linux API function (syscall number)
syscall

;---------- Code.Subroutines -----------------------------------------------;

;---------------------------------------------------;
; Write decimal parameter with validity checks      ;
; Invalid if zero and if 32-bit overflow            ;
; INPUT:   RSI = Pointer to parameter name string   ;
;          RAX = Parameter value                    ;
; OUTPUT:  None                                     ;
;---------------------------------------------------;
ParameterWrite:
push rax
call StringWrite
pop rax
test eax,eax
jz BadParameter
mov rdx,0FFFFFFFFh
cmp rax,rdx
ja BadParameter
mov rdi,StringBuffer
push rdi
xor esi,esi
call DecimalPrint32
pop rsi
mov rdx,rdi
sub rdx,rsi
jmp ValidParameter
BadParameter:
mov rsi,StringUnknown
mov edx,1
ValidParameter:
; No RET, continue in the next subroutine
jmp StringWrite

;---------------------------------------------------;
; Text string console output with LF = Line Feed    ;
; INPUT:  RSI = String base address                 ;
;         RDX = String size                         ;
; OUTPUT: None                                      ;
;---------------------------------------------------;
StringWriteLF:
call StringWrite
; No RET, continue in the next subroutine
;---------------------------------------------------;
; LF = Line Feed console output                     ;
; INPUT:  None                                      ;
; OUTPUT: None                                      ;
;---------------------------------------------------;
WriteLF:
mov rsi,StringLF
mov edx,1
; No RET, continue in the next subroutine
;---------------------------------------------------;
; Text string console output                        ;
; INPUT:  RSI=String base address                   ;
;         RDX=String size                           ;
; OUTPUT: None                                      ;
;---------------------------------------------------;
StringWrite:
mov edi,1                  ; STDOUT
mov eax,SYS_WRITE          ; EAX = Linux API function (syscall number)
push rbx rcx rsi rbp       ; RBX, RCX, RSI, RBP non-volatile for this program
syscall
pop rbp rsi rcx rbx
ret

;---------- Print 32-bit Decimal Number ---------------;
; INPUT:   EAX = Number value                          ;
;          SI = Template size, chars. 0=No template    ;
;          RDI = Destination Pointer (flat)            ;
; OUTPUT:  RDI = New Destination Pointer (flat)        ;
;                modified because string write         ;
;------------------------------------------------------;
DecimalPrint32:
cld
push rbx rcx                 ; Note RAX, RDX is volatile
mov ebx,esi
mov bh,80h-10
add bh,bl
mov ecx,1000000000
DP32_00:
xor edx,edx
div ecx                      ; Produce current digit
and al,0Fh
test bh,bh
js DP32_02
cmp ecx,1
je DP32_02
cmp al,0                     ; Not actual left zero ?
jz DP32_03
DP32_02:
mov bh,80h                   ; Flag = 1
or al,30h
stosb                        ; Store char
DP32_03:
push rdx
xor edx,edx
mov eax,ecx
mov ecx,10
div ecx
mov ecx,eax
pop rax
inc bh
test ecx,ecx
jnz DP32_00
pop rcx rbx
DP32_Ret:
ret

;---------- Print 64-bit Hex Number ----------------;
; INPUT:  RAX = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint64:
push rax
ror rax,32
call HexPrint32
pop rax
; no RET, continue at next subroutine
;---------- Print 32-bit Hex Number ----------------;
; INPUT:  EAX = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint32:
push rax
ror eax,16
call HexPrint16
pop rax
; no RET, continue at next subroutine
;---------- Print 16-bit Hex Number ----------------;
; INPUT:  AX  = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint16:
push rax
xchg al,ah
call HexPrint8
pop rax
; no RET, continue at next subroutine
;---------- Print 8-bit Hex Number -----------------;
; INPUT:  AL  = Number                              ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint8:
push rax
ror al,4
call HexPrint4
pop rax
; no RET, continue at next subroutine
;---------- Print 4-bit Hex Number -----------------;
; INPUT:  AL  = Number (bits 0-3)                   ;
;         RDI = Destination Pointer                 ;
; OUTPUT: RDI = Modify                              ;
;---------------------------------------------------;
HexPrint4:
cld
push rax
and al,0Fh
cmp al,9
ja HP4_AF
add al,'0'
jmp HP4_Store
HP4_AF:
add al,'A'-10
HP4_Store:
stosb
pop rax
ret

;--- Print 64-bit Floating point double precision number ---------;
; Version 2 for correct visual big numbers,                       ;
; required next modification for small numbers                    ;
; Notes.                                                          ;
; FISTTP instruction manipulate with x87,                         ;
; but must be validated by CPUID.SSE3.                            ;
; FCOMI instruction must be validated by                          ;
; CPUID.CMOV and CPUID.x87.                                       ;
;                                                                 ;
; INPUT:   RAX = Number value (Floation Point Double Precision)   ;
;          RDI = Destination Pointer (flat)                       ;
; OUTPUT:  RDI = New Destination Pointer (flat)                   ;
;                modified because string write                    ;
;-----------------------------------------------------------------;
FloatingPrint64:
cld
;--- Save parameters, built stack frame ---
push rbx rsi rax rax         ; Save input and built scratch pad (rax)
pushq 999                    ; [rsp+24] = Integer, as big number new limit
pushq 10                     ; [rsp+16] = Integer, as float divisor
pushq 9999                   ; [rsp+08] = Integer, as big number limit
pushq 10000000               ; [rsp+00] = Integer, as float multiplier
;--- Check and write sign "+" or "-" ---
; No STOSB, can't corrupt RAX
btr rax,63                   ; SF flag = bit RAX.63 (sign) , clear RAX.63=0
mov byte [rdi],' '           ; Write " " if sign = plus
jnc FP64_00                  ; Go if CF flag = 0
mov byte [rdi],'-'           ; Write "-" if sign = minus
FP64_00:
inc rdi                      ; Output string pointer +1
;--- Detect special cases ---
test rax,rax
jz FP64_Zero                 ; Go if special case = 0.0
mov rbx,07FF8000000000000h
cmp rax,rbx
je FP64_QNAN                 ; Go if special case = QNAN
mov rbx,07FF0000000000000h
cmp rax,rbx
je FP64_INF                  ; Go if special case = INF
ja FP64_NAN                  ; Go if special case = NAN
;--- Load input number(x), get abs(x) ---
fld qword [rsp+32]           ; Load input number to ST0
fabs                         ; Make positive number (absolute)
;--- Check for big number ---
fild qword [rsp+08]          ; Load big number limit
fcomip st1
jc FP64_Big
;--- Number calculations ---
fld st0                      ; Duplicate input number
fisttp qword [rsp+32]        ; Store integer part
fild qword [rsp+32]          ; Load integer part
fxch st1                     ; Exchange, st0=full, st1=integer
fsub st0,st1                 ; Subtract integer, st0=float
fimul dword [rsp+00]         ; Multiply, st0=float*10000000
fisttp qword [rsp+40]        ; Store float part as integer 0-10000000
fstp st                      ; Free ST0
;--- Write number integer.float ---
mov eax,[rsp+32]             ; Load EAX=Integer , high 32 bits = 0
xor esi,esi                  ; SI=0, no chars template
call DecimalPrint32          ; Built ASCII string for integer
mov al,'.'
stosb                        ; Write decimal point
mov eax,[rsp+40]             ; Load EAX=Float
mov si,7                     ; SI=7, template=7 chars
call DecimalPrint32          ; Built ASCII string for float
;--- Remove stack frame and return ---
FP64_01:
add rsp,48                   ; Remove scratch pads
pop rsi rbx                  ; Restore registers
ret                          ; Return to caller
;--- Handling big numbers ---
FP64_Big:
mov dword [rsp+00],2
FP64_03:
inc dword [rsp+00]
fidiv word [rsp+16]
fild qword [rsp+24]          ; Load show big number limit
fcomip st1
jc FP64_03
fistp dword [rsp+32]         ; Store integer part
mov eax,[rsp+32]             ; Load integer part
xor edx,edx                  ; Dividend high 32 bits = 0
mov ebx,100
div ebx
xor esi,esi                  ; SI=0, no chars template
push rdx
call DecimalPrint32          ; Built ASCII string for integer
mov al,'.'
stosb                        ; Write decimal point
pop rax
mov si,2
call DecimalPrint32          ; Built ASCII string for float
mov al,'E'
stosb                        ; Write "E"
mov eax,[rsp+00]             ; Get decimal exponent
call DecimalPrint32          ; Built ASCII string for exponent
jmp FP64_01
;--- Handling special cases ---
FP64_Zero:                   ; Zero
mov eax,'0.0 '
jmp FP64_02                  
FP64_INF:                    ; "INF" = Infinity
mov eax,'INF '
jmp FP64_02
FP64_NAN:
mov eax,'NAN '               ; "NAN" = Not a number
jmp FP64_02
FP64_QNAN:
mov eax,'QNAN'               ; "QNAN" = Quiet not a number
jmp FP64_02
FP64_02:
stosd
jmp FP64_01


;---------- Library main functionality ----------------------------------------;

include 'include\GetRandomData.inc'
include 'include\MeasureReadFile.inc'
include 'include\MeasureWriteFile.inc'
include 'include\MeasureCopyFile.inc'
include 'include\MeasureMixedIO.inc'
include 'include\MeasureDeleteFile.inc'

;---------- Data -----------------------------------------------------------;

segment readable writeable

TimespecWait:    ; Seconds=1 and Nanoseconds=0 values, time for wait
DQ  1, 0         
TimespecRemain:  ; Seconds and Nanoseconds, remain stored if wait interrupted
DQ  0, 0
TscFrequencyHz:  ; TSC frequency, Hz, remain 0 if timer or measurement failed
DQ  0
TscAtStart:      ; TSC value at start target pattern
DQ  0            

StringsRegisters:
DB  'RAXRBXRCXRDXRSPRBPRSIRDI'
DB  'R8 R9 R10R11R12R13R14R15'
DB  'ST0ST1ST2ST3ST4ST5ST6ST7'
DB  'XMM0 XMM1 XMM2 XMM3 XMM4 XMM5 XMM6 XMM7 '
DB  'XMM8 XMM9 XMM10XMM11XMM12XMM13XMM14XMM15'
StringInterval:
DB  '   '
StringSeparator:
DB  ' | '
StringDTSC:
DB  'dTSC/Sec(MHz)   = '
StringDTSCP:
DB  'dTSC/Pass(Clks) = '
StringDT:
DB  'dT(ms)          = '
StringComment:
DB  ' (plus 1 second for measure TSC clock)'
StringEqual:
DB  ' = '
StringH:
DB  'h'
StringUnknown:
DB  '?'
StringLF:
DB  0Ah

DoubleNumbers:
DQ  1.0 , 34.443 , 3.7E21 , -0.0387
DQ  27 DUP (1.0)
DQ  -1.0

StringBuffer:
DB  32 DUP (?)
DataBuffer:
DB  1024 DUP (?)

;--- File I/O debug support ---

ReadFileName   DB  'a1.bin',0     ; 'C:\TEMP\a1.bin',0
WriteFileName  DB  'a2.bin',0     ; 'C:\TEMP\a2.bin',0
align 4096
IPB       DB  4096      DUP (?)
OPB       DB  4096      DUP (?)
BUFFER    DB  1024*1024 DUP (?)  ; 16384 DUP (?)
BUFALIGN  DB  4096      DUP (?)

;---------- End ------------------------------------------------------------;





