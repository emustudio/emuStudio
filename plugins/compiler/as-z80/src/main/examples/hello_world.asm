; prints Hello, world!
; Best use with ADM-3A terminal

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
db 201, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 187, 10, 13
db 186, "Hello world!", 186, 10, 13
db 200, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 188
db 0


