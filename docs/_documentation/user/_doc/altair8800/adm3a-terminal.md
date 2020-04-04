---
layout: default
title: Device adm3a-terminal
nav_order: 8
parent: MITS Altair8800
permalink: /altair8800/adm3a-terminal
---

# Terminal LSI ADM-3A

Emulation of famous terminal from Lear Siegler, Inc. - ADM-3A. It had a nick name 'Dumb Terminal'. In the time (1974), due to its cheapness and speed capabilities required in that time, it became de facto standard in the industry. Often it was used in connection with MITS Altair 8800 computer, so the decision of which terminal to emulate was clear.

Maintenance manual can be downloaded at [this link][manual1]{:target="_blank"}, operator's manual [here][manual2]{:target="_blank"}.

## Display

Terminal could display 128 ASCII characters (upper-case and lower-case letters, punctuation and numbers). Original ADM-3 could display only 64 (only capital-letters and some other). For saving very expensive RAM the terminal offered size 12 rows x 80 columns, with optional extension to 24 rows x 80 columns. The size used in the emulator is hardcoded to 80 columns x 24 rows.

Besides, the emulator uses custom font colored green, with anti-aliasing support and double-buffering.

## Keyboard

Terminal could generate always 128 ASCII characters (upper-case, lower-case, punctuation and numbers). Besides, it could generate special control characters which had effect on the current cursor position and were not sent to CPU.

Emulated device allows to generate almost anything what your host keyboard can give. It is only up to font which characters it can display. The font cannot display any special non-US characters used in various languages. Just classic ASCII.

Besides, the terminal can capture control codes (holding `CTRL` plus some key), and special control codes (`ESC + '=' + [X] + [Y]`, sets new cursor position, where `[X]` is a key translated to X position and `[Y]` a key translated into Y position of a cursor). The following subsection lists all possible control and special control key combinations.

### Control codes

The following table shows control codes (`CTRL` plus some key combinations). The table can be found in original manuals. The emulator is following it.

|---
| Code     | ASCII mnemonic | Function in ADM-3A
|-|-|-
|`CTRL+@`  | `NUL`   |
|`CTRL+A`  | `SOH`   |
|`CTRL+B`  | `STX`   |
|`CTRL+C`  | `ETX`   |
|`CTRL+D`  | `EOT`   |
|`CTRL+E`  | `ENQ`   | Initiates ID message with automatic "Answer Back" option.
|`CTRL+F`  | `ACK`   |
|`CTRL+G`  | `BEL`   | Sounds audible beep in ADM-3A (not in emulator yet :( )
|`CTRL+H`  | `BS`    | Backspace
|`CTRL+I`  | `HT`    |
|`CTRL+J`  | `LF`    | Line feed
|`CTRL+K`  | `VT`    | Upline
|`CTRL+L`  | `FF`    | Forward space
|`CTRL+M`  | `CR`    | Return
|`CTRL+N`  | `SO`    | Unlock keyboard
|`CTRL+O`  | `SI`    | Lock keyboard
|`CTRL+P`  | `OLE`   |
|`CTRL+Q`  | `DCI`   |
|`CTRL+R`  | `DC2`   |
|`CTRL+S`  | `DC3`   |
|`CTRL+T`  | `DC4`   |
|`CTRL+U`  | `NAK`   |
|`CTRL+V`  | `SYN`   |
|`CTRL+W`  | `ETB`   |
|`CTRL+X`  | `CAN`   |
|`CTRL+Y`  | `EM`    |
|`CTRL+Z`  | `SUB`   | Clear screen
|`CTRL+[`  | `ESC`   | Initiate load cursor
|`CTRL+x`  | `FS`    |
|`CTRL+]`  | `GS`    |
|`CTRL+^`  | `RS`    | Home cursor
|---

### Absolute cursor position from the keyboard

Terminal allows to set the absolute cursor position, when in "Cursor control Mode". ADM-3A emulator does not have such mode, but `ESC + '=' + [X] + [Y]` combination allows to set the cursor position. By pressing `ESC` key, it initiates the "load cursor" operation. If the user then presses `=` key, the terminal expects another two keystrokes, and translates them into `[X]` and `[Y]` coordinates for the new position of the cursor. The following table shows the key-to-coordinate translation table.

|---
| Key  | Number
|-|-
|`' '` | 0
|`!`   | 1
|`"`   | 2
|`#`   | 3
|`$`   | 4
|`%`   | 5
|`&`   | 6
|`'`   | 7
|`(`   | 8
|`)`   | 9
|`*`   | 10
|`+`   | 11
|`,`   | 12
|`-`   | 13
|`.`   | 14
|`/`   | 15
|`0`   | 16
|`1`   | 17
|`2`   | 18
|`3`   | 19
|`4`   | 20
|`5`   | 21
|`6`   | 22
|`7`   | 23
|`8`   | 24
|`9`   | 25
|`:`   | 26
|`;`   | 27
|`<`   | 28
|`=`   | 29
|`>`   | 30
|`?`   | 31
|`@`   | 32
|`A`   | 33
|`B`   | 34
|`C`   | 35
|`D`   | 36
|`E`   | 37
|`F`   | 38
|`G`   | 39
|`H`   | 40
|`I`   | 41
|`J`   | 42
|`K`   | 43
|`L`   | 44
|`M`   | 45
|`N`   | 46
|`O`   | 47
|`P`   | 48
|`Q`   | 49
|`R`   | 50
|`S`   | 51
|`T`   | 52
|`U`   | 53
|`V`   | 54
|`W`   | 55
|`X`   | 56
|`Y`   | 57
|`Z`   | 58
|`[`   | 59
|`\`   | 60
|`]`   | 61
|`^`   | 62
|`_`   | 63
|```   | 64
|`a`   | 65
|`b`   | 66
|`c`   | 67
|`d`   | 68
|`e`   | 69
|`f`   | 70
|`g`   | 71
|`h`   | 72
|`i`   | 73
|`j`   | 74
|`k`   | 75
|`l`   | 76
|`m`   | 77
|`n`   | 78
|`o`   | 79
|---

## ADM-3A Settings

It is possible to configure the terminal either from GUI or manually modifying configuration settings. Modification of settings requires restarting emuStudio.

The "settings" window is shown in the following image:

![Settings window of ADM-3A terminal]({{ site.baseurl }}/assets/altair8800/adm3a-settings.png)

- *A*: File for reading input (when redirected)
- *B*: File for writing output (when redirected)
- *C*: In automatic mode, how long the terminal should wait until it reads next input character from the file
       (in milliseconds)
- *D*: Whether every keystroke will also cause to display it. Programs don't always "echo" the characters back
       to the screen.
- *E*: Whether terminal GUI should be always-on-top of other windows
- *F*: Whether the display should use anti-aliasing.
- *G*: Clears the screen.
- *H*: Rolls the screen down by 1 line
- *I*: If checked, then by pressing OK the settings will be saved to the configuration file. If not, they will be not
       saved. In any case, the effect of the settings will be visible immediately.

Terminal behaves differently when emuStudio is run in automatic (or "no GUI") mode. In that moment, input is redirected to be read from a file, and also output is redirected to be written to another file. File names are configurable in the computer config file. Using redirection in GUI mode is currently not possible.

## Configuration file

The following table shows all the possible settings of ADM-3A plugin:

|---
|Name              | Default value        | Valid values          | Description
|-|-|-|-
|`inputFileName`   | `adm3A-terminal.in`  | Path to existing file | File for reading input (when redirected)
|`outputFileName`  | `adm3A-terminal.out` | Path to existing file | File for writing output (when redirected)
|`inputReadDelay`  | 0                    | > 0                   | How long the terminal should wait until it reads next input character from the file (in milliseconds)
|`antiAliasing`    | false                | true / false          | Whether the display should use anti-aliasing.
|`halfDuplex`      | false                | true / false          | Whether every keystroke will also cause to display it.
|---


[manual1]: http://www.mirrorservice.org/sites/www.bitsavers.org/pdf/learSiegler/ADM3A_Maint.pdf
[manual2]: http://maben.homeip.net/static/s100/learSiegler/terminal/Lear%20Siegler%20ADM3A%20operators%20manual.pdf
