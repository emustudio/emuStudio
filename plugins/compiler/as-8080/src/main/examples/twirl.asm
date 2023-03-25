; Twirling bar animation by Paolo Amoroso <info@paoloamoroso.com>
;
; Runs on an Altair 8800 with an ADM-3A terminal. Press any key
; to interrupt the program.


FRAMES    equ  8              ; Number of animation frames

CLS       equ  1ah            ; ADM-3A escape sequence
HOME      equ  1eh            ; ADM-3A escape sequence
STATUS    equ  10h            ; Input status port
READY     equ  1              ; Character ready status mask


          mvi  a, CLS         ; Clear screen
          call putchar

loop:     lxi  h, ANIM        ; Initialize frame pointer...
          mvi  b, FRAMES      ; ...and count

loop1:    mvi  a, HOME        ; Go to home
          call putchar

          mov  a, m           ; Print current frame
          call putchar

          push psw
          in   STATUS
          ani  READY          ; Key pressed?
          jnz  exit           ; Yes
          pop  psw

          inx  h
          dcr  b
          jnz  loop1

          jmp  loop


exit:     pop psw             ; Clear psw left on stack
          hlt


ANIM:     db   '|/-\|/-\'     ; 8 frames


include	'include\putchar.inc'
