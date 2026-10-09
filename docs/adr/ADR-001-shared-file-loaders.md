# ADR-001: Shared file loaders in emuLib

## Status
Accepted

## Context
Tracked by [emuLib #119](https://github.com/emustudio/emuLib/issues/119).
Both audiotape-player and byte-mem parse TAP/TZX. Byte memory also loads HEX and BIN.
Their destinations differ: tape playback needs raw bytes and timing; direct memory loading needs addressed bytes.
The second consumer satisfies the extraction condition recorded in emuLib #60.

## Decision
emuLib owns `runtime.io.FileLoader`, format recognition, immutable data/tape blocks, tape traversal,
playback callback decoding, and Spectrum header/packet decoding. Both plugins use this contract.
The generic loader emits blocks through `FileLoader.Listener`; BIN emits unaddressed bytes. HEX retains the existing IntelHEX I8HEX implementation.
Tape playback follows TZX control flow; memory loading visits blocks in file order and accepts only
standard/turbo Spectrum CODE data. byte-mem owns BIN placement and strict Spectrum CODE pairing/checksum/length policy.
Playback preserves original bytes, including corrupt checksums, and existing callback behavior.
`TapeListener` implements the common listener directly, without a sink factory.
GUI, cassette state, CPU scheduling, EAR output, memory placement, banks, and sidecars remain plugin responsibilities.

## Consequences
Duplicated format parsers disappear. Existing IntelHEX APIs remain available to compilers.
Plugins require an emuLib snapshot containing the new API. For local development and validation use
`./gradlew -PemuLibPath=../emuLib :plugins:memory:byte-mem:test :plugins:device:audiotape-player:test`.
Unsupported recordings/custom loaders remain outside direct memory loading; shared parsing does not emulate BASIC.
File errors restore the previous selected bank without rolling back earlier writes.
