# ADR-018: Shared video and audio recording in emuLib

## Status
Accepted

## Context
Space Invaders and ZX Spectrum duplicate recording queues, MP4 encoding, PCM buffering, audio muxing and temporary
file cleanup. Their capture sources and clocks differ, but these file and lifecycle operations are reusable utilities,
like Intel HEX support. Maintaining two engines has already produced different buffering and save behavior.

## Decision
emuLib owns `net.emustudio.emulib.runtime.recording.RecordingSession` and its package-private MP4 implementation.
JCodec 0.2.3 is an emuLib implementation dependency; its types do not appear in the public API. The session copies inputs,
accepts video and audio independently or in paired captures, preserves rational frame rates and records signed 16-bit
little-endian stereo PCM at the producer's sample rate. A bounded worker queue encodes H.264 during recording; Save drains
accepted captures, finalizes video and muxes PCM. Discard drops pending captures. Capture failures and queue overload are
reported to the plugin without blocking emulation; all exit paths release temporary files.

Plugins own native raster capture, clocks, audio synthesis/mixing, record controls, file chooser and UI error handling.
Spectrum submits completed native frames at the emulated frame boundary, preserving the border and padding odd raster
heights to an even size. Painting and window resizing do not change recordings. Invaders retains 224x256 at 60 fps.

## Consequences
Both plugins use one video/audio engine and shared tests in emuLib. They no longer depend directly on JCodec or maintain
local recording classes. Save avoids re-encoding frames and temporary storage holds encoded video plus compressed PCM.
Consumers require an updated emuLib with its JCodec runtime dependencies. Toolbar behavior remains in each device.
This supersedes the device-private recorder ownership in ADR-016; its hardware and audio-production boundaries remain.
