org 0000H

ld HL, message
loop:
ld A, (HL)
cp 0
jr Z, end
out (11H), A
inc HL
jp loop

end:
halt

message:
db "Hello world!",0