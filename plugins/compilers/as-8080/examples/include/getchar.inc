; load a char from keyboard
; result in reg. A

getchar:
    in 10h
    ani 1 ; test status byte if char is ready
    jz getchar
    in 11h 	; read char
    out 11h	; echo char
    ret
