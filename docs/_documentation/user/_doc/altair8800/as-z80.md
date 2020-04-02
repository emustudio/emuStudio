---
layout: default
title: Assembler as-z80
nav_order: 2
parent: MITS Altair8800
permalink: /altair8800/as-z80
---

# Assembler "as-z80"

The assembler syntax is inspired by `as-8080` assembler, and by instruction set described [here][asz80]{:target="_blank"}. The assembler supports the following features:

- macro support (unlimited nesting)
- include other files support
- conditional assembly
- data definition
- relative addressing using labels
- literals and expressions in various radixes (bin, dec, hex, oct)
- output is in [Intel HEX][intelhex]{:target="_blank"} format

## Running from command line

The assembler is provided as part of emuStudio, and usually it is run from GUI. But it can be run also from the command line, as follows:


- on Linux:
```
> bin/as-z80 [--output output_file.hex] [source_file.asm]
```

- on Windows:
```
> bin\as-z80.bat [--output output_file.hex] [source_file.asm]
```

All command line options include:

```
Options:
	--output, -o	file: name of the output file
	--version, -v	: print version
	--help, -h	: this help
```

## Lexical symbols

The assembler does not differentiate between upper and lower case (it is case-insensitive). The token/symbol types are as follows:



|---
|Type      | Description
|-|-
| Keywords  | instruction names; preprocessor directives (`org`, `equ`, `var`, `macro`, `endm`, `include`, `if`, `endif`); data definitions (`db`, `dw`, `ds`); CPU registers
| Identifiers | `([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*` except keywords
| Labels      |
| Constants   | strings or integers
| Operators   | `+`, `-`, `*`, `/`, `=`, `%`, `&`, `\|`, `!`, `~`, `<<`, `>>`, `>`, `<`, `>=`, `<=`
| Comments    | semi-colon (`;`) with text after it until the end of the line
|---

### Constants

Numeric constants can be only integers, encoded with one of several number radixes. The possible formats are written using regexes:

- binary numbers: `[0-1]+[bB]`
- decimal numbers: `[0-9]+[dD]?`
- octal numbers: `[0-7]+[oOqQ]`
- hexadecimal numbers: `[0-9][0-9a-fA-F]*[hH]`

Characters or strings must be enclosed in double-quotes, e,g,: `LD E, "*"`

### Identifiers

Identifiers must fit to the following regex: `([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*`. It means, that it has to start with a letter a-z (or A-Z) or the at-sign (`@`). Then, it can be followed by letters, at-sign, or numbers.

However, they must not equal to any keyword.

## Instructions syntax

The program is basically a sequence of instructions. The instructions are separated by a new line. The instruction have optional and mandatory parts, e.g.:

    LABEL: CODE OPERANDS ; COMMENT


|---
| Part | Required | Notes
|-|-|-
|`LABEL`    | Optional   | Identifier of the memory position, followed by a colon (`:`). It can be used as forward or backward reference in instructions which expect memory address (or 16 bit number).
|`CODE`     | Mandatory  | Instruction name.
|`OPERANDS` | It depends | If applicable, a comma-separated (`,`) operands of the instruction.
|`COMMENT`  | Optional   | semi-colonm (`;`) followed by any text until the end of the line.
|---

Fields `CODE` and `OPERANDS` must be separated by at least one space. For example:

    HERE:   LD C, 0  ; Put 0 into C register
            DB 3Ah   ; Data constant of size 1 byte
    LOOP:   JP LOOP  ; Infinite loop


Labels are optional. Instructions and pseudo-instructions and register names are reserved for assembler and cannot be used as labels. Also, there cannot be more definitions of the same label.

Operands must be separated with comma (`,`). There exist several operand types, which represent so-called "address modes". Allowed address modes depend on the instruction. The possibilities are:

- Implicit addressing: instructions do not have operands. They are implicit.
- Register addressing: operands are registers. 8-bit general-purpose register names are: `A`, `B`, `C`, `D`, `E`, `H`, `L`. Register pairs have names: `BC`, `DE`, `HL`. Stack pointer is defined as `SP`, and program status word (used by `push` / `pop` instructions) as `AF`. Another 16-bit registers are defined as `IX`, `IY`.
- Register indirect addressing: for example, loading a memory value at address in `HL` pair: `LD A, (HL)`.
- Immediate addressing: operand is the 8-bit constant. It can be also one character, enclosed in double-quotes.
- Direct addressing: operand is either 8-bit or 16-bit constant, which is understood as the memory location (address). For example: `LD (1234h), HL`.

Immediate data or addresses can be defined in various ways:

- Integer constant
- Integer constant as a result of evaluation of some expression (e.g. `2 << 4`, or `2 + 2`)
- Current address - denoted by special variable `$`. For example, instruction `JP $+6` denotes a jump by 6-bytes further from the current address.
- Character constants, enclosed in double-quotes (e.g. `LD A, "*"`)
- Labels. For example: `JP THERE` will jump to the label `THERE`.
- Variables. For example:

```
    VALUE VAR 'A'
    LD A, VALUE
```

## Expressions

An expression is a combination of the data constants and operators. Expressions are evaluated in compile-time. Given any two expressions, they must not be defined in circular way.
Expressions can be used anywhere a constant is expected.

There exist several operators, such as:


|---
| Expression | Notes
|-|-
|`+`  | Addition. Example: `DB 2 + 2`; evaluates to `DB 4`
|`-`  | Subtraction. Example: `DW $ - 2`; evaluates to the current compilation address minus 2.
|`*`  | Multiply.
|`/`  | Integer division.
|`=`  | Comparison for equality. Returns 1 if operands equal, 0 otherwise. Example: `DB 2 = 2`; evaluates to `DB 1`.
|`%`  | Remainder after integer division. Example `DB 4 mod 3`; evaluates to `DB 1`.
|`&`  | Logical and.
|`\|` | Logical or.
|`~`  | Logical xor.
|`!`  | Logical not.
|`<<` | Shift left by 1 bit. Example: `DB 1 SHL 3`; evaluates to `DB 8`
|`>>` | Shift right by 1 bit.
|`>`  | Greater than. Example: `DB 3 > 2`; evaluates to `DB 1`
|`<`  | Less than.
|`>=` | Greater or equal than.
|`<=` | Less or equal than.
|---

Operator priorities are as follows:


|---
|Priority | Operator    | Type
|-|-|-
| 1       | `( )`       | Unary
| 2       | `*`, `/`, `%`, `<<`, `>>`, `>`, `<`, `>=`, `<=` | Binary
| 3       | `+`, `-`    | Unary and binary
| 4       | `=`         | Binary
| 5       | `!`         | Unary
| 6       | `&`         | Binary
| 7       | `\|`, `~`   | Binary
|---

All operators work with its arguments as if they were 16-bit. Their results are always 16-bit numbers. If there is expected 8-bit number, the result is automatically "cut" using operation `result AND 0FFh`. This may be unwanted behavior and might lead to bugs, but it is often useful so the programmer must ensure the correctness.

## Defining data

Data can be defined using special pseudo-instructions. These accept constants. Negative integers are using [two's complement][twocompl]{:target="_blank"}.

The following table describes all possible data definition pseudo-instructions:


|---
| Expression | Notes
|-|-
| `DB [expression]`  | Define byte. The `[expression]` must be of size 1 byte. Using this pseudo-instruction, a string can be defined, enclosed in single quotes. For example: `DB 'Hello, world!'` is equal to `DB 'H'`, `DB 'e'`, etc. on separate lines.
| `DW [expression]`  | Define word. The `[expression]` must be max. of size 2 bytes. Data are stored using [little endian][littleendian]{:target="_blank"}.
| `DS [expression]`  | Define storage. The `[expression]` represents number of bytes which should be "reserved". The reserved space will not be modified in memory. It is similar to "skipping" particular number of bytes.
|---

### Examples

        HERE:  DB 0A3H          ; A3
        W0RD1: DB 5*2, 2FH-0AH  ; 0A25
        W0RD2: DB 5ABCH SHR 8   ; 5A
        STR:   DB "STRINGSpl"   ; 535452494E472031
        MINUS: DB -03H          ; FD

        ADD1: dw COMP          ; 1C3B  (assume COMP is 3B1CH)
        ADD2: dw FILL          ; B43E (assume FILL is 3EB4H)
        ADD3: dw 3C01H, 3CAEH  ; 013CAE3C

## Including other source files

It is both useful and good practice to write modular programs. According to the [DRY][dry]{:target="_blank"} principle the repetitive parts of the program should be refactored out into functions or modules. Functionally similar groups of these functions or modules can be put into a library, reusable in other programs.

The pseudo-instruction `include` exists for the purpose of including already written source code into the current program. The pseudo-instruction is defined as follows:

        INCLUDE "[filename]"

where `[filename]` is a relative or absolute path to the file which will be included, enclosed in double-quotes. The file can include other files, but there must not be defined circular includes (compiler will complain).

The current compilation address (denoted by `$` variable) after the include will be updated about the binary size of the included file.

The namespace of the current program and the included file is *shared*. It means that labels or variables with the same name in the current program and the included file are prohibited. Include file "sees" everything in the current program as it was its part.

### Example

Let `a.asm` contains:

        ld b, 80h

Let `b.asm` contains:

        include "a.asm"

Then compiling `b.asm` will result in:

        06 80     ; ld b, 80h

## Origin address

Syntax: `ORG [expression]`

Sets the value to the `$` variable. It means that from now on, the following instructions will be placed at the address given by the `[expression]`. Effectively, it is the same as using `DS` pseudo-instruction, but instead of defining number of skipped bytes, we define concrete memory location (address).

The following two code snippets are equal:


|---
| Address | Block 1       | Block 2       | Opcode
|-|-|-|-
| `2C00`  | `LD A,C`      | `LD A,C`      | `79`
| `2C01`  | `JP NEXT`     | `JP NEXT`     | `C3 10 2C`
| `2C04`  | `DS 12`       | `ORG $+12`    |
| `2C10`  | `NEXT: XOR A` | `NEXT: XOR A` | `AF`
|---

## Equate

Syntax: `[identifier] EQU [expression]`

Define a constant. The `[identifier]` is a mandatory name of the constant.

`[expression]` is the 16-bit expression.

The pseudo-instruction will define a constant - assign a name to given expression. The name of the constant then can be used anywhere where the constant is expected and the compiler will replace it with the expression.

It is not possible to redefine a constant.

## Variables

Syntax: `[identifier] VAR [expression]`

Define or re-define a variable. The `[identifier]` is a mandatory name of the constant. 

`[expression]` is the 16-bit expression.

The pseudo-instruction will define a variable - assign a name to given expression. Then, the name of the variable can be used anywhere where the constant is expected.

It is possible to redefine a variable, which effectively means to reassign new expression to the same name and forgetting the old one. The reassignment is aware of locality, i.e. before it the old value will be used, after it the new value will be used.

## Conditional assembly

Syntax:

        if [expression]
            i n s t r u c t i o n s
        endif

At first, the compiler evaluates the `[expression]`. If the result is 0, instructions between `if` and `endif` will be ignored. Otherwise they will be included in the source code.

## Defining and using macros

Syntax:

        [identifier] macro [operands]
            i n s t r u c t i o n s
        endm


The `[identifier]` is a mandatory name of the macro.

The `[operands]` part is a list of identifiers, separated by commas (`,`). Inside the macro, operands act as constants. If the macro does not use any operands, this part can be omitted.

The namespace of the operand identifiers is macro-local, ie. the operand names will not be visible outside the macro. Also, the operand names can hide variables, labels or constants defined in the outer scope.

The macros can be understood as "templates" which will be expanded in the place where they are "called". The call syntax is as follows:


        [macro name] [arguments]


where `[macro name]` is the macro name as defined above. Then, `[arguments]` are comma-separated expressions, in the order as the original operands are defined. The number of arguments must be the same as number of macro operands.

The macro can be defined anywhere in the program, even in some included file. Also, it does not matter in which place is called - above or below the macro definition.

### Examples

        SHV MACRO
        LOOP: RRCA        ; Right rotate with carry
              AND 7FH     ; Clear MSB of accumulator
              DEC D       ; Decrement rotation counter - register D
              JP NZ, LOOP ; Jump to next rotation
        ENDM

The macro `SHV` can be used as follows:

        LD A, (TEMP)
        LD D,3  ; 3 rotations
        SHV
        LD (TEMP), A

Or another definition:

        SHV MACRO AMT
              LD D,AMT   ; Number of rotations
        LOOP: RRCA
              AND 7FH
              DEC D
              JP NZ, LOOP
        ENDM

And usage:

        LD A, (TEMP)
        SHV 5
        LD (TEMP), A

Which has the same effect as the previous example.


[asz80]: http://www.z80.info/zip/z80cpu_um.pdf
[intelhex]: https://en.wikipedia.org/wiki/Intel_HEX
[twocompl]: https://en.wikipedia.org/wiki/Two's_complement
[littleendian]: https://en.wikipedia.org/wiki/Endianness#Little-endian
[dry]: https://en.wikipedia.org/wiki/Don't_repeat_yourself
