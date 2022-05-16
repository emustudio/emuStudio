; Twirling bar animation by Paolo Amoroso <info@paoloamoroso.com>
;
; Runs on an Altair 8800 with an ADM-3A terminal. Press any key
; to interrupt the program.


FRAMES    equ  8              ; Number of animation frames

CLS       equ  1ah            ; ADM-3A escape sequence
HOME      equ  1eh            ; ADM-3A escape sequence
STATUS    equ  10h            ; Input status port
READY     equ  1              ; Character ready status mask


          ld  a, CLS          ; Clear screen
          call putchar

loop:     ld  hl, ANIM        ; Initialize frame pointer...
          ld  b, FRAMES       ; ...and count

loop1:    ld  a, HOME         ; Go to home
          call putchar

          ld  a, (hl)         ; Print current frame
          call putchar

          push af
          in a, (STATUS)
          and  READY          ; Key pressed?
          jp nz,  exit        ; Yes
          pop  af

          inc  hl
          dec  b
          jp nz,  loop1

          jp  loop


exit:     pop af              ; Clear psw left on stack
          halt


ANIM:     db   '|/-\|/-\'     ; 8 frames


include	'include\putchar.inc'
