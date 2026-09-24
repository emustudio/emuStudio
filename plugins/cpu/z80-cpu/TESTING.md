# Z80 compatibility tests

The Gradle suite covers documented and undocumented instruction behavior,
flags, interrupts, prefix handling, timing side effects, I/O, disassembly, and
48K ZX Spectrum snapshot loading. Run it with:

```sh
./gradlew :plugins:cpu:z80-cpu:test
```

The following machine-code suites were used for the ZX Spectrum 48K acceptance
run. They completed without reported failures:

- `ccffrm.tap`, `fusetest.tap`, `minfo.tap`, `tc-modes.tap`
- `z80ccfscr.tap`, `z80doc.tap`, `z80memptr.tap`, `colour-panels.tap`
- `int_skip.tap`, `ptime.tap`, `ulatest3.tap`, `z80ccf.tap`
- `z80flags.tap`, `z80type.tap`, `floatspy.tap`, `keyboard.tap`
- `stime.tap`, `z80bltst.tap`, `z80docflags.tap`, `z80full.tap`

These external TAP tests are not redistributed and are not part of CI. Before a
release, run them with the `ZX Spectrum 48K` computer and record any regression
against this list. Timing tests should use the default 3500 kHz CPU frequency.

Snapshot parser tests cover uncompressed and compressed Z80 v1 files, extended
Z80 v2/v3 page blocks, SNA register and stack restoration, RAM, interrupt state,
and the ULA border value. Only 48K snapshots are supported.
