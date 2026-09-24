# 88-MDS

Emulates the MITS 88-MDS minidisk controller and 88-MDDR drives.

The controller uses the original programmed-I/O ports 08h, 09h, and 0Ah. Raw images contain 35 tracks,
16 sectors per track, and 137 bytes per sector (76,720 bytes, with optional trailing padding).
