; Finds a char with maximum ASCII code

org 1000

dcx sp        ; stack initialization (0FFFFh)

lxi h,text1 
call putstr   ; print text1

mvi d,0       ; char with maximum ASCII code

char_loop:
call getchar
cpi 10        ; end of input?
jz char_end
cpi 13    
jz char_end

cmp d 
jc char_loop  ; A < D ? 
mov d, a
jmp char_loop
char_end:

lxi h,text2   ; print text2
call putstr

mov a,d
call putchar
call newline
hlt

include 'include\getline.inc'
include 'include\putstr.inc'
include 'include\putchar.inc'
include 'include\getchar.inc'
include 'include\newline.inc'

text1: db 'Char with maximum ASCII code (getchar) ...',10,13,'Enter text: ',0
text2: db 10,13,'Char with maximum ASCII code: ',0

