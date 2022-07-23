; load a char from keyboard
; result in reg. A

getchar:
    in a, (10H)
    and 1 ; test status byte if char is ready
    jp z, getchar
    in a, (11H) 	; read char
    out (11H), a	; echo char
    ret
