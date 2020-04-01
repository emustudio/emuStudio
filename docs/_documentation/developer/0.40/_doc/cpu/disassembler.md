---
layout: default
title: Disassembler
nav_order: 4
parent: Writing a CPU
permalink: /cpu/disassembler
---

# Disassembler

Disassembler is not needed for the emulation itself. It is needed for emuStudio to be able to visually show the instructions. You can develop your own disassembler by implementing interface [Disassembler][disassembler]{:target="_blank"} from emuLib. Or you can use [Edigen][edigen]{:target="_blank"}, a disassembler generator for emuStudio. 

[Edigen][edigen]{:target="_blank"} works similarly as parser generator: developer writes a specification file. Then, [Edigen][edigen]{:target="_blank"} (either from the command line or using [Gradle][edigen-gradle]{:target="_blank"}) generates disassembler and decoder of the source code, using predefined templates, bundled in [Edigen][edigen]{:target="_blank"}.

Specification files have `.eds` file extension. A [SSEM][ssem]{:target="_blank"} CPU specification file looks as follows:

```
instruction = "JMP": line(5)     ignore8(8) 000 ignore16(16) |
              "JPR": line(5)     ignore8(8) 100 ignore16(16) |
              "LDN": line(5)     ignore8(8) 010 ignore16(16) |
              "STO": line(5)     ignore8(8) 110 ignore16(16) |
              "SUB": line(5)     ignore8(8) 001 ignore16(16) |
              "CMP": 00000       ignore8(8) 011 ignore16(16) |
              "STP": 00000       ignore8(8) 111 ignore16(16);

line = arg: arg(5);

ignore8 = arg: arg(8);

ignore16 = arg: arg(16);

%%

"%s %d" = instruction line(shift_left, shift_left, shift_left, bit_reverse, absolute) ignore8 ignore16;
"%s" = instruction ignore8 ignore16;
```

The specification file might look a bit cryptic at first sight, but it's quite easy. The content is divided into two sections, separated with two `%%` chars on a separate line. The first section contains rules which are used for parsing the instruction binary codes and assign labels to the codes. The second section specifies the disassembled string formats for particular rules.

There can exist multiple rules, and rules can include another rules. If the rule includes the same rule recursively, it means it's a constant. In that case, in the parenthesis after the rule inclusion must be a number of bits which the constant takes.

[edigen]: https://github.com/emustudio/edigen
[edigen-gradle]: https://github.com/emustudio/edigen-gradle-plugin
[ssem]: https://en.wikipedia.org/wiki/Manchester_Baby
[disassembler]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/Disassembler.html
