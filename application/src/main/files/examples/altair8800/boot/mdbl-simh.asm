; MDBL.ASM
; disassembled from simh emulator

di
ld b, 0x80 ; how many times reset SIMH
ld a, 0x0E ; reset SIMH

resetSimhLoop:
out (0xFE), a
dec b
jp nz, resetSimhLoop

ld a, 0x16 ; stop timer interrupts
out (0xFE), a
ld a, 0x12 ; finds out if we have banked memory
out (0xFE), a

in a, (0xFE) ; get banks count
or a
jp z, relocateSelf ; jump if no banked memory present

ld a, 0x0C    ; select bank
out (0xFE), a
xor a         ; bank = 0
out (0xFE), a

relocateSelf:
ld hl, 0x5C00  ; relocate address
ld de, start   ; code to relocate
ld c, 0x88     ; bytes count

relocate:
ld a, (de)
ld (hl), a
inc de
inc hl
dec c
jp nz, relocate

jp 0x5C00

start:
ld sp, 0x5D21
ld a, 0  ; set drive 0
out (0x08), a

ld a, 4 ; head load
out (0x09), a
jp l5

waitForDrive:
in a, (0x08) ; port 1 status
and 2 ; is the drive alive? (selected)
jp nz, waitForDrive

ld a, 2 ; head out
out (0x09), a

l5:
in a, (0x08) ; port 1 status
and 0x40 ;
jp nz, waitForDrive

ld de, 0
ld b, 8

l11:

push bc
push de

ld de, 0x8086
ld hl, l13

l6:
in a, (0x09)
rra
jp c, l6

and 0x1F
cp b
jp nz, l6

l7:
in a, (0x08)
or a
jp m, l7

in a, (0xA)
ld (hl), a
inc hl
dec e
jp nz, l7

pop de
ld hl, 0x5C8B
ld b, 0x80

l8:
ld a, (hl)
ld (de), a
inc hl
inc de
dec b
jp nz, l8

pop bc
ld hl, 0x5C00
ld a, d
cp h
jp nz, l9

ld a, e
cp l

l9:
jp nc, l10
inc b
inc b
ld a, b
cp 0x20
jp c, l11

ld b, 1
jp z, l11

l12:
in a, (0x08)
and 2
jp nz, l12

ld a, 1
out (0x09), a
ld b, 0
jp l11

l10:
ld a, 0x80
out (0x08), a
ei

jp 0

l13:



