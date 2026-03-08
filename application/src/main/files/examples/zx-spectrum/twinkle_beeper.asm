; ZX Spectrum 48K beeper example
; Plays the first phrase of "Twinkle Twinkle Little Star": C C G G A A G
;
; Assemble as code at 32768 (8000H) and run with:
;   RANDOMIZE USR 32768
;
; The tone is produced by toggling ULA port FEh between the EAR/MIC states
; used by the emulator beeper implementation.

org 8000H

BEEPER_PORT equ 0FEH
BEEPER_ON   equ 10H
BEEPER_OFF  equ 08H
NOTE_GAP    equ 4000

start:
    ld hl, melody

next_note:
    ld e, (hl)
    inc hl
    ld d, (hl)
    inc hl
    ld c, (hl)
    inc hl
    ld b, (hl)
    inc hl

    ld a, b
    or c
    jr z, finish

    push hl
    call play_note
    pop hl

    ld bc, NOTE_GAP
    call pause_gap
    jr next_note

finish:
    ld a, BEEPER_OFF
    out (BEEPER_PORT), a

hang:
    halt
    jr hang

play_note:
    ld a, BEEPER_OFF

play_half_wave:
    xor 18H
    out (BEEPER_PORT), a

    push bc
    push de

delay_half_period:
    dec de
    ld a, d
    or e
    jr nz, delay_half_period

    pop de
    pop bc

    dec bc
    ld a, b
    or c
    jr nz, play_half_wave
    ret

pause_gap:
    ld a, BEEPER_OFF
    out (BEEPER_PORT), a

pause_loop:
    dec bc
    ld a, b
    or c
    jr nz, pause_loop
    ret

; Each note is stored as:
;   dw <half-period delay loop count>, <number of half-waves>
;
; Delay values are calibrated for the simple busy-loop above on a 3.5 MHz ZX Spectrum.
melody:
    dw 254,  94   ; C4, about 180 ms
    dw 254,  94   ; C4
    dw 169, 141   ; G4
    dw 169, 141   ; G4
    dw 150, 158   ; A4
    dw 150, 158   ; A4
    dw 169, 329   ; G4, about 420 ms
    dw 0, 0
