; input : X0 - string X zero-ended, X in {1,2,3,....}*, on the input tape
; output: XR - reverse of X

<input> 1 2 3 3 2 1 2 3  5 3 4 5 53 34 2  34

load=10
store2

read_next:
  read1
  load 1
  jz print
  store *2
  load 2
  add=1
  store2
  jmp read_next
print:
  load 2
  sub=1
  store 2
  sub =9
  jz exit
  write *2
  jmp print
exit:
  halt