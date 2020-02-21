; Z80 5 seconds Timer for 10 MHz
ORG 0000H
  LD SP,0FFFFH
MAIN:
  CALL TIMER5
  halt

TIMER5:   LD E, 35H
J60:      LD B, 0FFH
J61:      LD D, 0FFH
J62:      DEC D
          JP NZ,J62
          DEC B
          JP NZ,J61
          DEC E
          JP NZ,J60
          RET