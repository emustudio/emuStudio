<input> 3 4 hello howdy

; COPY(X,Y)
;
; input:
;   reg.1: X
;   reg.2: Y
;
; output:
;   reg.X: (reg.Y)
;   reg.Y: (reg.Y)


<input> sss
; load X,Y
read 1
read 2

; load r.X, r.Y
read *1
read *2

; copy
load *2
store *1

halt