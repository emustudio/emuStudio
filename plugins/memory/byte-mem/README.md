# byte-mem

This project is emulator of "byte" operating memory for various machines (memory operating on byte cells). It is part
of [emuStudio](https://www.emustudio.net/).

The official documentation can be
found [here](https://www.emustudio.net/docuser/mits_altair_8800/index/#operating-memory-code-standard-mem-code)

`size` accepts bytes as an integer or string. String values can use `K`/`k` for
KiB and `M`/`m` for MiB, for example `size = "64K"`. Legacy `memorySize`
settings remain supported.
