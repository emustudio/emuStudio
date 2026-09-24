# Altair HDSK

Implements the SIMH Altair HDSK extension on port FDh. It exposes up to 16 raw hard-disk images through the
SIMH seven-byte read/write command packet and transfers sector data directly to or from emulated memory.

Default geometry is 2,048 tracks, 32 sectors per track, and 128 bytes per sector (8 MiB).
