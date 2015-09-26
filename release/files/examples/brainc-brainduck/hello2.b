In this example we use three memory cells — first for uppercase letters ‘H’ and ‘W’, second for lowercase
letters and third for special characters ‘,’, ‘ ‘ and ‘!’ — and three index cells to shorten the notation
of ASCII-codes changes. The memory used looks like this:

(index cell 1) (uppercase letters cell) (index cell 2) (lowercase letters cell) (index cell 3) (special characters cell) 

Source: http://progopedia.com/version/mullers-brainfuck-2.0/

++++++[>++++++++++++<-]>.
>++++++++++[>++++++++++<-]>+.
+++++++.
.
+++.
>++++[>+++++++++++<-]>.
<+++[>----<-]>.
<<<<<+++[>+++++<-]>.
>>.
+++.
------.
--------.
>>+.