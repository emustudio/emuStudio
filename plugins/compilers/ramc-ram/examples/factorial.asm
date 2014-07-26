; Factorial N
;
; input:  N
; output: N!
;

<input> 8

  read 1
  load 1
  jgtz ok
  load =1
  store 2
  jmp exit
ok:
  load 1
  store 2
loop:
  sub =1
  jz exit
  store 1
  mul 2
  store 2
  load 1
  jmp loop
exit:
  write 2
  halt
