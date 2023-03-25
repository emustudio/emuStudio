; ZEXALL / ZEXDOC tests
; simulation of some BDOS calls
;
; load zexall.com / zexdoc.com at location 0x100 into memory
; then compile this code
; set program address to 0x100
; start emulation

di
halt

; bdos simulation
org 5
push af
push de

ld a, 2
cp c ; print char
jp nz, str
ld a, e
out (0x11), a
jp exit


str:
ld a, 9
cp c ; print string
jp nz, exit

putstr:
ld a, (de)
inc de

cp '$'
jp z, exit

out (11h), a
jp putstr

exit:
pop de
pop af
ret
