; reverse-engineered modified disk boot loader (mdbl.bin) from simh emulator

org 0FF00h

di
ld b, 80h

ld a, 0Eh

l1:
out (0FEh), a
dec b
jp nz, l1

ld a, 16h
out (0FEh), a
ld a, 12h
out (0FEh), a

in a, (0FEh)
or a
jp z, l2
ld a, 0Ch
out (0FEh), a
xor a
out (0FEh), a

l2:
ld hl, 5C00h
ld de, l4
ld c, 88h

l3:
ld a, (de)
ld (hl), a
inc de
inc hl
dec c
jp nz, l3
jp 5C00h

l4:
ld sp, 5D21h
ld a, 0
out (8h), a
ld a, 4
out (9h), a
jp 5C19h

in a, (8h)
and 2
jp nz, 5C0Eh
ld a, 2
out (9h), a
in a, (8h)
and 40h
jp nz, 5C0Eh
ld de, 0h
ld b, 8
push bc
push de

ld de, 8086h
ld hl, 5C88h

in a, (9h)
rra
jp c, 5C2Dh
and 1Fh

cp b
jp nz, 5C2Dh
in a, (8h)
or a
jp m, 5C39h
in a, (0Ah)

ld (hl), a
inc hl
dec e
jp nz, 5C39h
pop de
ld hl, 5C8Bh
ld b, 80h
ld a, (hl)
ld (de), a
inc hl
inc de
dec b
jp nz, 5C4Dh
pop bc
ld hl, 5C00h
ld a, d
cp h
jp nz, 5C60h
ld a, e
cp l
jp nc, 5C80h
inc b
inc b

ld a, b
cp 20h
jp c, 5C25h
ld b, 1
jp z, 5C25h

in a, (8h)
and 2h
jp nz, 5C70h
ld a, 1
out (9h), a
ld b, 0
jp 5C25h
ld a, 80h
out (8h), a
ei
jp 0
