# Altair paper tape reader and punch

The device follows the SIMH Altair PTR/PTP convention:

- status port `12h`: bit 0 means reader data is available; bit 1 means the punch is ready;
- data port `13h`: reads consume the input tape and writes append to the output tape;
- the first read at end-of-tape returns CP/M end-of-file byte `1Ah`;
- writing `03h` to the status port clears the end-of-tape state.

`.pt`, Intel HEX, and binary files are byte streams. Intel HEX text is delivered unchanged for the guest loader.
