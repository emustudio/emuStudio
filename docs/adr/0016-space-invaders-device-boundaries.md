# ADR-016: Space Invaders device boundaries

## Status
Accepted

## Context
Space Invaders combines CPU ports, keyboard input, a memory-backed framebuffer, and raster interrupts. Putting these
concerns in byte-mem or the 8080 CPU would couple generic plugins to one machine.

## Decision
Provide one device plugin with five internal boundaries: a port/input/shift-register core, a Swing framebuffer
adapter over MemoryContext, a Java Sound sample-playback adapter, a 120 Hz clock alternating RST 1 and RST 2,
and an asynchronous MP4 recording adapter.
The core decodes sound ports 3 and 5, triggers effects on rising edges, gates the UFO loop and amplifier, and stops
audio on reset. The adapter loads external 0.wav through 9.wav samples from `soundSamplesDirectory`; `soundEnabled`
defaults to enabled for GUI execution and disabled for headless execution. Missing samples or audio devices leave
emulation running with a warning. Read video RAM through the existing memory context; do not add game-specific
memory-mapped I/O to byte-mem.

Recording follows the ZX Spectrum record/stop/save flow and uses the same bundled JCodec encoder and PCM audio
muxing approach. Keep recorder implementations private to each device rather than importing another plugin's
implementation. Capture the native 224x256 framebuffer at 60 fps and mix currently playing samples into
48 kHz stereo audio, applying the selected volume. Capture continues when Swing repaint requests are coalesced;
H.264 encoding runs during recording on a worker, through a queue bounded to two seconds of frames. If the encoder
falls behind, stop recording and report the failure rather than blocking emulation or retaining unlimited frames.
Save only finalizes the encoded MP4 and adds PCM audio, off the Swing event thread. Window scaling adds no detail
to the recorded pixels and does not change recording dimensions. Closing the display discards active recording
and releases temporary files. Compress temporary PCM data losslessly with JDK gzip; store only encoded video
rather than gigabytes of raw frames per minute in the system temporary directory.

## Consequences
The CPU and memory plugins remain reusable. The display can run headless while still producing interrupts. Wall-clock
refresh favors playable emulation over cycle-exact scanline timing; ROM images and sound samples remain user-supplied.
Sound playback uses the JDK; recording uses JCodec already bundled by emuStudio. Neither changes the CPU or shared
plugin API. Temporary recordings consume disk space until exported or discarded.
