; Beginner ZX Spectrum 48K example.
; Assemble at 8000H and run from BASIC with: RANDOMIZE USR 32768

org 8000H

start:
    ld a, 1                 ; blue border
    out (0FEH), a

    ld hl, 4000H            ; bitmap: alternating vertical stripes
    ld de, 4001H
    ld bc, 17FFH
    ld (hl), 55H
    ldir

    ld hl, 5800H            ; attributes: bright yellow ink on blue paper
    ld de, 5801H
    ld bc, 02FFH
    ld (hl), 4EH
    ldir

finished:
    halt
    jr finished
