Release Notes for emuStudio version 0.39
(c) Copyright 2006-2017, Peter Jakubčo

# New computers:
- RASP (raspc-rasp, rasp-cpu, rasp-mem) - thanks to Michal Šipoš
- SSEM (as-ssem, ssem-cpu, ssem-mem, ssem-display)
- re-implemented BrainDuck 

# Whole project:
- introduced logging using logback+slf4j
- introduced Maven 3 for dependency and project management
- new user documentation was created
- new developer's guide was created
- new website was created
- emulation automation was fixed for all machines (MITS Altair, RAM, RASP, BrainDuck, SSEM)
- lots of refactorings
- introduction of Travis CI

# main-module:
- rewritten debugger and disassembler
- introduced pagination in debugger
- introduced saving computer schema to image
- better place location of splitter for editor / compiler output
- fixed GUI problems
- "previous instruction" just decrements program counter
- fixed syntax highlighting
- fixed: on breakpoint, instruction list was not pointing at current (next) instruction 
- fixed: on 'pages backwards' button when the input window is cancelled, NullPointerException appeared in log
- fixed: when file was not saved before compilation, on most compilers NullPointerException appeared in log
- fixed: in source code editor, when open file is cancelled, compiler output window was cleared
- find/replace dialog is now possible to be cancelled with ESC key
- schema editor: enable breaking connection lines with movable pins
- fixed undo/redo capability
- fixed: deadlock in source code editor

# as-8080:
- fixed `db 'if'`
- added command line
- fixed compiling `ani` instruction
- better error recovery implementation
- fixed: relative path to include files did not work

# as-z80:
- fixed `db 'if'` does not work
- added command line
- better error recovery implementation
- fixed: relative path to include files did not work

# 8080-cpu:
- introduced real-time program dumping
- refactorted + thoroughly tested
- fixed: PC change did not update the status panel GUI
- fixed decoder and disassembler (bug in edigen)
- fixed: `inc` and `dcr` instructions

# z80-cpu:
- introduced real-time program dumping
- refactorted + thoroughly tested
- fixed: PC change did not update the status panel GUI
- fixed: z80 JR xx,label incorrectly calculated offset
- refactored GUI

# 88-disk:
- introduced Java NIO to improve performance

# 88-sio: 
- fixed: status port manipulation
- add ability to bind to multiple CPU ports
- fixed: NPE in MITS 88-SIO

# standard-mem:
- add possibility to load image at runtime with a button
- add possibility to find a sequence

# adm3A-terminal:
- fixed cursor blinking
- fixed: terminal did not accept CTRL+C 
- fixed: terminal could load custom font

# ramc-ram
- added command line
- better error recovery implementation

# ram-cpu:
- IP change does not update the status panel GUI
- Introduce 'label' column in debug table

# ram-mem
- problem with loading programs

# abstractTape-ram
- canceling the edit of the symbol in the abstract tape does not work
- display a number of registers it's pointing at

# brainc-brainduck
- added command line
- reimplemented to real brainfuck language

# brainduck-cpu:
- fixed: when BrainDuck CPU was reset, it did not clear memory
- introduced profiler for improving performance
 
# brainduck-terminal:
- reimplemented
