; Tests signalling interrupts on input

lxi h, hello  ; load address of 'hello' label to HL
call print    ; print hello

mvi a, 1      ; 88-SIO: input interrupts enable
out 0x10

ei            ; enable CPU interrupts
loop:
jmp loop      ; do this forever (or until...)


; interrupt handler
org 0x38      ; assuming interrupt vector is set to 7
in 0x11       ; read char from 88-SIO (and ignore it)
lxi h, key    ; load address of 'key' label to HL
call print    ; print "key pressed"
ret           ; return from the interrupt


hello: db 'Hello, world!',10,13,0
key: db 'Key pressed!',10,13,0

; Procedure for printing text to terminal.
; Input: pair HL must contain the address of the ASCIIZ string
print:
    mov a, m  ; load character from HL
    inx h     ; increment HL
    cpi 0     ; is the character = 0?
    rz        ; yes; quit
    out 11h   ; otherwise; show it
    jmp print ; and repeat from the beginning
