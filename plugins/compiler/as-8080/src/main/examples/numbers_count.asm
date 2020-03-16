org 1000

dcx sp           ; stack initialization (0FFFFh)

lxi h,text1
call putstr      ; print text1

lxi d,input      ; address for string input
call getline     ; read from keyboard

lxi b,input
mvi d,0          ; numbers counter

char_loop:
ldax b
inx b
cpi 10           ; end of input?
jz char_end
cpi 13
jz char_end
cpi '0'
jc char_loop     ; less than '0'?
cpi '9'+1
jnc char_loop    ; more than '9'+1?
inr d            ; number
jmp char_loop
char_end:

lxi h,text2      ; print text2
call putstr

mov a,d
adi '0'          ; value and its ASCII form ...
call putchar
call newline
hlt

include 'include\getchar.inc'
include 'include\getline.inc'
include 'include\putstr.inc'
include 'include\putchar.inc'
include 'include\newline.inc'

text1: db 'Count of numbers on input ...',10,13,'Enter text: ',0
text2: db 10,13,'Count: ',0
input: ds 30
