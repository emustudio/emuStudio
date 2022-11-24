; Tests signalling interrupts on input

ld hl, hello  ; load address of 'hello' label to HL
call print    ; print hello

ld a, 1       ; 88-SIO: input interrupts enable
out (0x10), a

ei            ; enable CPU interrupts
im 1          ; interrupt mode 1

loop:
ei
jp loop       ; do this forever (or until...)


; interrupt handler
org 0x38      ; 88-sio interrupt vector is ignored in IM 1 mode

in a, (0x11)  ; read char from 88-SIO (and ignore it)
ld hl, key    ; load address of 'key' label to HL
call print    ; print "key pressed"
reti          ; return from the interrupt


hello: db 'Hello, world!',10,13,0
key: db 'Key pressed!',10,13,0

; Procedure for printing text to terminal.
; Input: pair HL must contain the address of the ASCIIZ string
print:
    ld a, (hl)   ; load character from HL
    inc hl       ; increment HL
    cp  0        ; is the character = 0?
    ret z        ; yes; quit
    out (11h), a ; otherwise; show it
    jp print     ; and repeat from the beginning
