; print string on terminal screen
; string specified by address in reg. pair HL
; end of string - char 0

putstr:
    ld a, (hl)
    inc hl
    cp 0
    ret z
    out (11h), a
    jp putstr
