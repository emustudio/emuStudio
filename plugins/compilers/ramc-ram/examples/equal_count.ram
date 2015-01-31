; input: X0 - string X zero-ended, X in {1,2,3,....}*, on the input tape
; output: 1  if N1(X) = N2(X) (i.e. if X contains equal number of "1" and "2")
;         0  otherwise

<input> 1 2 3 3 2 1 1 33 21 1 2 1 2 112 2 1 2 11 2 1 2 21 2 1

load=0
store 2
read_next:
  read 1
  load 1
  jz final_test
  sub=1
  jz increment
  load1
  sub=2
  jz decrement
  jmp read_next
increment:
  load2
  add=1
  store2
  jmp read_next
decrement:
  load 2
  sub=1
  store2
  jmp read_next
final_test:
  load2
  jz print_one
  write=0
  halt
print_one:
  write=1
  halt