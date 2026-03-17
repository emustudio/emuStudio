; https://espamatica.com/zx-spectrum-assembly-space-battle-0x0c-sound/

; Standalone ZX Spectrum 48K beeper demo based on the note table from the
; original article.
;
; Assemble as code at 32768 (8000H) and run with:
;   RANDOMIZE USR 32768

org  0x8000

NOTE_DURATION_DIV EQU 0x04

start:
call Play

jr start

; -------------------------------------------------------------------
; ROM beeper routine.
;
; Input: HL -> Note.
;        DE -> Duration.
;
; Alters the value of the AF, BC, DE, HL and IX registers.
; -------------------------------------------------------------------
BEEP   EQU 0x03b5

; -------------------------------------------------------------------
; Notes to be uploaded to HL
; -------------------------------------------------------------------
C_0    EQU 0x6868
Cs_0   EQU 0x628d
D_0    EQU 0x5d03
Ds_0   EQU 0x57bf
E_0    EQU 0x52d7
F_0    EQU 0x4e2b
Fs_0   EQU 0x49cc
G_0    EQU 0x45a3
Gs_0   EQU 0x41b6
A_0    EQU 0x3e06
As_0   EQU 0x3a87
B_0    EQU 0x373e
C_1    EQU 0x3425
Cs_1   EQU 0x3134
D_1    EQU 0x2e6f
Ds_1   EQU 0x2bd3
E_1    EQU 0x295c
F_1    EQU 0x2708
Fs_1   EQU 0x24d5
G_1    EQU 0x22c2
Gs_1   EQU 0x20cd
A_1    EQU 0x1ef4
As_1   EQU 0x1d36
B_1    EQU 0x1b90
C_2    EQU 0x1a02
Cs_2   EQU 0x188b
D_2    EQU 0x1728
Ds_2   EQU 0x15da
E_2    EQU 0x149e
F_2    EQU 0x1374
Fs_2   EQU 0x125b
G_2    EQU 0x1152
Gs_2   EQU 0x1058
A_2    EQU 0x0f6b
As_2   EQU 0x0e9d
B_2    EQU 0x0db8
C_3    EQU 0x0cf2
Cs_3   EQU 0x0c36
D_3    EQU 0x0b86
Ds_3   EQU 0x0add
E_3    EQU 0x0a40
F_3    EQU 0x09ab
Fs_3   EQU 0x091e
G_3    EQU 0x089a
Gs_3   EQU 0x081c
A_3    EQU 0x07a6
As_3   EQU 0x0736
B_3    EQU 0x06cd
C_4    EQU 0x066a
Cs_4   EQU 0x060c
D_4    EQU 0x05b3
Ds_4   EQU 0x0560
E_4    EQU 0x0511
F_4    EQU 0x04c6
Fs_4   EQU 0x0480
G_4    EQU 0x043d
Gs_4   EQU 0x03ff
A_4    EQU 0x03c4
As_4   EQU 0x038c
B_4    EQU 0x0357
C_5    EQU 0x0325
Cs_5   EQU 0x02f7
D_5    EQU 0x02ca
Ds_5   EQU 0x02a0
E_5    EQU 0x0279
F_5    EQU 0x0254
Fs_5   EQU 0x0231
G_5    EQU 0x020f
Gs_5   EQU 0x01f0
A_5    EQU 0x01d3
As_5   EQU 0x01b7
B_5    EQU 0x019c
C_6    EQU 0x0183
Cs_6   EQU 0x016c
D_6    EQU 0x0156
Ds_6   EQU 0x0141
E_6    EQU 0x012d
F_6    EQU 0x011b
Fs_6   EQU 0x0109
G_6    EQU 0x00f8
Gs_6   EQU 0x00e9
A_6    EQU 0x00da
As_6   EQU 0x00cc
B_6    EQU 0x00bf
C_7    EQU 0x00b2
Cs_7   EQU 0x00a7
D_7    EQU 0x009c
Ds_7   EQU 0x0091
E_7    EQU 0x0087
F_7    EQU 0x007e
Fs_7   EQU 0x0075
G_7    EQU 0x006d
Gs_7   EQU 0x0065
A_7    EQU 0x005e
As_7   EQU 0x0057
B_7    EQU 0x0050
C_8    EQU 0x004a
Cs_8   EQU 0x0044
D_8    EQU 0x003e
Ds_8   EQU 0x0039
E_8    EQU 0x0034
F_8    EQU 0x0030
Fs_8   EQU 0x002b
G_8    EQU 0x0027
Gs_8   EQU 0x0023
A_8    EQU 0x0020
As_8   EQU 0x001c
B_8    EQU 0x019


; -------------------------------------------------------------------
; Duration factors to be loaded in DE.
;
; The ROM BEEP routine expects DE = frequency * duration_in_seconds.
; Dividing the frequency constant by 4 yields quarter-second notes, which
; makes the melody recognizable as a standalone demo.
; -------------------------------------------------------------------
C_0_f  EQU 0x0010 / NOTE_DURATION_DIV
Cs_0_f EQU 0x0011 / NOTE_DURATION_DIV
D_0_f  EQU 0x0012 / NOTE_DURATION_DIV
Ds_0_f EQU 0x0013 / NOTE_DURATION_DIV
E_0_f  EQU 0x0014 / NOTE_DURATION_DIV
F_0_f  EQU 0x0015 / NOTE_DURATION_DIV
Fs_0_f EQU 0x0017 / NOTE_DURATION_DIV
G_0_f  EQU 0x0018 / NOTE_DURATION_DIV
Gs_0_f EQU 0x0019 / NOTE_DURATION_DIV
A_0_f  EQU 0x001b / NOTE_DURATION_DIV
As_0_f EQU 0x001d / NOTE_DURATION_DIV
B_0_f  EQU 0x001e / NOTE_DURATION_DIV
C_1_f  EQU 0x0020 / NOTE_DURATION_DIV
Cs_1_f EQU 0x0022 / NOTE_DURATION_DIV
D_1_f  EQU 0x0024 / NOTE_DURATION_DIV
Ds_1_f EQU 0x0026 / NOTE_DURATION_DIV
E_1_f  EQU 0x0029 / NOTE_DURATION_DIV
F_1_f  EQU 0x002b / NOTE_DURATION_DIV
Fs_1_f EQU 0x002e / NOTE_DURATION_DIV
G_1_f  EQU 0x0031 / NOTE_DURATION_DIV
Gs_1_f EQU 0x0033 / NOTE_DURATION_DIV
A_1_f  EQU 0x0037 / NOTE_DURATION_DIV
As_1_f EQU 0x003a / NOTE_DURATION_DIV
B_1_f  EQU 0x003d / NOTE_DURATION_DIV
C_2_f  EQU 0x0041 / NOTE_DURATION_DIV
Cs_2_f EQU 0x0045 / NOTE_DURATION_DIV
D_2_f  EQU 0x0049 / NOTE_DURATION_DIV
Ds_2_f EQU 0x004d / NOTE_DURATION_DIV
E_2_f  EQU 0x0052 / NOTE_DURATION_DIV
F_2_f  EQU 0x0057 / NOTE_DURATION_DIV
Fs_2_f EQU 0x005c / NOTE_DURATION_DIV
G_2_f  EQU 0x0062 / NOTE_DURATION_DIV
Gs_2_f EQU 0x0067 / NOTE_DURATION_DIV
A_2_f  EQU 0x006e / NOTE_DURATION_DIV
As_2_f EQU 0x0074 / NOTE_DURATION_DIV
B_2_f  EQU 0x007b / NOTE_DURATION_DIV
C_3_f  EQU 0x0082 / NOTE_DURATION_DIV
Cs_3_f EQU 0x008a / NOTE_DURATION_DIV
D_3_f  EQU 0x0092 / NOTE_DURATION_DIV
Ds_3_f EQU 0x009b / NOTE_DURATION_DIV
E_3_f  EQU 0x00a4 / NOTE_DURATION_DIV
F_3_f  EQU 0x00ae / NOTE_DURATION_DIV
Fs_3_f EQU 0x00b9 / NOTE_DURATION_DIV
G_3_f  EQU 0x00c4 / NOTE_DURATION_DIV
Gs_3_f EQU 0x00cf / NOTE_DURATION_DIV
A_3_f  EQU 0x00dc / NOTE_DURATION_DIV
As_3_f EQU 0x00e9 / NOTE_DURATION_DIV
B_3_f  EQU 0x00f6 / NOTE_DURATION_DIV
C_4_f  EQU 0x0105 / NOTE_DURATION_DIV
Cs_4_f EQU 0x0115 / NOTE_DURATION_DIV
D_4_f  EQU 0x0125 / NOTE_DURATION_DIV
Ds_4_f EQU 0x0137 / NOTE_DURATION_DIV
E_4_f  EQU 0x0149 / NOTE_DURATION_DIV
F_4_f  EQU 0x015d / NOTE_DURATION_DIV
Fs_4_f EQU 0x0172 / NOTE_DURATION_DIV
G_4_f  EQU 0x0188 / NOTE_DURATION_DIV
Gs_4_f EQU 0x019f / NOTE_DURATION_DIV
A_4_f  EQU 0x01b8 / NOTE_DURATION_DIV
As_4_f EQU 0x01d2 / NOTE_DURATION_DIV
B_4_f  EQU 0x01ed / NOTE_DURATION_DIV
C_5_f  EQU 0x020b / NOTE_DURATION_DIV
Cs_5_f EQU 0x022a / NOTE_DURATION_DIV
D_5_f  EQU 0x024b / NOTE_DURATION_DIV
Ds_5_f EQU 0x026e / NOTE_DURATION_DIV
E_5_f  EQU 0x0293 / NOTE_DURATION_DIV
F_5_f  EQU 0x02ba / NOTE_DURATION_DIV
Fs_5_f EQU 0x02e4 / NOTE_DURATION_DIV
G_5_f  EQU 0x0310 / NOTE_DURATION_DIV
Gs_5_f EQU 0x033e / NOTE_DURATION_DIV
A_5_f  EQU 0x0370 / NOTE_DURATION_DIV
As_5_f EQU 0x03a4 / NOTE_DURATION_DIV
B_5_f  EQU 0x03db / NOTE_DURATION_DIV
C_6_f  EQU 0x0417 / NOTE_DURATION_DIV
Cs_6_f EQU 0x0455 / NOTE_DURATION_DIV
D_6_f  EQU 0x0497 / NOTE_DURATION_DIV
Ds_6_f EQU 0x04dd / NOTE_DURATION_DIV
E_6_f  EQU 0x0527 / NOTE_DURATION_DIV
F_6_f  EQU 0x0575 / NOTE_DURATION_DIV
Fs_6_f EQU 0x05c8 / NOTE_DURATION_DIV
G_6_f  EQU 0x0620 / NOTE_DURATION_DIV
Gs_6_f EQU 0x067d / NOTE_DURATION_DIV
A_6_f  EQU 0x06e0 / NOTE_DURATION_DIV
As_6_f EQU 0x0749 / NOTE_DURATION_DIV
B_6_f  EQU 0x07b8 / NOTE_DURATION_DIV
C_7_f  EQU 0x082d / NOTE_DURATION_DIV
Cs_7_f EQU 0x08a9 / NOTE_DURATION_DIV
D_7_f  EQU 0x092d / NOTE_DURATION_DIV
Ds_7_f EQU 0x09b9 / NOTE_DURATION_DIV
E_7_f  EQU 0x0a4d / NOTE_DURATION_DIV
F_7_f  EQU 0x0aea / NOTE_DURATION_DIV
Fs_7_f EQU 0x0b90 / NOTE_DURATION_DIV
G_7_f  EQU 0x0c40 / NOTE_DURATION_DIV
Gs_7_f EQU 0x0cfa / NOTE_DURATION_DIV
A_7_f  EQU 0x0dc0 / NOTE_DURATION_DIV
As_7_f EQU 0x0e91 / NOTE_DURATION_DIV
B_7_f  EQU 0x0f6f / NOTE_DURATION_DIV
C_8_f  EQU 0x105a / NOTE_DURATION_DIV
Cs_8_f EQU 0x1153 / NOTE_DURATION_DIV
D_8_f  EQU 0x125b / NOTE_DURATION_DIV
Ds_8_f EQU 0x1372 / NOTE_DURATION_DIV
E_8_f  EQU 0x149a / NOTE_DURATION_DIV
F_8_f  EQU 0x15d4 / NOTE_DURATION_DIV
Fs_8_f EQU 0x1720 / NOTE_DURATION_DIV
G_8_f  EQU 0x1880 / NOTE_DURATION_DIV
Gs_8_f EQU 0x19f5 / NOTE_DURATION_DIV
A_8_f  EQU 0x1b80 / NOTE_DURATION_DIV
As_8_f EQU 0x1d23 / NOTE_DURATION_DIV
B_8_f  EQU 0x1ede / NOTE_DURATION_DIV


Song_1:
dw G_2_f,G_2,   G_2_f, G_2,  G_2_f,G_2,    Ds_2_f,Ds_2, As_2_f,As_2
dw G_2_f,G_2,   Ds_2_f,Ds_2, As_2_f,As_2,  G_2_f,G_2,   G_2_f,G_2 
dw G_2_f,G_2,   G_2_f, G_2,  Ds_2_f,Ds_2,  As_2_f,As_2, G_2_f,G_2 
dw Ds_2_f,Ds_2, As_2_f,As_2, G_2_f,G_2,    D_3_f,D_3,   D_3_f,D_3 
dw D_3_f,D_3,   Ds_3_f,Ds_3, As_2_f,As_2,  Fs_2_f,Fs_2, Ds_2_f,Ds_2
dw As_2_f,As_2, G_2_f,G_2

dw G_3_f,G_3,   G_2_f,G_2,   G_2_f, G_2,   G_3_f,G_3,   Fs_3_f,Fs_3 
dw F_3_f,F_3,   E_3_f,E_3,   Ds_3_f,Ds_3,  E_3_f,E_3,   Gs_2_f,Gs_2
dw Cs_3_f,Cs_3, C_3_f,C_3,   B_2_f,B_2,    As_2_f,As_2, A_2_f,A_2
dw As_2_f,As_2, Ds_2_f,Ds_2, Fs_2_f,Fs_2,  Ds_2_f,Ds_2, Fs_2_f,Fs_2
dw As_2_f,As_2, G_2_f,G_2,   As_2_f,As_2,  D_3_f,D_3

dw G_3_f,G_3,   G_2_f,G_2,   G_2_f,G_2,    G_3_f,G_3,   Fs_3_f,Fs_3
dw F_3_f,F_3,   E_3_f,E_3,   Ds_3_f,Ds_3,  E_3_f,E_3,   Gs_2_f,Gs_2 
dw Cs_3_f,Cs_3, C_3_f,C_3,   B_2_f,B_2,    As_2_f,As_2, A_2_f,A_2
dw As_2_f,As_2, Ds_2_f,Ds_2, Fs_2_f,Fs_2,  Ds_2_f,Ds_2, As_2_f,As_2
dw G_2_f,G_2,   A_2_f,A_2,   G_2_f,G_2

dw G_2_f,G_2,   G_2_f, G_2,  G_2_f,G_2,    Ds_2_f,Ds_2, As_2_f,As_2
dw G_2_f,G_2,   Ds_2_f,Ds_2, As_2_f,As_2,  G_2_f,G_2,   G_2_f,G_2
dw G_2_f,G_2,   G_2_f,G_2,   Ds_2_f,Ds_2,  As_2_f,As_2, G_2_f,G_2
dw Ds_2_f,Ds_2, As_2_f,As_2, G_2_f,G_2


Song_2:
dw D_4_f,D_4,  D_4_f,D_4,  D_4_f,D_4,  G_4_f,G_4,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_5_f,G_5,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_5_f,G_5,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  C_5_f,C_5,  A_4_f,A_4

dw D_4_f,D_4,  D_4_f,D_4,  D_4_f,D_4,  G_4_f,G_4,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_5_f,G_5,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_5_f,G_5,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  C_5_f,C_5,  A_4_f,A_4

dw D_4_f,D_4,  D_4_f,D_4,  E_4_f,E_4,  E_4_f,E_4,   C_5_f,C_5
dw B_4_f,B_4,  A_4_f,A_4,  G_4_f,G_4,  G_4_f,G_4,   A_4_f,A_4
dw B_4_f,B_4,  A_4_f,A_4,  E_4_f,E_4,  Fs_4_f,Fs_4, D_4_f,D_4
dw D_4_f,D_4,  E_4_f,E_4,  E_4_f,E_4,  C_5_f,C_5,   C_5_f,C_5
dw B_4_f,B_4,  A_4_f,A_4,  G_4_f,G_4,  D_5_f,D_5,   D_5_f,D_5
dw A_4_f,A_4,  D_4_f,D_4,  D_4_f,D_4,  E_4_f,E_4,   E_4_f,E_4
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_4_f,G_4,   G_4_f,G_4
dw A_4_f,A_4,  B_4_f,B_4,  A_4_f,A_4,  E_4_f,E_4,   Fs_4_f,Fs_4

dw D_5_f,D_5,  D_5_f,D_5,  G_5_f,G_5,  F_5_f,F_5,   Ds_5_f, Ds_5
dw D_5_f,D_5,  C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,   G_4_f, G_4
dw D_5_f,D_5

dw D_4_f,D_4,  D_4_f,D_4,  D_4_f,D_4,  G_4_f,G_4,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_5_f,G_5,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  A_4_f,A_4,  G_5_f,G_5,   D_5_f,D_5
dw C_5_f,C_5,  B_4_f,B_4,  C_5_f,C_5,  A_4_f,A_4
dw 0000

ptrSound:
dw Song_2

Play:
ld   hl, (ptrSound)
ld   e, (hl)
inc  hl
ld   d, (hl)
ld   a, d
or   e
jr   nz, play_cont

play_reset:
ld   hl, Song_1
ld   (ptrSound), hl
ret

play_cont:
inc  hl
ld   c, (hl)
inc  hl
ld   b, (hl)
inc  hl
ld   (ptrSound), hl
ld   h, b
ld   l, c

call BEEP

ret

