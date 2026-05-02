; ZX Spectrum 48K AY-3-8910 example
; Plays a short three-voice arrangement of the opening phrase of "Ode to Joy".
;
; Use the ZxSpectrum48KAY.toml configuration, assemble as code at 32768 (8000H),
; then run with:
;   RANDOMIZE USR 32768
;
; Register select uses BC = FFFDh and register data uses BC = BFFDh,
; matching the AY-3-8910 ports exposed by the emulator's ZX Spectrum bus.

org 8000H

AY_PORT_LO   equ 0FDH
AY_SELECT_HI equ 0FFH
AY_DATA_HI   equ 0BFH

AY_MIXER    equ 7
AY_VOLUME_A equ 8
AY_VOLUME_B equ 9
AY_VOLUME_C equ 10

LEAD_VOLUME equ 0EH
HARM_VOLUME equ 0AH
BASS_VOLUME equ 06H

TICK_DELAY equ 4500
GAP_TICKS  equ 1

NOTE_G2 equ 045CH
NOTE_A2 equ 03E2H
NOTE_C3 equ 0344H
NOTE_G3 equ 022EH
NOTE_A3 equ 01F1H
NOTE_B3 equ 01BBH
NOTE_C4 equ 01A2H
NOTE_D4 equ 0174H
NOTE_E4 equ 014CH
NOTE_F4 equ 0139H
NOTE_C5 equ 00D1H
NOTE_D5 equ 00BAH
NOTE_E5 equ 00A6H
NOTE_F5 equ 009DH
NOTE_G5 equ 008CH

start:
    call ay_init
    ld hl, score

play_next:
    ld a, (hl)
    inc hl
    or a
    jr z, finish
    push af

    ld e, (hl)
    inc hl
    ld d, (hl)
    inc hl
    call set_channel_a

    ld e, (hl)
    inc hl
    ld d, (hl)
    inc hl
    call set_channel_b

    ld e, (hl)
    inc hl
    ld d, (hl)
    inc hl
    call set_channel_c

    pop af
    call wait_ticks
    call short_gap
    jr play_next

finish:
    call mute_all

hang:
    halt
    jr hang

ay_init:
    ld a, AY_MIXER
    ld e, 038H
    call write_ay
    call mute_all
    ret

set_channel_a:
    ld a, e
    or d
    jr z, channel_a_rest

    ld a, 0
    call write_ay
    ld a, 1
    ld e, d
    call write_ay
    ld a, AY_VOLUME_A
    ld e, LEAD_VOLUME
    call write_ay
    ret

channel_a_rest:
    ld a, AY_VOLUME_A
    ld e, 0
    jp write_ay

set_channel_b:
    ld a, e
    or d
    jr z, channel_b_rest

    ld a, 2
    call write_ay
    ld a, 3
    ld e, d
    call write_ay
    ld a, AY_VOLUME_B
    ld e, HARM_VOLUME
    call write_ay
    ret

channel_b_rest:
    ld a, AY_VOLUME_B
    ld e, 0
    jp write_ay

set_channel_c:
    ld a, e
    or d
    jr z, channel_c_rest

    ld a, 4
    call write_ay
    ld a, 5
    ld e, d
    call write_ay
    ld a, AY_VOLUME_C
    ld e, BASS_VOLUME
    call write_ay
    ret

channel_c_rest:
    ld a, AY_VOLUME_C
    ld e, 0
    jp write_ay

mute_all:
    ld a, AY_VOLUME_A
    ld e, 0
    call write_ay
    ld a, AY_VOLUME_B
    call write_ay
    ld a, AY_VOLUME_C
    jp write_ay

short_gap:
    call mute_all
    ld a, GAP_TICKS
    jp wait_ticks

wait_ticks:
    or a
    ret z
    ld d, a

wait_tick:
    ld bc, TICK_DELAY

wait_inner:
    dec bc
    ld a, b
    or c
    jr nz, wait_inner

    dec d
    jr nz, wait_tick
    ret

write_ay:
    push bc
    ld c, AY_PORT_LO
    ld b, AY_SELECT_HI
    out (c), a
    ld b, AY_DATA_HI
    ld a, e
    out (c), a
    pop bc
    ret

; Event format:
;   db <duration in ticks>
;   dw <lead channel>, <harmony channel>, <bass channel>
; Duration 0 marks the end of the score.
score:
    db 4
    dw NOTE_E5, NOTE_C4, NOTE_C3
    db 4
    dw NOTE_E5, NOTE_C4, NOTE_C3
    db 4
    dw NOTE_F5, NOTE_D4, NOTE_G2
    db 4
    dw NOTE_G5, NOTE_E4, NOTE_C3

    db 4
    dw NOTE_G5, NOTE_E4, NOTE_C3
    db 4
    dw NOTE_F5, NOTE_D4, NOTE_G2
    db 4
    dw NOTE_E5, NOTE_C4, NOTE_C3
    db 4
    dw NOTE_D5, NOTE_B3, NOTE_G2

    db 4
    dw NOTE_C5, NOTE_A3, NOTE_A2
    db 4
    dw NOTE_C5, NOTE_A3, NOTE_A2
    db 4
    dw NOTE_D5, NOTE_B3, NOTE_G2
    db 4
    dw NOTE_E5, NOTE_C4, NOTE_C3

    db 4
    dw NOTE_D5, NOTE_B3, NOTE_G2
    db 4
    dw NOTE_C5, NOTE_A3, NOTE_A2
    db 8
    dw NOTE_C5, NOTE_G3, NOTE_C3
    db 0
