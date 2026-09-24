# ADR-017: Multiple source editor tabs

## Status
Accepted

## Context
One `REditor` replaced its file whenever another source was opened. Multi-file programs need independent dirty state,
navigation, and session restoration.

## Decision
Add an application-internal `TabbedEditor` implementing the existing `Editor` port. It owns one `REditor` per tab and
delegates editor commands to the active tab. Persist only open file paths in the computer TOML as `openSourceFiles`.

## Consequences
Compiler and emulator actions keep using the stable `Editor` boundary. Each tab retains its own buffer and dirty state.
Moving a computer configuration can leave stored absolute paths unavailable; missing files are skipped on restoration.
