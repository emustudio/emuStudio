# ADR-002: MITS parallel I/O and hard disk boundaries

## Status
Accepted

## Context
Tracked by [#256](https://github.com/emustudio/emuStudio/issues/256) and
[#255](https://github.com/emustudio/emuStudio/issues/255).
The old 88-PIO specification incorrectly described an 8255 at the DCDD's ports.
Original 88-PIO uses two 8212 latches; 88-4PIO uses 6820 PIAs. MITS hard disks attach
through 4PIO, while SIMH's synthetic HDSK uses a distinct FDh protocol.

## Decision
Keep both parallel boards in one `88-pio` plugin, selected by `boardType`.
PIO alone owns CPU ports. Its plugin-local `PioContext` exposes 4PIO pins,
direction masks and control-line callbacks. Keep both disk interfaces in one `88-hdsk`
plugin, selected by `controllerType`: `MITS` connects through this context and owns
controller commands, buffers and platter images; `SIMH` preserves the synthetic FDh/DMA
interface and remains the default for existing configurations. GUI and files stay in
their device plugins. emuLib's public API is unchanged.

Bundled Altair configurations select two PIAs at A0h–A7h and connect MHDSK behind PIO.
DCDD remains at 08h–0Ah; synthetic HDSK mode remains at FDh.

## Consequences
Guest DDR setup and handshakes use the original board's register behavior.
There is one CPU-port owner and no hard-disk dependency on desktop internals or DMA.
Existing 8255-style PIO configs and three byte endpoints must migrate to the selected
real board model. MITS disk mode requires `88-pio.jar`; this context is an inter-plugin API,
not a shared emuLib service. Raw-sector media and immediate completion have explicit
physical-model limits documented in the HDSK README. Earlier local `88-mhdsk.jar`
configs migrate to `88-hdsk.jar` with `controllerType = "MITS"`; the separate module is removed.
