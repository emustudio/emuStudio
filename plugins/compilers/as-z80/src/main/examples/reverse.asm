; Print reversed text

org 1000

dec sp       ; stack initialization (0FFFFh)

ld hl,text1 
call putstr  ; print text1
ld de,input  ; address for string input
call getline ; read from keyboard

ld bc,input

ld d,0      ; chars counter

char_loop:
ld a, (bc) 
inc bc        ; bc = bc+1
cp 10       ; end of input?
jp z, char_end
cp 13
jp z, char_end

inc d        ; d =d+1
jp char_loop
char_end:

dec bc        ;  bc = bc-1
dec bc

call newline

char2_loop:
ld a, (bc)
call putchar

dec bc

dec d
jp z, char2_end

jp char2_loop
char2_end:

halt

include "include\getchar.inc"
include "include\getline.inc"
include "include\putstr.inc"
include "include\putchar.inc"
include "include\newline.inc"

text1: db "Reversed text ...",10,13,"Enter text: ",0
text2: db 10,13,"Reversed: ",0
input: ds 30
