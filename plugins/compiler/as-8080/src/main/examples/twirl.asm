; Twirling bar animation by Paolo Amoroso <info@paoloamoroso.com>
;
; Runs on an Altair 8800 with an ADM-3A terminal. Press any key
; to interrupt the program.


FRAMES    equ  8              ; Number of animation frames

CLS       equ  1ah            ; ADM-3A escape sequence
HOME      equ  1eh            ; ADM-3A escape sequence
STATUS	equ  10h            ; Input status port
READY     equ  1              ; Character ready status mask


          mvi  a, CLS         ; Clear screen
          call putchar

loop:     lxi  h, ANIM
          mvi  b, FRAMES

loop1:    mvi  a, HOME        ; Go to home
          call putchar

          mov  a, m           ; Print current frame
          call putchar

          inx  h
          dcr  b

          push psw
          in   STATUS         ; Key pressed?
          ani  READY
          jnz  exit           ; Yes
          pop  psw

          jnz  loop1

          jmp  loop
					
exit:     hlt


ANIM:     db   '|/-\|/-\'    ; 8 frames


include	'include\putchar.inc'
