# Open Issues — Clarification Plan

Generated: 2026-05-04. Covers all 39 open GitHub issues in the
[emustudio/emuStudio](https://github.com/emustudio/emuStudio) repository.

Each entry provides: **Context** (what exists today), **Motivation** (why it matters), and
**High-Level Steps** (concrete implementation plan).

---

## #168 — Report connection status when running emulation

**Context**  
emuStudio loads plugins (CPU, memory, compiler, devices) at runtime and wires them together as
described in a TOML config file. Currently there is no runtime verification step: if a device
doesn't find its expected I/O-port neighbour, or two devices claim the same port range, emulation
silently misbehaves. `VirtualComputer` starts plugins in order but never aggregates or logs which
connections each plugin actually established.

**Motivation**  
- Users building new virtual computer configs need immediate confirmation that wiring is correct.  
- Plugin developers need to verify port/memory range assignments at startup.  
- Silent misconfiguration (overlapping I/O ports, unresolved context references) wastes debugging time.

**High-Level Steps**  
1. Add an optional `ConnectionReport` interface (or default method on `Plugin`) in emuLib that
   returns a list of `ConnectionInfo` records (type: IO-port / memory / plugin-peer, range,
   direction, description).  
2. Implement `ConnectionReport` in existing device plugins (`88-sio`, `88-dcdd`, `simh-pseudo`,
   `zxspectrum-bus`, etc.) reporting their claimed port/memory ranges and connected plugin IDs.  
3. In `VirtualComputer.start()`, iterate all loaded plugins, query `ConnectionReport` if
   available, aggregate results.  
4. Log the collected info at `INFO` level via SLF4J so it appears in the log pane.  
5. Emit a `WARN` when two plugins report overlapping port or memory ranges.  
6. Optionally: add a "Connections" tab in the debugger view showing the connection table.

---

## #171 — standard-mem: Implement find next, find previous

**Context**  
The `byte-mem` hex editor can search for a byte pattern. Currently only a single "find" action
exists — after a match is highlighted there is no way to navigate to the next or previous
occurrence without re-opening the search dialog and re-entering the pattern.

**Motivation**  
When debugging programs that write repeated sentinel values or searching for a string literal in
RAM, users need to scroll through all occurrences quickly without repeated manual searches.

**High-Level Steps**  
1. Persist the last successful search pattern and the match address in the memory GUI state.  
2. Add "Find Next" (F3) and "Find Previous" (Shift+F3) toolbar actions in the hex editor.  
3. On "Find Next", scan forward from `lastMatchAddress + 1` wrapping at memory size; on "Find
   Previous" scan backward from `lastMatchAddress - 1` wrapping at 0.  
4. Highlight the new match and scroll the hex table to centre it.  
5. Show a status-bar message "Match N of M" or "No more matches" when the search wraps/exhausts.  
6. Clear persisted state when the search dialog opens with a new pattern.

---

## #178 — Create Windows & Linux installer

**Context**  
emuStudio is distributed as a plain ZIP. Users must manually install a JRE, unpack the archive,
and run a shell script. On Windows there is no start-menu entry; on Linux there is no
desktop-integration or package-manager entry.

**Motivation**  
- Lowers the barrier for non-developer users (retro-computing enthusiasts, educators).  
- Proper installers bundle a JRE and remove the "install Java first" friction.  
- Platform update mechanisms (Flathub) handle automatic updates.

**High-Level Steps**  
1. **Linux (Flatpak):** Write a `net.emustudio.emuStudio.yaml` manifest, bundle OpenJDK runtime,
   declare AppStream metadata, publish to Flathub.  
2. **Windows (EXE):** Use the Gradle `jpackage` task to produce a self-contained `.exe` installer
   with bundled JRE, start-menu shortcut, and `.toml` file-type association.  
3. Add Gradle tasks `packageLinux` / `packageWindows` producing respective artifacts in CI.  
4. Add a GitHub Actions release-workflow step to attach installer artifacts to the GitHub Release.  
5. Document installation steps in `README.md` and on the emuStudio website.

---

## #179 — Implement front panel of MITS Altair 8800

**Context**  
The MITS Altair 8800 is the flagship emulated computer in emuStudio. Its most iconic feature is a
physical front panel with 36 toggle switches and 36 LEDs. Currently emuStudio renders the Altair
8800 without this panel — users can only interact via the terminal.

**Motivation**  
- The front panel is the defining UX artifact of the Altair 8800.  
- Toggle-switch entry was the original programming interface before serial terminals existed.  
- A visual front panel makes emuStudio stand out for educational demonstrations.

**High-Level Steps**  
1. Create an `altair8800-frontpanel` device plugin (or add a GUI component to `88-sio`) with a
   Swing canvas rendering the front panel image.  
2. Model 36 toggle switches mapped to address bus (A0–A15), data bus (D0–D7), and control lines
   (EXAMINE, EXAMINE NEXT, DEPOSIT, DEPOSIT NEXT, RESET, RUN, STOP, STEP, …).  
3. Model 36 LEDs (address, data, status: INTE, PROT, MEMR, INP, MI, OUT, HLTA, STACK, WO, INT,
   WAIT, HLDA, RESET, HOLD) that update on every CPU cycle/step.  
4. Connect the panel to the CPU via emuLib context: RUN/STOP toggle calls `cpu.run()` /
   `cpu.pause()`; EXAMINE reads memory at address bus value; DEPOSIT writes data bus value.  
5. Use a timer-driven repaint (≥ 30 fps) to animate LED state during free-running emulation.  
6. Add configuration for panel image theme (original vs. stylised).

---

## #189 — Implement C compiler for 8080/Z80

**Context**  
emuStudio has assemblers for 8080 and Z80. Writing non-trivial programs requires either
hand-crafted assembly or an external toolchain. A C compiler targeting 8080/Z80 would allow users
to write higher-level programs and load them directly into the emulator from the IDE.

**Motivation**  
- Dramatically lowers the barrier for writing test programs.  
- Enables classic CP/M C programs to be compiled and run inside emuStudio.  
- SDCC (GPL) is the most complete, actively maintained open-source C compiler for 8080/Z80.

**High-Level Steps**  
1. Evaluate existing options: SDCC (GPL), ACK, Small-C. SDCC is the recommended starting point.  
2. Wrap SDCC as an emuStudio `Compiler` plugin: shell out to a bundled SDCC binary, capture
   output, load the resulting Intel HEX / binary into memory.  
3. Alternatively, port a subset of Small-C to Java for zero-dependency native operation.  
4. Implement error output parsing to populate emuStudio's error list panel (file/line/column).  
5. Expose compiler flags (optimisation level, memory model, target: 8080 vs. Z80) as plugin
   settings.  
6. Write user documentation and example C programs on the emuStudio website.

---

## #190 — Allow saving files directly to 88-dcdd images

**Context**  
`88-dcdd` emulates the MITS Altair disk controller. CP/M disk images are loaded read-only. Users
who compile a program inside emuStudio currently cannot write it into a CP/M disk image — they
must use external tools (cpmtools, etc.).

**Motivation**  
- A seamless compile-to-disk workflow is critical for productive CP/M development.  
- Disk images are the primary distribution format for CP/M software.

**High-Level Steps**  
1. Add a `writeToDisk(String cpmFileName, byte[] data)` API to the `88-dcdd` plugin context.  
2. After successful compilation, offer a "Save to disk" action in the source editor toolbar.  
3. Implement CP/M directory entry creation and data-block allocation in `DiskImage` within
   `88-dcdd`.  
4. Support CP/M 2.2 disk geometry (standard Altair format) as the initial target.  
5. Persist changes back to the `.dsk` file on write or on a deliberate "Flush disk" action.  
6. Add a file browser UI in the `88-dcdd` plugin GUI to list and delete files on the mounted image.

---

## #191 — Implement Intel 80386 CPU

**Context**  
emuStudio currently emulates 8-bit CPUs (8080, Z80) and simple academic CPUs (SSEM, RAM, RASP).
An Intel 80386 CPU would open the door to emulating IBM PC/AT-compatible machines capable of
running MS-DOS or early Windows.

**Motivation**  
- 80386 emulation is a long-term goal toward running real x86 OSes inside emuStudio.  
- Would position emuStudio alongside QEMU/Bochs for educational use in OS course labs.

**High-Level Steps**  
1. Create an `x86-cpu` (or `i386-cpu`) plugin in `plugins/cpu/`.  
2. Implement the 80386 register set: EAX–EDI, segment registers, CR0–CR3, EFLAGS, EIP, debug
   registers.  
3. Implement real-mode instruction decoding and execution (8086/88 subset first, extend to 286
   and 386 protected mode).  
4. Implement the x86 instruction decoder (ModRM, SIB, displacement, immediate encoding) —
   consider generating it with edigen or adapting an existing open-source decoder.  
5. Implement protected mode: GDT/LDT/IDT, segment descriptor loading, rings 0/3, 4 KB paging.  
6. Add a 4 GB-addressable memory plugin; wire interrupts via a virtual 8259A PIC plugin.  
7. Write a test suite using existing x86 opcode test vectors.

---

## #192 — Implement video display for Space Invaders

**Context**  
Space Invaders (1978) is the canonical "hello world" of 8080 emulation. It uses a 256×224 1-bit
pixel framebuffer at memory addresses 0x2400–0x3FFF (rotated 90°), three hardware shift registers
(ports 2/4), and drives a raster display at ~60 Hz via interrupt. emuStudio has an 8080 CPU
plugin but no video display device.

**Motivation**  
- Space Invaders is the proof-of-concept showcase target for every serious 8080 emulator.  
- The framebuffer model is simple and well-documented, making it a good first video device.

**High-Level Steps**  
1. Create a `spaceinvaders-display` device plugin in `plugins/device/`.  
2. Map the plugin to memory region 0x2000–0x3FFF via `byte-mem` memory-mapped I/O (see #268).  
3. Implement the 8-bit shift register responding to ports 2 (shift amount) and 4 (shift data),
   returning the shifted result on port 3.  
4. Implement a Swing `Canvas` that reads the 256×224 framebuffer (1 bit/pixel, column-major,
   rotated) and renders it at a configurable scale with optional colour overlays.  
5. Drive the display refresh at ~60 Hz using `ScheduledExecutorService`; send RST 1 (scanline 96)
   and RST 2 (scanline 224) interrupts to the CPU.  
6. Map keyboard input (arrow keys = joystick, space = fire, C = coin, 1 = start) to input ports
   1 and 2.  
7. Document wiring in a `space-invaders.toml` computer config file.

---

## #206 — Application: Add settings

**Context**  
All emuStudio user preferences (font, colour scheme, debugger column visibility, address format)
are stored inside individual computer TOML config files or hardcoded. There is no global
application-level settings store that persists independently of any specific virtual computer.

**Motivation**  
- Font and theme preferences are personal and should apply universally across all computers.  
- Debugger display options are a user workflow preference, not a per-computer setting.  
- Without persistent settings, users reconfigure on every launch.

**High-Level Steps**  
1. Extend `AppSettings` to persist to `~/.config/emustudio/settings.toml` (XDG on Linux,
   `%APPDATA%\emuStudio` on Windows).  
2. Define schema: `editor.fontFamily`, `editor.fontSize`, `editor.theme`,
   `debugger.addressFormat`, `debugger.visibleColumns`.  
3. Add a "Settings" menu item opening a `SettingsDialog` with tabs: Editor, Debugger.  
4. Apply settings on dialog close: update `REditor` font/theme, update `DebugTableModelImpl`
   column config.  
5. Persist settings on write; reload on application start.  
6. Ensure settings don't conflict with per-computer config — settings are global defaults only.

---

## #218 — byte-mem: support size suffix like K, M

**Context**  
`byte-mem` accepts a numeric size in bytes in its TOML config (e.g., `size = 65536`). Users must
manually calculate sizes in raw bytes, which is error-prone for common values like 64 KiB or 1 MiB.

**Motivation**  
- Human-readable size strings reduce config authoring errors.  
- A 64 K Altair memory config should read `size = "64K"` not `size = 65536`.

**High-Level Steps**  
1. In `byte-mem` settings reader, detect whether `size` is a string (`"64K"`, `"1M"`) or integer.  
2. Implement `parseSizeString(String s)` recognising `K`/`k` (×1024), `M`/`m` (×1048576); no
   suffix = plain bytes. Values above 2 GiB → configuration error.  
3. Write unit tests: `"64K" → 65536`, `"1M" → 1048576`, `"512" → 512`, invalid → exception.  
4. Update byte-mem documentation and config example.  
5. Apply the same utility to `standard-mem` if it has a similar size setting.

---

## #219 — Implement watch expressions below CPU status

**Context**  
The emuStudio debug panel shows CPU register state. There is blank space below the register table
that could host a watch-expression panel — similar to the watch panel in GDB or any modern IDE.

**Motivation**  
- Tracking a specific memory address or register combination across many steps currently requires
  manually reading the debug table on each break.  
- Watch expressions dramatically speed up debugging complex programs.

**High-Level Steps**  
1. Define a simple expression grammar: `mem[address]`, `mem16[address]`, `reg(NAME)`, arithmetic
   (`+`, `-`, `*`, `/`), hex literals (`0x…`).  
2. Implement a recursive-descent parser and evaluator (`WatchExpression`) receiving a `CPU` and
   `MemoryContext` at evaluation time.  
3. Add a `WatchPanel` Swing component (a `JTable` with columns: Expression, Value, Format) below
   the CPU status panel in `EmulatorPanel`.  
4. On every CPU step/break, re-evaluate all watch expressions and refresh the table.  
5. Allow adding/removing/editing expressions via right-click context menu and "Add watch" button.  
6. Persist watch expressions in the per-computer TOML config under a `[watches]` section.  
7. Support display formats: hexadecimal, decimal, binary, ASCII character.

---

## #220 — Link compiler and emulator better (source-level debugging)

**Context**  
After compilation, emuStudio loads the binary into memory and shows it in the disassembler debug
table. There is currently no link from a disassembled instruction row back to the source code line
that generated it.

**Motivation**  
- Source-level debugging (click disassembler row → jump to source line) is the #1 productivity
  feature for assembler/compiler users.  
- Without it, emuStudio's IDE value proposition is weakened compared to standalone assemblers.

**High-Level Steps**  
1. Extend the emuLib `Compiler` interface to return `List<SourceMapping>` (address → file + line)
   after compilation.  
2. Store source mappings in memory metadata (see #344).  
3. In `DebugTableModelImpl`, look up the source mapping for each row's address.  
4. Add a double-click handler on debug table rows: if a mapping exists, open the source file in
   the editor and highlight the corresponding line.  
5. Invalidate the mapping for an address whenever memory at that address is written.  
6. Add a configuration option to disable source mapping tracking for performance-sensitive scenarios.

---

## #235 — Implement Motorola 68000 CPU emulator

**Context**  
The Motorola 68000 CPU powered the original Apple Macintosh (1984), Amiga, Atari ST, and many
workstations. Long-term goal: emulate a Mac 128K or Mac Plus inside emuStudio.

**Motivation**  
- 68000 emulation is a significant leap in emuStudio's scope.  
- The 68K ISA is well-documented with existing open-source emulators and test suites.

**High-Level Steps**  
1. Create an `m68k-cpu` plugin in `plugins/cpu/`.  
2. Implement the register set: D0–D7, A0–A7/SP, PC, SR.  
3. Implement instruction decoding for all 68000 addressing modes — consider using edigen to
   generate the decoder from the ISA specification.  
4. Implement all 68000 instructions (MOVE, ADD, SUB, MUL, DIV, branch, shift, rotate, BCD,
   system).  
5. Implement exception handling: bus error, address error, illegal instruction, privilege
   violation, interrupt auto-vectors 1–7, trap vectors.  
6. Validate against the Musashi test suite and/or 68K opcode test ROMs.  
7. Create a minimal `mac128k` computer config connecting `m68k-cpu`, `byte-mem` with ROM image,
   and stub peripheral devices.

---

## #242 — Implement Paper Tape Reader (PTR) and Paper Tape Puncher (PTP) in Altair 8800

**Context**  
The MITS Altair 8800 supported paper tape I/O through the 88-SIO serial card connected to an
ASR-33 Teletype or standalone paper tape reader/puncher. Paper tape was a primary program
distribution medium in the mid-1970s. SIMH emulates these via `ptr` and `ptp` units in
`altairz80_sio.c`.

**Motivation**  
- Loading programs from paper tape was the original Altair workflow before floppy disks.  
- `simh-pseudo` device (#338) needs attach/detach/reset for PTR/PTP once these devices exist.  
- Enables loading `.pt` paper tape files (Intel HEX or binary) directly into the emulator.

**High-Level Steps**  
1. Create an `88-ptr-ptp` device plugin in `plugins/device/` (or extend `88-sio`).  
2. Model PTR as a byte-stream device on SIO port B: reading returns the next byte from the loaded
   tape file; when exhausted, returns an idle value.  
3. Model PTP as a byte-sink: any byte written is appended to an output file.  
4. Add a GUI with "Load tape" (file chooser for `.pt`/`.bin`), "Save tape", a progress bar, and a
   tape position indicator.  
5. Support `ATTACH PTR <file>` / `ATTACH PTP <file>` commands via the simh-pseudo protocol
   (see #338).  
6. Write unit tests loading a known Intel HEX tape file and verifying bytes are delivered in order.

---

## #251 — Allow opening multiple source code files

**Context**  
The emuStudio source code editor (`REditor`) currently supports a single open file at a time.
Opening a second file replaces the first.

**Motivation**  
- Multi-file projects are common (main file + library files + data files).  
- A tabbed editor is the standard IDE UX for multi-file editing.

**High-Level Steps**  
1. Replace the single `REditor` in `StudioFrame` with a `JTabbedPane` hosting multiple `REditor`
   instances, one per file.  
2. Add "New tab" (Ctrl+T), "Close tab" (Ctrl+W), and "Open file in new tab" (Ctrl+O) actions.  
3. Track the "active" tab; compiler and Run actions always act on the active tab's content.  
4. Show unsaved-changes indicator (`*` in tab title); prompt on close if dirty.  
5. Persist open file paths in the per-computer TOML config for reopening on next launch.  
6. Ensure Ctrl+click-to-open-include (#252) opens the included file in a new tab.

---

## #252 — Allow CTRL+click on include file to open it in editor

**Context**  
The assemblers support `#include "file.asm"` directives. When editing a file that includes other
files, there is currently no way to navigate directly to an included file from the editor.

**Motivation**  
- Click-to-navigate is a standard IDE feature that saves significant time in multi-file projects.  
- Combined with #251 (multi-file tabs), it makes include-heavy codebases practical.

**High-Level Steps**  
1. Add a `MouseListener` to `REditor` detecting Ctrl+click events.  
2. On Ctrl+click, use the RSyntaxTextArea token API to find the token under the cursor.  
3. If the token is inside an `#include "..."` directive, extract the filename string.  
4. Resolve the filename relative to the current file's directory (or the compiler home directory,
   see #294).  
5. If the file exists, open it in the editor (new tab if #251 is implemented).  
6. If not found, show a tooltip/status-bar message "File not found: …".

---

## #253 — as-8080, as-z80: change behavior of ORG

**Context**  
`ORG address` currently sets both the *logical* address of subsequent instructions *and* their
*physical* position in the output binary. This prevents writing self-relocating programs — code
loaded at one physical address but assembled to run at another logical address.

**Motivation**  
- Boot loaders are the canonical use case: loaded at 0x0100 (physical), assembled for 0xF000
  (logical).  
- Separating the two matches what NASM and Pasmo do.

**High-Level Steps**  
1. Change `ORG` to set *only* the logical (program counter) address without affecting the
   physical output offset.  
2. Introduce `LORG` (or `LOAD`) to set the physical load address; alternatively support
   `ORG logical, physical` two-argument form.  
3. Update the code generator to track `PC` (logical) and `outputOffset` (physical) separately.  
4. Update the Intel HEX writer: logical addresses in records, physical addresses for byte
   placement.  
5. Update assembler documentation; add a boot loader example.  
6. Write regression tests: `ORG 0xF000` after emitted code must not change output byte count.

---

## #254 — Implement 88-MDS (Altair Minidisk)

**Context**  
The MITS 88-MDS was a single-density 5.25″ minidisk controller for the Altair 8800, predating the
88-DCDD. Some Altair software specifically targets the MDS controller. SIMH implements it in
`altairz80_mds.c`.

**Motivation**  
- Historical completeness: enables loading MDS-format disk images incompatible with DCDD.

**High-Level Steps**  
1. Create an `88-mds` device plugin in `plugins/device/` mirroring the structure of `88-dcdd`.  
2. Implement the 88-MDS I/O port protocol (status port, command/data port) from the MITS manual
   or SIMH source.  
3. Implement disk image loading/saving for the MDS sector format (77 tracks × 16 sectors × 137
   bytes).  
4. Implement DMA-style sector read/write: the controller transfers bytes to/from a CPU-specified
   memory address.  
5. Add a plugin GUI showing drive status, loaded image path, and track/sector indicator.  
6. Write unit tests loading a known MDS image and verifying sector read byte sequences.

---

## #255 — Implement 88-HDSK (Altair Hard Disk)

**Context**  
The MITS 88-HDSK was an external hard disk interface for the Altair 8800, supporting Pertec
FD-400 drives. SIMH implements it in `altairz80_hdsk.c`. It allows CP/M to use large hard disk
images.

**Motivation**  
- Enables running CP/M with multi-megabyte disk images inside emuStudio.  
- Completes the Altair 8800 peripheral set.

**High-Level Steps**  
1. Create an `88-hdsk` device plugin in `plugins/device/`.  
2. Implement the 88-HDSK I/O port protocol from `altairz80_hdsk.c`: status, data, and control
   ports.  
3. Support the HDSK disk image format used by SIMH (raw sector images, configurable geometry).  
4. Implement sector addressing (cylinder/head/sector), read, write, and format commands.  
5. Add a GUI for mounting/unmounting disk image files and showing drive activity.  
6. Write unit tests for sector read/write round-trips.

---

## #256 — Implement 88-PIO (Parallel I/O)

**Context**  
The 88-PIO was a parallel I/O interface card for the Altair 8800 S-100 bus, based on the Intel
8255 PIA. It provides three 8-bit parallel ports (A, B, C) configurable as input or output. SIMH
implements it in `altairz80_sio.c`.

**Motivation**  
- Required by some Altair software that uses the parallel port for printer output or peripheral
  control.  
- Completes the Altair 8800 I/O device set.

**High-Level Steps**  
1. Create an `88-pio` device plugin in `plugins/device/`.  
2. Model the 8255 PIA: data registers (Port A: 0x08, Port B: 0x09, Port C: 0x0A) and a control
   word register (0x0B). Implement mode 0 (simple I/O) as minimum viable target.  
3. Support configurable I/O direction per port via the control word.  
4. Expose Port A, B, C values in a plugin GUI for inspection and manual input injection.  
5. Allow connecting a downstream device (e.g., a printer plugin) to Port A or B via emuLib
   context.  
6. Write unit tests for control-word writes and port read/write sequences.

---

## #258 — Implement 88-LP (line-printer)

**Context**  
The MITS 88-LP was a line-printer interface card for the Altair 8800 used to drive
Centronics-compatible parallel printers. CP/M's `PIP` command and many BASIC programs send output
to the line printer via this interface.

**Motivation**  
- Completes the Altair 8800 peripheral set for faithful CP/M emulation.  
- Many CP/M programs print listings, reports, or diagnostics to the line printer.

**High-Level Steps**  
1. Create an `88-lp` device plugin in `plugins/device/`.  
2. Implement the 88-LP I/O port protocol (status port and data port); writing a byte sends it to
   the printer.  
3. Buffer bytes into lines; flush on newline (0x0A) or form-feed (0x0C).  
4. Support two output sinks: (a) a GUI text area, (b) a text file on disk.  
5. Add "Clear" and "Save to file" buttons in the GUI.  
6. Write unit tests verifying bytes written to the data port appear in the output buffer.

---

## #259 — Implement 88-C700 printer

**Context**  
The MITS 88-C700 / 88-CENT-CB was a Centronics-interface parallel printer card for the Altair
8800, distinct from the 88-LP in port assignments and protocol. Reference:
<http://www.virtualaltair.com/virtualaltair.com/vac_88-C-700_88-CENT-CB.asp>

**Motivation**  
- Historical completeness alongside 88-LP — different CP/M BIOSes target one or the other.

**High-Level Steps**  
1. Create an `88-c700` device plugin in `plugins/device/` (may share infrastructure with `88-lp`).  
2. Implement the Centronics handshake protocol: STATUS port (STROBE, BUSY, ACKNOWLEDGE), DATA
   port.  
3. Buffer byte output; render to a scrollable text area in the plugin GUI.  
4. Support configurable port base address (default from MITS manual).  
5. Optionally output to a PDF or PostScript file for "printed" output.  
6. Write unit tests for the handshake state machine.

---

## #261 — ADM-3A terminal: allow text display without graphics background

**Context**  
`adm3A-terminal` renders a terminal using a fixed-size graphics canvas with a bitmap background
image of the Lear Siegler ADM-3A terminal. The terminal text is not selectable, not scrollable,
and the window is not resizable.

**Motivation**  
- Copy-paste from the terminal is essential (copying CP/M output, etc.).  
- A resizable, scrollable text terminal is more useful for everyday work.  
- The graphical background is a nice-to-have, not a hard requirement.

**High-Level Steps**  
1. Add a boolean plugin config option `graphicsBackground` (default `true` for backward
   compatibility).  
2. When `false`, render using a standard Swing `JTextArea` in a `JScrollPane`.  
3. Implement ADM-3A control codes (cursor movement, clear screen, etc.) in the text-mode renderer.  
4. Make the text-mode window freely resizable; adjust column/row count on resize.  
5. Support text selection and copy (Ctrl+C) in text mode.  
6. Promote the text mode as a "universal terminal" base that VT100 and other terminal plugins can
   extend.

---

## #264 — as-z80, as-8080: More assembler features

**Context**  
The current assemblers implement a basic set of directives (`ORG`, `DB`, `DW`, `DS`, `EQU`,
`#include`). Professional Z80/8080 assemblers (Sjasm, NASM, Pasmo) offer richer meta-programming
directives.

**Motivation**  
- `ASSERT` catches programmer errors at assembly time.  
- `ALIGN` is needed for data structures requiring alignment (e.g., 256-byte jump tables).  
- `REPEAT`/`REPT` generates repetitive sequences without copy-paste.  
- Multiple hex literal formats (`$FF`, `#FF`, `&FF`) are used in different Z80 coding traditions.

**High-Level Steps**  
1. **`ASSERT expr [, "message"]`** — assembly-time error if expression evaluates to zero/false.  
2. **`ALIGN n [, fill]`** — advance PC to next multiple of `n`, filling with `fill` (default 0x00).  
3. **Hex prefix variants** — accept `$`, `#`, `&` in addition to `0x` and `0FFh` suffix.  
4. **`REPEAT n … ENDREP`** — emit enclosed block `n` times; support loop variable `{#}`.  
5. **`BLOCK size [, fill]`** — allocate `size` bytes initialised to `fill` (alias for `DS size, fill`).  
6. Extend the ANTLR grammar for each feature, add evaluator support, write unit tests.

---

## #265 — Inline helper methods in emulation engines

**Context**  
CPU emulation inner loops (fetch-decode-execute) call many small helper methods (`readByte`,
`add8`, `setFlags`, `parity`). The JIT usually inlines these, but compile-command annotations can
guarantee inlining for the most critical paths and improve worst-case latency predictability.
Reference: <https://compile-command-annotations.nicoulaj.net/>

**Motivation**  
- Z80 / 8080 emulation at 3.5–4 MHz requires ~3.5 M instruction cycles per second; any avoidable
  dispatch overhead degrades timing accuracy.  
- Explicit annotations make the intent clear and survive JIT de-optimisation under GC pressure.

**High-Level Steps**  
1. Profile the 8080/Z80 emulation inner loop with JMH or async-profiler to identify the top-N
   hot helper methods.  
2. Add `compile-command-annotations` as a compile-only dependency.  
3. Annotate identified methods (`readByte`, `writeByte`, `addFlags`, `inc8`, `dec8`) with
   `@ForceInline`.  
4. Generate the HotSpot `CompileCommand` file via the annotation processor in the Gradle build.  
5. Bundle the generated file in the plugin JAR; add `-XX:CompileCommandFile=…` to launch scripts.  
6. Re-benchmark to confirm measurable improvement; document the approach in the CPU plugin README.

---

## #266 — Add ability to set breakpoints in the source code

**Context**  
Breakpoints are currently set only in the debugger's disassembly table (by address). Source-code
breakpoints — clicking the gutter of the source editor to set a breakpoint at a line — are a
standard IDE feature. Depends on source-to-address mapping from #220.

**Motivation**  
- Address-based breakpoints require knowing the assembled address of every interesting line —
  impractical for large programs.  
- Source-line breakpoints decouple debugging from address knowledge.

**High-Level Steps**  
1. Add a clickable gutter to `REditor` (RSyntaxTextArea supports `GutterIconInfo`); clicking line
   N toggles a breakpoint icon.  
2. After compilation, use the source mapping (#220) to resolve each gutter breakpoint line to a
   memory address.  
3. Register the resolved address as a CPU breakpoint via `CPU.setBreakpoint(address)`.  
4. When emulation hits a breakpoint and pauses, highlight the corresponding source line.  
5. Persist breakpoints per file in the computer TOML config; re-resolve after each compilation.  
6. Handle lines with no corresponding address (data, comment, blank) with a "Cannot set breakpoint
   here" tooltip.

---

## #267 — Add ability to setup CPU breakpoints in configuration

**Context**  
Breakpoints set during a debugging session are lost on application restart. There is no way to
pre-configure breakpoints in the computer TOML config.

**Motivation**  
- When repeatedly testing a specific routine (e.g., CP/M BDOS at 0x0005), users should not
  manually set the same breakpoints on every launch.  
- Persistent breakpoints are a standard feature of embedded debuggers and emulators.

**High-Level Steps**  
1. Add a `[breakpoints]` section to the computer TOML config: `addresses = [0x0005, 0x0100, …]`.  
2. On computer load in `VirtualComputer`, read the list and call `cpu.setBreakpoint(address)` for
   each.  
3. In the debugger GUI, when the user adds/removes a breakpoint, update the in-memory config and
   offer a "Save breakpoints" action.  
4. Alternatively, auto-save breakpoints to config on application exit.  
5. Display loaded breakpoints in the debug table on startup (rows at those addresses show the
   breakpoint icon immediately).

---

## #268 — byte-mem: memory-mapped I/O

**Context**  
Many hardware architectures (ZX Spectrum ULA, Space Invaders framebuffer, video controllers)
implement devices as memory-mapped I/O. Currently `byte-mem` is a flat RAM array with no hook
for mapping address ranges to device plugins. Required by #192 (Space Invaders) and #314 (ZX
Spectrum ULA).

**Motivation**  
- Memory-mapped I/O is a fundamental hardware concept; supporting it enables emulation of a much
  wider range of hardware.

**High-Level Steps**  
1. Extend `byte-mem` TOML config with a `[[mappings]]` array: `from`, `to`, `pluginId`,
   `direction` (READ / WRITE / BOTH).  
2. Add a `MemoryMappedDevice` emuLib interface: `byte read(int relativeAddress)` and
   `write(int relativeAddress, byte value)`.  
3. In `byte-mem`'s `read`/`write` methods, check if the address falls in a mapped range; if so,
   delegate to the mapped device.  
4. Populate the mapping table at plugin initialisation by looking up connected plugin contexts.  
5. Handle overlapping mappings: last-write-wins or explicit priority ordering.  
6. Write integration tests: map a stub device to 0x4000–0x7FFF; verify reads/writes go to the
   device and not the backing array.

---

## #271 — Do not enforce file saving when compiling

**Context**  
Clicking "Compile" currently forces the user to save the source file to disk before compilation
proceeds. This is annoying during iterative development. Root cause: compilers receive a file path,
not a string buffer (see #294).

**Motivation**  
- Modern IDEs compile from the in-memory buffer without requiring a save.  
- Forced saves pollute version history and break "undo" workflows.

**High-Level Steps**  
1. Extend the emuLib `Compiler` interface with an overload
   `compile(String sourceCode, Path sourceOrigin)` where `sourceOrigin` is used only for resolving
   relative includes.  
2. In `CompileAction`, pass the editor's current text buffer directly to the new overload.  
3. If `sourceOrigin` is null (unsaved new file), resolve includes relative to the current working
   directory.  
4. Remove the "save before compile" prompt; retain a "file modified" indicator in the tab title.  
5. Update `as-8080` and `as-z80` to implement the new overload.  
6. Write a test that compiles a source string without a backing file.

---

## #277 — as-z80, as-8080: ability to generate various output file formats

**Context**  
Both assemblers currently output only Intel HEX. Other formats are needed:

- **BIN** — raw binary for ROM programmers.  
- **TAP** — ZX Spectrum tape format for loading into the ZX Spectrum emulator (#314).  
- **TZX** — extended tape format with turbo-speed and pure-tone blocks.  
- **PRL** — Page Relocatable format for CP/M Plus RSX modules.

Reference: Pasmo assembler documentation.

**Motivation**  
- ZX Spectrum demos/games are typically distributed as TAP/TZX; the assembler must produce them
  directly.  
- CP/M RSX modules require PRL format.

**High-Level Steps**  
1. Add an output format option to the assembler plugin settings and `compile()` parameters.  
2. **BIN writer**: dump assembled bytes in order from the lowest `ORG` address.  
3. **TAP writer**: wrap binary in a standard TAP header block (type=3, filename, length, load
   address) followed by a data block; compute checksums.  
4. **TZX writer**: emit TZX header, then standard-speed data blocks equivalent to TAP content;
   allow configuring turbo-speed parameters.  
5. **PRL writer**: prepend a 256-byte PRL header with relocation bitmap; mark absolute addresses
   requiring page-load relocation.  
6. Expose format choice as a dropdown in the compiler plugin settings dialog and as a `--format`
   CLI flag in the `emuStudio automate` command.

---

## #294 — Compilers: ability to set a home directory

**Context**  
`#include "lib/macros.asm"` directives resolve relative to the source file's directory. When the
source is not yet saved to disk (#271), the compiler has no reference directory and relative
includes fail. Users also want a shared library directory for all projects.

**Motivation**  
- A configurable "home directory" decouples include resolution from the source file location.  
- Required as a prerequisite for #271 (compile from unsaved buffer).

**High-Level Steps**  
1. Add a `homeDirectory` setting to each compiler plugin's TOML config section.  
2. In `as-8080` and `as-z80`, use `homeDirectory` as the base for resolving relative `#include`
   paths; fall back to the source file's directory, then the current working directory.  
3. Expose `homeDirectory` as a configurable field in the compiler plugin settings dialog.  
4. Pass `homeDirectory` through the `Compiler.compile()` call or store it in plugin settings at
   initialisation.  
5. Document the setting and its precedence rules in the plugin documentation.

---

## #314 — Implement ZX Spectrum 48K (remaining items)

**Context**  
The ZX Spectrum 48K emulation is largely implemented. Several items remain:

- [ ] Load `.z80` snapshot format (most common snapshot format; full CPU state + memory restore).  
- [ ] Load `.sna` snapshot format (simpler snapshot; PC stored on stack).  
- [ ] TZX loader in `audiotape-player` (turbo-speed, pure-tone, pulse-sequence blocks).  
- [ ] ZX Spectrum 48K user documentation on the emuStudio website.  
- [ ] Z80 test pass documentation — table of which Z80 test ROMs pass/fail.  
- [ ] as-z80 code examples + tutorials for ZX Spectrum programming.

**High-Level Steps**  
1. **`.z80` loader**: parse v1/v2/v3 header; restore all Z80 registers, I, R, IFF1/IFF2, IM;
   decompress memory; restore border colour via ULA port 0xFE.  
2. **`.sna` loader**: parse the 49179-byte file; restore registers; copy 48K memory; pop PC from
   stack (SP+2).  
3. **TZX loader**: implement block types 0x10 (standard speed), 0x11 (turbo speed), 0x12 (pure
   tone), 0x13 (pulse sequence), 0x20 (pause), 0x5A (glue). Schedule pulse train delivery to the
   ULA EAR input.  
4. **Documentation**: write Markdown pages covering hardware specs, plugin configuration, keyboard
   mapping, and known limitations.

---

## #318 — Byte Memory: ability to load TAP and TZX files

**Context**  
Intel HEX files are the current standard for loading programs into `byte-mem` — they specify a
target address for each data record. TAP/TZX files are the ZX Spectrum equivalent: each block
contains a header with a load address and a data block. Direct loading would allow ZX Spectrum
programs to be loaded without going through the tape player device.

**Motivation**  
- Enables "instant load" of ZX Spectrum programs (bypass the tape loading process).  
- Useful for debugging: load directly into memory at known addresses.

**High-Level Steps**  
1. Parse TAP blocks: 2-byte block length, 1-byte flag (0=header, 0xFF=data), block data, checksum;
   for data blocks use the load address from the preceding header block.  
2. Parse TZX blocks: read TZX magic (`ZXTape!`), then parse block types 0x10/0x11 to extract data
   bytes; apply the same header/data interpretation as TAP.  
3. Register `.tap` and `.tzx` extensions in the memory plugin's file loader dialog.  
4. Handle multi-block TAP files: process all blocks sequentially, writing each data block to its
   declared address.  
5. Write unit tests loading a known TAP file and verifying memory contents match expected values.

---

## #338 — Enhance simh-pseudo when PTP/PTR are implemented

**Context**  
The `simh-pseudo` device emulates the SIMH pseudo-device protocol for CP/M BIOS
implementations. `AttachPTR`, `DetachPTR`, `ResetPTR`, `AttachPTP`, `DetachPTP` command handlers
already exist as stubs — `start()` bodies contain only
`//attachCPM(&ptr_unit)` and `clearWriteCommand()` — because the PTR/PTP device plugins do not yet
exist.

**Dependency**: #242 (Implement PTR/PTP in Altair 8800) must be completed first.

**Motivation**  
- CP/M programs that use the SIMH pseudo-device to attach/detach paper tape need these commands to
  wire up actual hardware.  
- Without this, CP/M SIMH BIOS builds that rely on paper tape I/O fail silently.

**High-Level Steps**  
1. Once #242 is done, obtain a `PaperTapeContext` from `88-ptr-ptp` via `ContextPool`.  
2. Implement `AttachPTR.start()`: call `context.attachTape(filename)` where `filename` is read
   from the simh command write buffer.  
3. Implement `DetachPTR.start()`: call `context.detachTape()`.  
4. Implement `ResetPTR.start()`: call `context.rewindTape()`.  
5. Implement analogous methods for `AttachPTP`, `DetachPTP`.  
6. Write integration tests that issue ATTACH/DETACH commands via the simh-pseudo protocol and
   verify the PTR/PTP context is invoked.

---

## #344 — Support memory metadata

**Context**  
Memory metadata is source-location information (file, line, column) attached to memory addresses.
Needed for source-level debugging (#220, #266) and editor margin annotations. emuLib issue #78
tracks the API definition.

Affects four subsystems:

**Source Code Editor**  
- After compilation, show metadata in a vertical gutter strip (line markers, breakpoint icons).  
- On any memory write, invalidate the metadata at that address.  
- Link editor line → memory address for breakpoints (#266).

**Compilers**  
- After assembly/compilation, write `SourceCodePosition` records into memory via a metadata API.

**Memories**  
- Store `SourceCodePosition` (nullable) alongside the data array.  
- Expose `setMetadata(int address, SourceCodePosition pos)` and `getMetadata(int address)`.  
- Persist metadata to/from a sidecar file on load/save.

**Devices**  
- Memory-mapped devices (see #268) may annotate the addresses they own with register semantics.

**High-Level Steps**  
1. Define `SourceCodePosition` (file, line, column) and the metadata API in emuLib.  
2. Implement metadata storage in `byte-mem` (nullable `SourceCodePosition[]` array, same length as
   data array).  
3. Update `as-8080` and `as-z80` to write metadata after compilation.  
4. Update `REditor` to read metadata and display line markers in the gutter.  
5. In `DebugTableModelImpl`, look up metadata for each address to enable source linkage (#220).  
6. Implement metadata persistence (sidecar file) in `byte-mem`.

---

## #345 — Get rid of memory getSize() dependency in call flow

**Context**  
`PaginatingDisassembler` takes a `Supplier<Integer> getMemorySize` in its constructor, and
`DebugTableModelImpl` / `EmulatorPanel` call `memoryContext.getSize()` to populate this supplier.
This creates a hard dependency on a single memory's address space size.

Problems:
- Computers with multiple memory banks or bank-switching have context-dependent address space
  boundaries.  
- A computer with ROM + RAM as separate plugins has no single `getSize()`.  
- `getSize()` returning a large value (4 GB for a 32-bit CPU) causes pre-allocation of huge
  structures.

**Motivation**  
- Correctly support bank-switched machines without hard-coding one memory size.  
- Make the disassembler address range dynamically extensible (grow-as-needed cache).

**High-Level Steps**  
1. Remove the `getMemorySize` supplier from `PaginatingDisassembler`; replace with a fixed
   configurable maximum address (defaults to `0xFFFF` for 8-bit CPUs) or a lazily growing page
   cache.  
2. In `CallFlow`, replace any `memorySize`-based loop bounds with the actual decoded instruction
   range.  
3. In `DebugTableModelImpl`, remove the `memorySizeChanged()` callback or repurpose it to update
   the CPU address space upper bound.  
4. Update `EmulatorPanel` to pass the CPU's address space size rather than memory's `getSize()`.  
5. Add `getAddressSpaceSize()` default method to the emuLib `CPU` interface (returning `0x10000`
   for backward compatibility).  
6. Write tests with a mock CPU having a 256-byte address space to verify the disassembler doesn't
   overflow.

---

## #352 — Replace custom ImageIcons with UIManager constants

**Context**  
emuStudio's toolbar and dialogs load icons from bundled resources (`.png`/`.gif` files in
`src/main/resources`). The Java `UIManager` provides a standard set of look-and-feel icons via
constants like `UIManager.getIcon("Tree.openIcon")`.

**Motivation**  
- Reduces binary size (fewer bundled resources).  
- Automatically adapts to system theme (dark mode on macOS with Nimbus LAF).  
- Eliminates the visual inconsistency of emuStudio icons vs. system icon style.

**High-Level Steps**  
1. Audit all `new ImageIcon(getClass().getResource(…))` calls in the application source
   (`StudioFrame`, `DialogsGui`, `EmuStudioGui`, `REditor`).  
2. For each icon, identify the closest `UIManager` constant equivalent.  
3. Replace each `ImageIcon` construction with `UIManager.getIcon("key")` with a null-safe fallback
   to the original resource if the key returns null.  
4. In application startup, call `UIManager.put("key", customIcon)` for icons that have no standard
   equivalent, so they remain customisable.  
5. Remove unused icon resources from `src/main/resources` once all references are replaced.  
6. Test under at least two look and feels (Metal, Nimbus) to ensure icons display correctly.

---

## #361 — Allow setting configuration and plugins base path in cmdline

**Context**  
`ConfigFiles.java` contains
`private static final Path basePath = Path.of(System.getProperty("user.dir"))` used for all config
file and plugin JAR resolution. emuStudio must always be launched from its installation directory.
If the working directory differs (e.g., a desktop shortcut), config and plugin files are not found.

**Motivation**  
- Enables running emuStudio from any working directory.  
- Required for Flatpak / system-wide installation where the CWD is not the install directory.  
- Enables running multiple emuStudio instances with isolated config directories.

**High-Level Steps**  
1. Add `--config-dir PATH` to `Runner` via picocli `@Option`; also accept `EMUSTUDIO_CONFIG_DIR`
   environment variable as fallback.  
2. Add `--plugins-dir PATH` similarly; fallback to `EMUSTUDIO_PLUGINS_DIR`.  
3. Change `ConfigFiles.basePath` from `static final` to a mutable static field; initialise it in
   `Runner.run()` from the CLI options, defaulting to `System.getProperty("user.dir")`.  
4. Thread `basePath` through `getAbsolutePluginPath()` and `loadConfigurations()`.  
5. Update startup scripts (`emuStudio.sh`, `emuStudio.bat`) to pass `--config-dir` and
   `--plugins-dir` pointing to the installation directory.  
6. Write tests in `ConfigFilesTest` verifying config files and plugin JARs resolve relative to the
   specified base path.

---

## #398 — Implement Audio chip AY-3-8910

**Context**  
The `audio-ay3_8910-chip` device plugin already exists in `plugins/device/` with a partial
implementation (`Ay38910Chip.java`, `DeviceImpl.java`, `AudioSink`, `SoundAudioSink`, a waveform
display GUI, and tests). The AY-3-8910 is the Programmable Sound Generator (PSG) used in ZX
Spectrum 128K, MSX, Amstrad CPC, and other Z80-based computers. The 48K ZX Spectrum uses only a
beeper, but many demos/games also target the AY chip (128K models or unofficial 48K interfaces
like the Fuller Box).

**What remains**  
- Integration with a ZX Spectrum 128K computer config.  
- Verification of all 16 AY registers (tone period A/B/C, noise period, mixer, amplitude A/B/C,
  envelope period, envelope shape, I/O ports A/B).  
- Correct resampling to host audio at 48 kHz.  
- Plugin documentation.

**High-Level Steps**  
1. Audit `Ay38910Chip.java` against the AY-3-8910 datasheet for all 16 registers, envelope
   generator, noise generator, mixer.  
2. Verify `SoundAudioSink` resampling produces clean audio at the expected sample rate.  
3. Create a `zxspectrum-128k.toml` computer config (or update `zxspectrum.toml`) adding the AY
   chip connected to the Z80 CPU.  
4. Map AY bus access to Z80 I/O ports: 0xFFFD (register select), 0xBFFD (register write), 0xFFFF
   (register read) — standard 128K Spectrum port mapping.  
5. Write integration tests that program the AY chip and verify the audio output waveform using
   `RecordingAudioSink`.  
6. Write plugin documentation for the emuStudio website.
