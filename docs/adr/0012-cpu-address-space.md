# ADR-012: CPU owns debugger address-space bounds

## Status
Accepted

## Context
The debugger bounded instruction traversal with the connected memory plugin size. That assumption fails for banked or split memory and couples CPU disassembly to one memory implementation.

## Decision
The public emuLib CPU contract now requires `getAddressSpaceSize()`. The application constructs debugger pagination from this CPU value and no longer reacts to memory-size changes as address-space changes.

## Consequences
CPU plugins must declare their addressable instruction range. Debugger pagination works without a memory plugin and remains bounded for large or banked memories. This is an intentional plugin API break with no compatibility default.
