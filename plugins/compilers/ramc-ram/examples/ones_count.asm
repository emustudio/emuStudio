; input : X0 - string X zero-ended, X in {1,2,3,....}*, on the input tape
; output: N1(X) - number of ones in X.
<input> 1 2 3 4 5 6 7 1 1 1 2 5 0

load =0
store 2

read_next: 
 read 1
 load 1
 jz print

 sub =1
 jz increment

 jmp read_next

increment:
 load 2
 add =1
 store 2
 jmp read_next

print:
 write 2
 halt
