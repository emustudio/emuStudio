; Modified Disk Boot Loader (MDBL)
; reverse-engineered from simh emulator
; only use with simh-formatted disk images

org 0xFF00

di             ; Disable interrupts
mvi b, 0x80
mvi a, 0x0E    ; Reset the SIMH pseudo device

reset:
out 0xFE
dcr b
jnz reset      ; Do it for 128 times

mvi a, 0x16    ; Stop timer interrupts
out 0xFE
mvi a, 0x12    ; Determines whether machine has banked memory
out 0xFE
in 0xFE        ; Number of banks
ora a          ; zero=no banks
jz copy_ourselves

mvi a, 0x0C    ; Select bank
out 0xFE
xra a          ; bank=0
out 0xFE

copy_ourselves:
lxi h, 0x5C00  ; destination address
lxi d, start   ; source address
mvi c, 0x88    ; 136 times (bytes)

do_copy:
ldax d         ; A <- source
mov m, a       ; dest <- A
inx d
inx h
dcr c
jnz do_copy
jmp 0x5C00

; ORG 0x5C00   ; cannot do that in emuStudio
start:         ; at 0x5C00
lxi sp, 0x5D21
mvi a, 0       ; drive 0
out 0x8        ; select (track = sector = 0)
mvi a, 4       ; head load
out 0x9
jmp 0x5C19

in 0x8         ; at 0x5C0E; read status port
ani 2          ; when 0, head movement allowed
jnz 0x5C0E     ; repeat until head movement allowed

mvi a, 2       ; head out
out 0x9
in 0x8         ; at 0x5C19; read status port
ani 0x40       ; when 0, indicates head is on track 0
jnz 0x5C0E     ; repeat until head is on track 0

lxi d, 0
mvi b, 0x8     ; boot sector number

push b         ; at 0x5C25
push d

lxi d, 0x8086  ; data read count: D=unimportant, E=data count (0x86 = 134 bytes = 1 sector)
lxi h, 0x5C88  ; destination where to read disk data

in 0x9         ; at 0x5C2D; next sector if head is loaded, read sector number
rar            ; port2: x x n n n n n t; where x-unused, n-sector number, t-sector true
jc 0x5C2D      ; repeat until "sector true"

ani 0x1F       ; get just sector number
cmp b          ; is it the one we requested?
jnz 0x5C2D     ; if not, repeat with next sector

in 0x8         ; at 0x5C39; read status port
ora a          ; test 0x80 bit (sign flag will be set in this case)
jm 0x5C39      ; When bit7=0, indicates that read circuit has new byte to read, otherwise repeat

in 0x0A        ; read data from disk
mov m, a       ; (hl) <- data byte
inx h
dcr e          ; count--
jnz 0x5C39     ; if count!=0, repeat

pop d          ; DE=0
lxi h, 0x5C8B  ; source address (where the sector is loaded), ignore first 3 bytes
mvi b, 0x80    ; data count (=128 bytes sector)

mov a, m       ; at 0x5C4D; A <- (HL)
stax d         ; (DE) <- A
inx h
inx d
dcr b
jnz 0x5C4D     ; repeat until 128 bytes are read

pop b          ; B=sector number
lxi h, 0x5C00  ; our code start address
mov a, d       ; latest dest address high byte (where the copy ended so far)
cmp h          ; are we crossing 0x5C00 boundary?
jnz 0x5C60     ; jump if H != D

mov a, e       ; latest dest address low byte
cmp l          ; are we crossing 0x00 boundary ? (only if D=0x5C)

jnc 0x5C80     ; at 0x5C60 - CY=H>D or (H=D and L>E); if H<=D or (H=D and L<=E) - we're DONE.
               ; that means we are copying at least 5C00 data since initially D=0
inr b          ; sector++
inr b          ; sector++ (continue skewed sector line)
mov a, b
cpi 0x20       ; sector < 32 ?
jc 0x5C25      ; if yes, jump to read next sector

mvi b, 1       ; new sector number (new skewed sector line)
jz 0x5C25      ; sector == 32 (the last one); if yes jump to read sector "line" starting at B

in 0x8         ; at 0x5C70; here the sector number > 32 (must increment track); read status port
ani 2          ; when 0, head movement is allowed
jnz 0x5C70     ; repeat until head movement is allowed

mvi a, 1       ; head in (track++; sector=0)
out 0x9
mvi b, 0       ; new sector number (new skewed sector line)
jmp 0x5C25     ; read the "line"

mvi a, 0x80    ; at 0x5C80; unselect drive
out 0x8
ei             ; enable interrupts
jmp 0          ; jump to CP/M "kernel"
