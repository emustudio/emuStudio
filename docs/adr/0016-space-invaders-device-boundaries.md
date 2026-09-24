# ADR-016: Space Invaders device boundaries

## Status
Accepted

## Context
Space Invaders combines CPU ports, keyboard input, a memory-backed framebuffer, and raster interrupts. Putting these
concerns in byte-mem or the 8080 CPU would couple generic plugins to one machine.

## Decision
Provide one device plugin with three internal boundaries: a port/input/shift-register core, a Swing framebuffer
adapter over MemoryContext, and a 120 Hz clock alternating RST 1 and RST 2. Read video RAM through the existing memory
context; do not add game-specific memory-mapped I/O to byte-mem.

## Consequences
The CPU and memory plugins remain reusable. The display can run headless while still producing interrupts. Wall-clock
refresh favors playable emulation over cycle-exact scanline timing; ROM images remain user-supplied.
