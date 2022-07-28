; Generate interrupt test (using SIMH-pseudo device)
; Interrupt mode 2
;
; IM 2

dec sp ; initialize stack

im 2
ld a, inttbl / 256  ; address of interrupt vector table
ld i,a 
ei

main:

ld a, 33  ; genInterruptCmd
out (0xfe), a
ld a, 0 ; inthdlr1
out (0xfe), a
out (0xfe), a // signal interrupt

halt

i1: db 'Hello from int1',10,13,0
i2: db 'Hello from int2',10,13,0

include 'include/putstr.inc'

org 0x100

; interrupt vector table
inttbl:
  dw inthdlr1
  dw inthdlr2
  ; ... up to 0x1FF

  
org 0x200

inthdlr1:
  ld hl, i1
  call putstr
  reti

inthdlr2:
  ld hl, i2
  call putstr
  reti
