; Timer interrupt test (using SIMH-pseudo device)
; default handler address: 0xFC00
; timer in simh-pseudo operates at interrupt mode 0 as a normal call

dec sp ; initialize stack
im 0   ; interrupt mode 0
ei     ; enable interrupts

ld a, 21       ; startTimerInterruptsCmd
out (0xfe), a  ; out to simh-pseudo device

loop:
ei
jp loop

halt

i1: db 'Hello from timer',10,13,0

include 'examples/as-z80/include/putstr.inc'

; timer interrupt handler
org 0xFC00

  ld hl, i1
  call putstr
  reti
