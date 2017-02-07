; read string from keyboard
; chars loaded after ENTER
; reg. pair DE - address for storing read text

getline:
    ld c, 0 ; char count
next_char:
    in a, (10h)
    and 1 ; test status byte if char is ready
    jp z, next_char
    in a, (11h) ; read char

    ; interpret some special keys
    cp 13 ; CR
    jp z, getline_ret
    cp 10 ; LF
    jp z, getline_ret
    cp 8  ; backspace
    jp nz, save_char

    ld a, c ; test if we are in the beginning
    cp 0
    jp z, next_char ; if yes, nothing is done (ignored BS)
    dec de ; address--;
    dec c ; charcount--;
    ld a,8 ; send backspace to the screen
    out (11h), a
    ld a, 32 ; clear char by ' '
    out (11h), a
    ld a,8 ; backspace again
    out (11h), a
    jp next_char
save_char:
    out (11h), a
    ld (de), a
    inc de
    inc c
    jp next_char
getline_ret:
    ld a, 10
    ld (de), a
    inc de
    ld a,13
    ld (de), a
    inc de
    ld a, 0
    ld (de), a
    ret
