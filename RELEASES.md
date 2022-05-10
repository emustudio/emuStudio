# Version 0.40

All issues for the milestone are listed [here](https://github.com/emustudio/emuStudio/milestone/3?closed=1).

## Whole project:
- updated to Java 11
- changed build system - moved from Maven to Gradle
- reorganized directory structure, introduced startup scripts
- reimplemented generating of names of configuration files (to not clash with filesystem rules)
- changed configuration file format to [TOML](https://github.com/toml-lang/toml)
- many GUI fixes (e.g. `ESC` closes all dialogs, source code tab displays file name, dialog modality issues, added icon to windows)
- all dialogs accepting memory addresses can use various number radixes
- rewritten project website

## main-module:
- removed configuration editor popup in the schema editor
- reimplemented source code editor using [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea)
- reimplemented emulation automation (`--input` is not required anymore, introduced more command line options)
- introduced configuration file for the main module
- be able to configure Look and Feel in the configuration
- make debug table responsive
- allow to change font size in the editor
- fix: arrows in schema panel are wrongly displayed, but they work in schema editor
- fix: when computer is not possible to open due some error, no error dialog shows up
- fix: editing a just opened file does not detect changes when opening another file (and doesn't ask for saving)
- fix: find/replace - replace all does not work
- fix: plugin settings are not saved
- fix: timed emulation cannot be paused
- fix: Windows: instruction table has not a monospace font but some serif (also 88-sio status)
- fix: Windows: toolbar buttons are not highlighted when mouse is over (works on standard memory)
- fix: Windows: ESC won't clear marked occurences after text search in editor
- fix: Windows: in "View computer" dialog copyright doesn't show correct "č" character - encoding problem.
- fix: In about dialog, the logo panel has different background than the logo itself
- fix: Saving schema in schema editor removes all plugin settings
- fix: Opening schema in editor them immediately save it - the name disappears in the list of computers
- fix: When running emulation, page control panel won't disappear
- fix: Open computer dialog: ENTER should open computer
- fix: Save schema image does not work correctly
- fix: When compilation fails, and then source errors are fixed, compiler still reports failure
- fix: If invalid config file is put to config/, emuStudio wont start
- fix: after saving newly created computer in schema editor, it is not shown in the list

## RASP:
- rewritten raspc-rasp grammar and compiler
- raspc-rasp: implemented `<input>` directive

## SSEM:
- fix: SSEM noodle-timer doesn't work

## adm3A-terminal:
- fix: load cursor from software
- fix: keyboard does not read input
- fix: "here is" does not work (in GUI) - throws some hidden exception
- fix: "always on top" doesn't work

## 88-disk:
- implement reading files from CP/M filesystems
- fix: settings show weird unparseable ports in text fields (e.g. `Optional[8]`, etc.)

## 88-sio:
- fix: On computer reset, 88-SIO buffer is not cleared; on automatic emulation (non-interactive), reset should not clear the buffer
- fix: Removed port is not saved

## abstract-tape:
- renamed from abstractTape-ram
- fix: vertical resize of abstract tape won't extend the symbols list
- fix: In schema editor, abstractTape-ram is shown as "abstractTape" only
- fix: Input tape (copy.ram) does not show all inputs properly (row 04 is missing)

## standard-mem:
- fix: selected value is white and has white background (invisible)
- fix: Could not open memory image from "all files (.)" filter
- fix: with banking: no memory content is shown in the table (e.g. loaded image)
- fix: in case of banked memory, during load image first ask about memory location, then bank index
- fix: Find sequence dialog does not search by pressing ENTER

## 8080-cpu:
- fix: CPU is in Stopped state initially. After clicking on "run" it won't and hang whole emuStudio

# Version 0.39

All issues for the milestone are listed [here](https://github.com/emustudio/emuStudio/milestone/2?closed=1).

## New computers:
- RASP (raspc-rasp, rasp-cpu, rasp-mem) - thanks to Michal Šipoš
- SSEM (as-ssem, ssem-cpu, ssem-mem, ssem-display)
- re-implemented BrainDuck 

## Whole project:
- introduced logging using logback+slf4j
- introduced Maven 3 for dependency and project management
- new user documentation was created
- new developer's guide was created
- new website was created
- emulation automation was fixed for all machines (MITS Altair, RAM, RASP, BrainDuck, SSEM)
- lots of refactorings
- introduction of Travis CI

## main-module:
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

## as-8080:
- fixed `db 'if'`
- added command line
- fixed compiling `ani` instruction
- better error recovery implementation
- fixed: relative path to include files did not work

## as-z80:
- fixed `db 'if'` does not work
- added command line
- better error recovery implementation
- fixed: relative path to include files did not work

## 8080-cpu:
- introduced real-time program dumping
- refactorted + thoroughly tested
- fixed: PC change did not update the status panel GUI
- fixed decoder and disassembler (bug in edigen)
- fixed: `inc` and `dcr` instructions

## z80-cpu:
- introduced real-time program dumping
- refactorted + thoroughly tested
- fixed: PC change did not update the status panel GUI
- fixed: z80 JR xx,label incorrectly calculated offset
- refactored GUI

## 88-disk:
- introduced Java NIO to improve performance

## 88-sio: 
- fixed: status port manipulation
- add ability to bind to multiple CPU ports
- fixed: NPE in MITS 88-SIO

## standard-mem:
- add possibility to load image at runtime with a button
- add possibility to find a sequence

## adm3A-terminal:
- fixed cursor blinking
- fixed: terminal did not accept CTRL+C 
- fixed: terminal could load custom font

## ramc-ram
- added command line
- better error recovery implementation

## ram-cpu:
- IP change does not update the status panel GUI
- Introduce 'label' column in debug table

## ram-mem
- problem with loading programs

## abstractTape-ram
- canceling the edit of the symbol in the abstract tape does not work
- display a number of registers it's pointing at

## brainc-brainduck
- added command line
- reimplemented to real brainfuck language

## brainduck-cpu:
- fixed: when BrainDuck CPU was reset, it did not clear memory
- introduced profiler for improving performance
 
## brainduck-terminal:
- reimplemented


# Version 0.38b

All issues for the milestone are listed [here](https://github.com/emustudio/emuStudio/milestone/1?closed=1).


-  Add possibility to modify settings of plug-ins in abstract schema
   editor by double-clicking on an element or with using pop-up menu
   with right-click on an element.

-  Fixed several bugs in abstract schema drawing

-  Added pop-up menu when user right clicks on an element in abstract schema
-  Add ability to resize elements in abstract schema
-  Add useGrid, gridGap and width/height of all elements into schema and configuration file

-  Fixed debug table column title: mnemo to opcode
-  Created listener for possibility to call updates on debug table by any plugins.

-  Fixed bug: when deleted last computer in computers dialog, exception was thrown
-  Changed Vector to ArrayList in HighlightThread

-  Fixed bug in text editor: file saving

-  Fixed bug in 'view of computer' - devices were shown weirdly

-  Fixed synchronization problems in automatized emulation execution
-  Added new parameter "--nogui" that won't show GUI in the automatization

-  Fixed automatization, now "auto" setting is set to "true" if the emulation is automatized.
   Plugins may read this setting to determine this.

-  Fixed possible NullPointerExceptions throws when disassembler is not implemented inside a CPU

-  Re-implement pagination in the debug table

-  Next try to overcome thread deadlock bug connected with syntax highlighting :(

-  Make all rows in debug table visible
