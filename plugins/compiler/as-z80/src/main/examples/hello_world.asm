; prints Hello, world! in a "message box"
; Best use with ADM-3A terminal to leverage graphical symbols

org 0000H

ld HL, message
loop:
ld A, (HL)
cp 0
jr z, exit
out (11H), A
inc HL
jp loop

exit:
halt

message:
db 201, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 187, 10, 13
db 186, "Hello world!", 186, 10, 13
db 200, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 188
db 0

