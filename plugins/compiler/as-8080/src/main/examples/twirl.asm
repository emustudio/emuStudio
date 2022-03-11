; Twirling bar animation by Paolo Amoroso <info@paoloamoroso.com>
;
; Runs on an Altair 8800 with an ADM-3A terminal. To interrupt
; the program click "Stop emulation".


FRAMES    equ  8              ; Number of animation frames

CLS       equ  1ah            ; ADM-3A escape sequence
HOME      equ  1eh            ; ADM-3A escape sequence


          mvi  a, CLS         ; Clear screen
          call putchar

loop:     lxi  h, ANIM
          mvi  b, FRAMES

loop1:    mvi  a, HOME        ; Go to home
          call putchar

          mov  a, m           ; Print current frame
          push h
          call putchar

          inx  h
          dcr  b
          jnz  loop1

          jmp  loop

          hlt


ANIM:     db   '|/-\|/-\'    ; 8 frames


include 'include\putchar.inc'
