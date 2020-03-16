; print string on terminal screen
; string specified by address in reg. pair HL
; end of string - char 0

putstr:
    mov a, m
    inx h
    cpi 0
    rz
    out 11h
    jmp putstr
