This example uses iterative definition of Fibonacci numbers. A high-level description of what it does is:
store two last numbers in variables c4 and c5 (initially c4=0, c5=1), print the number stored in c5 (this
operation takes the major part of the code), calculate next number (c6 = c5+c4), and move the numbers
sequence one number back (c4 = c5, c5 = c6). A low-level description is given in the comments, notation
“cXvY” meaning that after execution of the commands in the line the data pointer is at cell X, and the
value at this cell is Y. A total of 12 memory cells is used.

This example uses one minor cheat: classic Brainfuck interpreter uses byte variables to store values of
memory cells, so Fibonacci numbers 14 through 16 will cause overflow. Writing long arithmetics in
Brainfuck is a bit of overkill, so in this example we assume that memory cells can store integer values. 

Source: http://progopedia.com/version/mullers-brainfuck-2.0/

++++++++++++++++++++++++++++++++++++++++++++		c1v44 : ASCII code of comma
>++++++++++++++++++++++++++++++++			c2v32 : ASCII code of space
>++++++++++++++++					c3v11 : quantity of numbers to be calculated
>							c4v0  : zeroth Fibonacci number (will not be printed)
>+							c5v1  : first Fibonacci number
<<							c3    : loop counter
[							block : loop to print (i)th number and calculate next one
>>							c5    : the number to be printed

							block : divide c5 by 10 (preserve c5)
>							c6v0  : service zero
>++++++++++						c7v10 : divisor
<<							c5    : back to dividend
[->+>-[>+>>]>[+[-<+>]>+>>]<<<<<<]			c5v0  : divmod algo; results in 0 n d_n%d n%d n/d
>[<+>-]							c5    : move dividend back to c5 and clear c6
>[-]							c7v0  : clear c7

>>							block : c9 can have two digits; divide it by ten again
>++++++++++						c10v10: divisor
<							c9    : back to dividend
[->-[>+>>]>[+[-<+>]>+>>]<<<<<]				c9v0  : another divmod algo; results in 0 d_n%d n%d n/d
>[-]							c10v0 : clear c10
>>[++++++++++++++++++++++++++++++++++++++++++++++++.[-]]c12v0 : print nonzero n/d (first digit) and clear c12
<[++++++++++++++++++++++++++++++++++++++++++++++++.[-]] c11v0 : print nonzero n%d (second digit) and clear c11

<<<++++++++++++++++++++++++++++++++++++++++++++++++.[-]	c8v0  : print any n%d (last digit) and clear c8
<<<<<<<.>.                                              c1c2  : print comma and space
							block : actually calculate next Fibonacci in c6
>>[>>+<<-]						c4v0  : move c4 to c6 (don't need to preserve it)
>[>+<<+>-]						c5v0  : move c5 to c6 and c4 (need to preserve it)
>[<+>-]							c6v0  : move c6 with sum to c5
<<<-							c3    : decrement loop counter
]
<<++...							c1    : output three dots
