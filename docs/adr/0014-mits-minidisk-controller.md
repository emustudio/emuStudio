# ADR-014: MITS minidisk controller

## Status
Accepted

## Context
MITS 88-MDS software expects 35-track minidisks and controller behavior that differs from the 88-DCDD drive.
Both devices use ports 08h-0Ah, so one computer cannot connect both controllers simultaneously.

## Decision
Provide 88-MDS as a separate device plugin with its own image lifecycle and GUI. Model the original programmed-I/O
protocol, 35 tracks, 16 sectors per track, 137-byte raw sectors, automatic head loading, and ignored head-unload command.

## Consequences
Users choose either 88-MDS or 88-DCDD in a computer configuration. MDS images remain raw and interoperable with
SIMH; CPU-driven sector transfers preserve original timing semantics without adding a DMA service to emuLib.
