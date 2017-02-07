; Print reversed text

org 1000

dcx sp       ; stack initialization (0FFFFh)

lxi h,text1 
call putstr  ; print text1
lxi d,input  ; address for string input
call getline ; read from keyboard

lxi b,input

mvi d,0      ; chars counter

char_loop:
ldax b 
inx b        ; bc = bc+1
cpi 10       ; end of input?
jz char_end
cpi 13
jz char_end

inr d        ; d =d+1
jmp char_loop
char_end:

dcx b        ;  bc = bc-1
dcx b

call newline

char2_loop:
ldax b
call putchar

dcx b

dcr d
jz char2_end

jmp char2_loop
char2_end:


hlt

include 'include\getchar.inc'
include 'include\getline.inc'
include 'include\putstr.inc'
include 'include\putchar.inc'
include 'include\newline.inc'

text1: db 'Reversed text ...',10,13,'Enter text: ',0
text2: db 10,13,'Reversed: ',0
input: ds 30
