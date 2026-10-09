# MITS parallel I/O (#256)

One `88-pio.jar` plugin supports two original boards. Neither uses an Intel 8255.

| `boardType` | Hardware | Default CPU ports | Context |
| --- | --- | --- | --- |
| `88-PIO` | Two Intel 8212 latches | `04h–05h` | One `DeviceContext<Byte>` |
| `88-4PIO` (default) | Motorola 6820 PIAs | `A0h–A7h` | `PioContext` pins and control lines |

Bundled Altair computers use `boardType = "88-4PIO"`, `basePort = 160`, `piaCount = 2`.
MHDSK connects to that plugin; only PIO attaches CPU ports. DCDD remains at `08h–0Ah`.
For the original board use `boardType = "88-PIO"`, `basePort = 4`. Old `08h–0Bh`
8255 configurations must change; that interface was based on an incorrect specification.

## Original 88-PIO

`basePort` must be even, from 0 to 254. The even address reads status: bit 0 is
output-device-ready, bit 1 is input-data-ready. Writing it enables the corresponding interrupts.
The odd address reads the input latch and clears input-ready, or writes the output latch and
clears output-ready. Both latches are eight bits; input strobes overwrite the latch without a queue.

A peripheral calls `writeData` to strobe input. `readData` consumes output and raises
output-ready to request another byte. The GUI permits input strobes and output-ready changes.
`interruptVector` selects an 8080 RST vector from 0 to 7 (default 7).

## 88-4PIO

`basePort` selects an aligned block of 16 ports (`00h` through `F0h`). `piaCount` selects
1–4 populated PIAs; each owns four ports. Two PIAs provide the MHDSK's four eight-bit sides.
Each side's even address is control; its odd address accesses DDR when control bit 2 is
clear, or data when set. DDR bit 1 means output. Data reads mix input pins and output latches.

C1/C2 input edges latch interrupt flags independently of enables. Data reads clear flags;
control reads and data writes preserve them. C2 supports input, handshake output, pulse output,
and static low/high output. A-side output strobes follow data reads; B-side strobes follow
data writes. Handshake outputs return high on the selected C1 edge; pulse outputs return high
within the CPU access. Enabled flags signal the configured RST vector.

`net.emustudio.plugins.device.mits88pio.api.PioContext` exposes input pins, output pins and
DDR masks, C1/C2 input lines, and output/strobe callbacks. Channels run PIA1-A, PIA1-B,
PIA2-A, PIA2-B, etc. One peripheral owns the board's connector. Access and callbacks are
serialized on the context monitor; peripheral host-side work must use the same monitor.
Byte-only device contexts cannot represent these handshakes.

References: [original 88-PIO manual](https://deramp.com/downloads/altair/hardware/MITS%2088-PIO.pdf),
[original 88-4PIO manual](https://deramp.com/downloads/altair/hardware/MITS%2088-4PIO.pdf).
