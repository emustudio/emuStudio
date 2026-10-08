# ADR-016: Space Invaders device boundaries

## Status
Accepted

## Context
Space Invaders combines CPU ports, keyboard input, a memory-backed framebuffer, and raster interrupts. Putting these
concerns in byte-mem or the 8080 CPU would couple generic plugins to one machine.

## Decision
Provide one device plugin with four internal boundaries: a port/input/shift-register core, a Swing framebuffer
adapter over MemoryContext, a Java Sound sample-playback adapter, and a 120 Hz clock alternating RST 1 and RST 2.
The core decodes sound ports 3 and 5, triggers effects on rising edges, gates the UFO loop and amplifier, and stops
audio on reset. The adapter loads external 0.wav through 9.wav samples from `soundSamplesDirectory`; `soundEnabled`
defaults to enabled for GUI execution and disabled for headless execution. Missing samples or audio devices leave
emulation running with a warning. Read video RAM through the existing memory context; do not add game-specific
memory-mapped I/O to byte-mem.

## Consequences
The CPU and memory plugins remain reusable. The display can run headless while still producing interrupts. Wall-clock
refresh favors playable emulation over cycle-exact scanline timing; ROM images and sound samples remain user-supplied.
Sound playback uses the JDK without another library or changes to the CPU or shared plugin API.
