# Altair paper tape reader and punch

The device follows the SIMH Altair PTR/PTP convention:

- status port `12h`: bit 0 means reader data is available; bit 1 means the punch is ready;
- data port `13h`: reads consume the input tape and writes append to the output tape;
- the first read at end-of-tape returns CP/M end-of-file byte `1Ah`;
- writing `03h` to the status port clears the end-of-tape state.

`.pt`, Intel HEX, and binary files are byte streams. Intel HEX text is delivered unchanged for the guest loader.

The GUI follows Audio Tape Player: browse a directory of available tapes on the
left, with Reader and Punch tabs on the right. Load, rewind, or eject reader tapes
and watch byte progress. Save tape opens the punch output; Close output finishes
it. Opening punch output replaces existing file contents. Guest software controls
reading and punching; closing the GUI leaves attached tapes available.

Settings offers `showGuiAtStartup` (default `false`). Save persists it; ESC
discards the draft. CPU ports stay fixed at `12h` and `13h`.
