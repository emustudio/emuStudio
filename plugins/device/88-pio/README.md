# MITS 88-PIO

Mode-0 emulation of the Intel 8255-compatible parallel interface used by the MITS 88-PIO.
Ports A, B, C, and the control register are available at CPU ports `08h` through `0Bh`.

The plugin publishes three standard `DeviceContext<Byte>` endpoints, ordered A, B, C. A connected
device writes input pin values with `writeData` and reads the current port output with `readData`.
