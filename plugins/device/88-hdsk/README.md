# Altair hard disks (#255)

One `88-hdsk.jar` plugin supports two interfaces, selected by `controllerType`:

| `controllerType` | Interface | Connection |
| --- | --- | --- |
| `SIMH` (default) | Synthetic SIMH HDSK extension | CPU port `FDh` and byte memory for DMA |
| `MITS` | MITS 88-HDSK / MHDSK | PIO plugin in `boardType = "88-4PIO"` mode |

## Synthetic SIMH HDSK

Existing configs without `controllerType` retain the SIMH interface. It exposes up to 16 raw
hard-disk images through seven-byte command packets and transfers sector data directly to/from
emulated memory. Default geometry is 2,048 tracks, 32 sectors per track and 128-byte sectors
(8 MiB). Existing `imageN`, `sectorSizeN` and `sectorsPerTrackN` settings remain valid.

## Settings dialog

The settings dialog follows the 88-DCDD layout: drive buttons, image Browse/Create/Unmount/Unmount all,
grouped parameters, a controller tab, and Save. Image and geometry edits remain drafts until Save;
ESC discards them. Browse selects an existing image; Create immediately creates a new file without
replacing existing media. Save mounts the selected image; ESC leaves newly created files unmounted.
Save persists all drives and applies their images/geometry to the running controller. MITS mode
offers eight platters with read-only settings and fixed geometry; SIMH offers sixteen drives with
configurable sector size and sectors per track. All image mounting and unmounting happens through
settings; Save also preserves the selection for the next launch.

Changing controller type takes effect after reopening the computer. The Controller tab shows the
required schema connections: CPU and memory for SIMH, or 88-4PIO for MITS.

## Device window

Both controller modes use one non-modal window in the 88-DCDD style: disk-selection buttons
with mounted-image indicators, grouped flags/geometry, and a scrollable read-only image path.
SIMH exposes sixteen drives and editable geometry; MITS exposes eight removable/fixed platters,
fixed geometry, and write protection. The status window has no image-management controls.
Activity and mount status refresh while visible;
ESC or closing the window stops refresh, and reopening resumes it.

## MITS 88-HDSK / MHDSK

Select `controllerType = "MITS"` and connect this device to `88-pio.jar` in
`boardType = "88-4PIO"` mode, with at least two PIAs. Bundled Altair configs use this mode.
Only PIO owns `A0h–A7h`; the MITS controller registers no CPU ports and needs no direct CPU
or guest-memory connection. Settings `imageN` and `readOnlyN` select its raw platter images.

| Port | 4PIO side | Read | Write |
| --- | --- | --- | --- |
| A0h/A1h | PIA1-A | Controller-ready control / error status | DDR/control setup |
| A2h/A3h | PIA1-B | Command-acknowledge control | Command high byte |
| A4h/A5h | PIA2-A | Data-ready control / controller data | DDR/control setup |
| A6h/A7h | PIA2-B | Altair-data-port-available control | Command low byte / data |

Guest software initializes DDR and control registers as in MITS table 3-C:
input/pulse control `2Ch` on A sides, output/handshake `24h` on PIA1-B,
output/pulse `2Ch` on PIA2-B. Write command low byte before high byte.
Reading the relevant data register acknowledges its ready flag; control reads preserve flags.

The high nibble selects seek (0), write/read sector (2/3), write/read buffer (4/5),
read IV status (6), set IV byte (8), unformatted read (A), header format (C), or initialize (E).
Four 256-byte buffers support partial transfers; byte count 0 means 256.
Each of four units keeps its own cylinder.
Buffer-ready is signaled once at the start of a block; subsequent bytes use the data strobes,
as in firmware v4.3. Controller-ready is signaled after the final byte.

Settings `image0`–`image7` mount existing raw platter files; `readOnly0`–`readOnly7` select
write protection. Indices are unit 0 removable/fixed, then unit 1 removable/fixed, through unit 3.
Each file contains 4,988,928 bytes: 406 cylinders × 2 surfaces × 24 sectors × 256 bytes.
Layout matches SIMH MHDSK: cylinder, surface, then sector. Use settings to create blank images without
overwriting existing files and to mount/unmount platters; the status window shows command/error activity.

Limits: this is a functional command and PIA handshake model. Mechanical delays, controller
microcode execution, extended platters (heads 4–7), and physical header/CRC bitstreams are
not emulated. Raw images imply formatted headers; FORMAT validates writable media, preserves
sector data, and leaves the selected unit at cylinder 405, matching header-only formatting.
Unformatted reads expose sector bytes with the documented CRC-error flag rather than a physical
bitstream. IV commands store diagnostic bytes and expose modeled status; arbitrary IV writes
do not operate all physical drive/data-card pins. A missing drive completes with not-ready
instead of waiting indefinitely for mechanical readiness.

References: [MITS manual](https://deramp.com/downloads/altair/hardware/hard_disk/88-HDSK.pdf),
[corrected handshakes and errors](https://deramp.com/downloads/altair/hardware/hard_disk/HDSK%20Doc%20Errata.pdf),
[firmware v4.3 and header-only format](https://deramp.com/downloads/altair/hardware/hard_disk/8X300%20version%204-3.pdf),
[SIMH MHDSK source](https://github.com/simh/simh/blob/master/AltairZ80/altairz80_mhdsk.c).
The test boot ROM retains its upstream notice in `src/test/resources/mhdsk-boot-LICENSE.txt`.
