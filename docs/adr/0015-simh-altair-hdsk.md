# ADR-015: SIMH Altair HDSK extension

## Status
Accepted

## Context
SIMH CP/M images use a synthetic HDSK peripheral for storage larger than original Altair floppy drives. It is not a
MITS hardware controller: software sends a seven-byte command packet to port FDh and the device transfers one sector
through emulated memory.

## Decision
Provide HDSK as a separate device plugin connected to the 8080 CPU and byte memory. Support 16 raw images,
configurable power-of-two sector size and sectors per track, SIMH read/write/parameter commands, and GUI image control.

## Consequences
SIMH HDSK-enabled CP/M software can reuse raw images. Geometry stays explicit per drive. IMD containers and unrelated
physical-controller commands are outside this plugin; no new emuLib service or dependency is required.
