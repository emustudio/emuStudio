---
layout: default
title: Device 88-sio
nav_order: 7
parent: MITS Altair8800
permalink: /altair8800/88-sio
---

# Serial board "88-sio"

Altair 8800 computer was equipped with serial board called [88-SIO][sio]{:target="_blank"}, or 88-2 SIO. It was a device which allowed connecting other devices using RS-232 interface. From one side it was attached to CPU on at least two ports (most commonly 0x10 and 0x11). The other side was ended with one or two physical ports (allowing to connect one or two devices). Real board supported both hardware and software interrupts.

The following image shows MITS 88-SIO-2 board.

![Serial board MITS 88-SIO-2]({{ site.baseurl }}/assets/altair8800/88-sio-2.png)

Original manual of MITS 88-SIO serial board can be downloaded at [this link][manual]{:target="_blank"}.

## Features

The plugin emulates only basic functionality of the board. It has the following features:

- allows to connect one device only
- CPU ports can be set manually
- setting of transfer speed, parity, number of stop bits is not supported
- GUI

Interrupts are not supported yet.

## CPU Ports settings

MITS 88-SIO board is attached to CPU using multiple ports. By default, the used CPU ports are:

- Status port: `0x03`, `0x10`, `0x14`, `0x16`, `0x18` (preferred: `0x10`)
- Data port: `0x02`, `0x11`, `0x15`, `0x17`, `0x19` (preferred: `0x11`)

The reason why there are multiple "bindings" is that there existed various software which expected specific bindings. The presented default values are the most common ones.

These numbers can be changed in the Settings window:

![Settings window]({{ site.baseurl }}/assets/altair8800/88-sio-ports.png)

- *A*: Attach Status SIO port to some new CPU port. The CPU port must be unique among both Status and Data ports attachments.
- *B*: Detach Status SIO port from selected CPU port.
- *C*: Attach Data SIO port to some new CPU port. The CPU port must be unique among both Status and Data ports attachments.
- *D*: Detach Data SIO port from selected CPU port.
- *E*: List of CPU ports to which the Status SIO port is attached
- *F*: Clear the current attachements of the Status SIO port and attach it to default CPU ports
- *G*: List of CPU ports to which the Data SIO port is attached
- *H*: Clear the current attachements of the Data SIO port and attach it to default CPU ports
- *I*: When selected, clicking on `OK` button will save the settings and will be applied at next emuStudio start.

## Connecting devices

MITS 88-SIO board as emuStudio plugin is a device which does nothing really useful. It just listens (and understands) commands coming from CPU through the I/O ports. The command is either a request for reading or request for writing to the attached device.

Theoretically any device which supports the basic I/O (reading/writing), can be attached to the board. More about plugin internals can be found in programmer's manual of emuStudio, which is not part of the user documentation.

Usually, attached devices were:

- serial terminal
- line printer
- paper tape reader/punch

In current implementation of Altair 8800 emulator, the only suitable device which can be attached to the board is terminal ADM-3A from Lear Siegler, Inc and which is described in its own section.

## Configuration file

The following table shows all the possible settings of MITS 88-SIO plugin:

|---
|Name                | Default value | Valid values                          | Description
|-|-|-|-
|`statusPortNumberX` | `0x10`        | > 0 and < 256; X range from 0 upwards | X-th Number of Status Port
|`dataPortNumberX`   | `0x11`        | > 0 and < 256; X range from 0 upwards | X-th Number of Data Port
|---

As can be seen; the `X` represents a number, it's a way how two SIO ports can be attached to multiple CPU ports.

## Programming

In order to show something useful, let's assume that a terminal LSI ADM-3A is attached to the board. Remember, the board only mediates the communication, it does not interpret any of the sent/received characters.

### CPU Ports

Whole communication between the board (and attached device) and CPU is controlled by programming the two ports: Status port and Data port. The following table shows the ports and how they are used.

|---
|Port     | Address | Input                      | Output
|-|-|-|-
|1        | `0x10`  | Read board status          | Not used. Originally used for enabling/disabling interrupts.
|2        | `0x11`  | Read data                  | Write data
|---

Now, detailed description of the ports follow. Bits are ordered in a byte as follows:

    D7 D6 D5 D4 D3 D2 D1 D0

where `D7` is the most significant bit, and `D0` the least significant bit.

### Port 1 ("Control" port)

Default addresses: `0x03`, `0x10`, `0x14`, `0x16`, `0x18` (preferred is `0x10`)

*WRITE*:

Controls input/output interrupts enable. If both interrupts are set to be enabled, it only empties transmitter buffer in the device, which was a post-step after interrupts being enabled. However, the plugin does not implement interrupts support.

- `D7 D6 D5 D4 D3 D2` : unused bits
- `D1 D0`             : Used for enabling/disabling interrupts. Not used in emuStudio.

*READ*:

Read status of the device.

- `D7` : _Output device ready_. Always 0 in the emulator.
- `D6` : Not used (always 0).
- `D5` : _Data available (for writing to the attached device)_. Always 0 in the emulator, meaning that no data is pending to be written. Data are written immediately after `OUT` instruction.
- `D4` : _Data overflow_. Value 1 means a new word of data has been received before the previous word was inputted to the accumulator. In emuStudio, this never happens.
- `D3` : _Framing error_. Value 1 means that data bit has no valid stop bit. In emuStudio, this never happens.
- `D2` : _Parity error_. Value 1 means that received parity does not agree with selected parity. In emuStudio, this never happens.
- `D1` : _Transmitter buffer empty_. Value 1 means that the data word has been received from the attached device and it's available for reading (from the Data port).
- `D0` : _Input device ready_. Value 1 means that the CPU can write data to the SIO (that the board is ready). Always 1 in the emulator.

### Port 2 ("Data" port)

Default addresses: `0x02`, `0x11`, `0x15`, `0x17`, `0x19` (preferred is `0x11`)

*WRITE*:

Write data to the attached device.

*READ*:

Read data from the attached device.

If the attached device sends asynchronously multiple data, the emulated board stores all in a buffer (queue) with unlimited capacity, so no data should be lost and can be read anytime.

### Program example

In this section it will be shown a small "How to" program terminal using 88-SIO ports.

#### Print a character on screen

In emuStudio, it is enough to write data to Port 2, e.g.:

```
mvi a, 'H'
out 11h
mvi a, 'i'
out 11h
```

#### Print a string on screen

For writing strings, it is more practical to have a procedure.

```
lxi h, text  ; load address of 'text' label to HL
call print   ; print text
hlt          ; halt CPU

text: db 'Hello, world!',0

; Procedure for printing text to terminal.
; Input: pair HL must contain the address of the ASCIIZ string
print:
    mov a, m  ; load character from HL
    inx h     ; increment HL
    cpi 0     ; is the character = 0?
    rz        ; yes; quit
    out 11h   ; otherwise; show it
    jmp print ; and repeat from the beginning
```

#### Reading character from keyboard

For reading a character, it is required to read the Port 1 until the character is not ready. Then we can read it from Port 2.

```
; Procedure will read a single character from terminal
; Input: none
; Output: register A will contain the character.
getchar:
    in 10h     ; read Port 1
    ani 1      ; is data ready ?
    jz getchar ; not; try again
    in 11h     ; yes; read it (into A register)
    ret
```

#### Reading text from keyboard

Now follows an example, which will read a whole line of characters into memory starting at address in `DE` pair. The procedure will interpret some control keys, like: backspace and ENTER keys.

```
lxi h, text        ; load address of 'text' label to HL
xchg               ; DE <-> HL
call getline       ; read line from the keyboard into DE

lxi h, text        ; load 'text' address again
call print         ; print the text on screen

hlt                ; halt CPU

text: ds 30        ; here will be stored the read text

;Procedure for reading a text from keyboard.
;Input: DE = address, where the text should be put after reading
;       C  = is used internally
getline:
    mvi c, 0       ; register C will be used as a counter of
                   ; read characters
next_char:
    in 10h         ; read Port 1: status
    ani 1          ; is the char ready for reading?
    jz next_char   ; not; try again
    in 11h         ; yes; read it to A register

    ; now ENTER and Backspace will be interpreted
    cpi 13         ; ENTER?
    jz getline_ret ; yes; it means end of input
    cpi 8          ; Backspace ?
    jnz save_char  ; if not; store the character

    ; Backspace interpretation
    mov a, c       ; A <- number of read characters
    cpi 0          ; are we at the beginning?
    jz next_char   ; yes; ignore the backspace

    dcx d          ; not; decrement DE
    dcr c          ; decrement count of read characters
    mvi a,8        ; "show" the backspace (terminal will
                   ; interpret this by moving the cursor
                   ; to the left by 1 char)
    out 11h
    mvi a, 32      ; "clear" the current character on screen
                   ; by a space character (ASCII code 32)
    out 11h

    mvi a,8        ; and move the cursor back again
    out 11h
    jmp next_char  ; jump to next char

save_char:         ; stores a character into memory at DE
    out 11h        ; show the character in A register
    stax d         ; store it at address DE
    inx d          ; increment DE
    inr c          ; increment number of read characters
    jmp next_char  ; jump to next char

getline_ret:       ; end of input
                   ; ENTER will be stored as CRLF
    mvi a,13       ; CR (Carriage Return)
    stax d         ; store the char
    inx d          ; increment DE
    mvi a, 10      ; LF (Line Feed)
    stax d         ; store the char
    inx d          ; increment DE
    mvi a, 0       ; char 0 (End-Of-Input)
    stax d         ; store the char
    ret            ; return
```

[sio]: http://www.s100computers.com/Hardware%20Folder/MITS/SIO-B/SIO.htm
[manual]: http://maben.homeip.net/static/s100/altair/cards/Altair%2088-SIO%20serial%20IO.pdf
