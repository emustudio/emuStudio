; read string from keyboard
; chars loaded after ENTER
; reg. pair DE - address for storing read text

getline:
    mvi c, 0 ; char count
next_char:
    in 10h
    ani 1 ; test status byte if char is ready
    jz next_char
    in 11h ; read char

    ; interpret some special keys
    cpi 13 ; CR
    jz getline_ret
    cpi 10 ; LF
    jz getline_ret
    cpi 8  ; backspace
    jnz save_char

    mov a, c ; test if we are in the beginning
    cpi 0
    jz next_char ; if yes, nothing is done (ignored BS)
    dcx d ; address--;
    dcr c ; charcount--;
    mvi a,8 ; send backspace to the screen
    out 11h
    mvi a, 32 ; clear char by ' '
    out 11h
    mvi a,8 ; backspace again
    out 11h
    jmp next_char
save_char:
    out 11h
    stax d
    inx d
    inr c
    jmp next_char
getline_ret:
    mvi a, 10
    stax d
    inx d
    mvi a,13
    stax d
    inx d
    mvi a, 0
    stax d
    ret
