---
layout: default
title: Device 88-disk
nav_order: 6
parent: MITS Altair8800
permalink: /altair8800/88-disk
---

# Disk controller "88-disk"

Altair Disk offered the advantage of fixed memory including relatively fast access to data. The speed of data transfer was 250 Kb/s (The plugin does not emulate this). Disk was connected with disk controller (or board), and the data were transferred in serial fashion, bit after bit.

Disk controller, on the other hand communicated with CPU. It transformed these serial data into 8-bit words which were stored/read by CPU into/from operating memory.

MITS 88-DISK offered to connect up to 16 disk devices (one can be seen in the front image in the Introduction section).

Original manual can be downloaded at [this link][manual]{:target="_blank"}.

## Features

A plugin emulates basic functionality of the whole disk system for Altair 8800 computer. It is not only disk controller, but also the disk drive.

The features include:

- allows to mount up to 16 disk images
- CPU ports can be set manually
- images can be saved for automatic mount at startup
- GUI

Interrupts are not supported yet.

## Mounting disk images

In order to mount DISK images to the device, please open device settings:

![Settings window of 88-DISK]({{ site.baseurl }}/assets/altair8800/88-disk-01.png)

- *A*: Select drive (A - P)
- *B*: Choosing the image file
- *C*: Set sectors count and sector length for the current drive. (NOTE: Be cautious with the settings. Incorrect values can result in disk image file damage. Default values are used for classic Altair8800 image files used by [simh][simh]{:target="_blank"}).
- *D*: Set default values for sector count and sector length for the current drive.
- *E*: Mount/unmount the image file onto/from the selected drive. Mount operation: If there is any disk mounted already, the new image will be re-mounted.
- *F*: Check box for saving the settings into the computer configuration file. If checked, the settings will be loaded after start.

## CPU Ports settings

MITS 88-DISK board communicates with CPU using its ports. There are three ports overall, each for different function. By default, the ports used by 88-DISK are:

- port 1: `0x08`
- port 2: `0x09`
- port 3: `0x0A`

These numbers can be changed in the Settings window, tab "CPU Ports":

![Setting CPU ports]({{ site.baseurl }}/assets/altair8800/88-disk-02.png)

## Configuration file

The following table shows all the possible settings of MITS 88-DISK plugin:

|---
|Name              | Default value | Valid values         | Description
|-|-|-|-
|`port1CPU`        | 0x08          | > 0 and < 256        | Number of Port 1
|`port2CPU`        | 0x09          | > 0 and < 256        | Number of Port 2
|`port3CPU`        | 0x0A          | > 0 and < 256        | Number of Port 3
|`sectorsPerTrack` | 32            | > 0                  | Count of sectors in a disk image
|`sectorLength`    | 137           | > 0                  | Size of one sector in bytes
|`image0`          | N/A           | Path to existing file| File name to mount on disk A (0)
| ...              | ...           | ...                  | ...
|`image15`         | N/A           | Path to existing file| File name to mount on disk P (15)
|---

## Programming

Data are written onto or read from disk in a serial fashion. The position in the floppy disk is uniquely set by the track number, sector number and the offset in the sector. It is rudimentary to know how many tracks are available, so as how many sectors per track and the sector size.

In Altair8800, drive `Pertec FD400` used 8" diskettes. Each had 77 tracks. The track had 32 sectors with 137 bytes long. Capacity was therefore `77 * 32 * 137 = 337568 B = 330 kB`. Software used less capacity, because 9 bytes from each sector were used for the integrity checksum.

### Setting the position

Track number and sector number can be set only incrementally, not directly. Setting the offset within the sector is more challenging.

After track and sector were set, programmer must "poll" the status port which tells him when the disk position is set to the beginning of the sector. Then, programmer must read data until he gets to the position where he wanted.

### CPU Ports

Controller communicates with CPU using three I/O ports at addresses (by default) 0x08, 0x09 and 0x0A. The following table shows the CPU ports and how they are used.

|---
|Port     | Address   | Input                      | Output
|-|-|-|-
|1        | `0x08`    | Disk and controller status | Select disk
|2        | `0x09`    | Get number of sector       | Disk settings
|3        | `0x0A`    | Read data                  | Write data
|---

Now, detailed description of the ports follow. Bits are ordered in a byte as follows:

    D7 D6 D5 D4 D3 D2 D1 D0

where `D7` is the most significant bit, and `D0` the least significant bit.

#### Port 1 (default address: 0x08)

*WRITE*:

Selects and enables one of 16 disk devices. By selecting a drive, all further operations
will be performed on that drive. If the disk has not mounted any disk image, all further operations will be ignored.
The previously selected device will be disabled.

- `D7`         : if the value is 1, disable the drive. If the value is 0, select and enable the drive.
- `D6 D5 D4`   : unused bits
- `D3 D2 D1 D0`: index of the drive to be selected. From 0-15.

*READ*:

Read disk status of the selected drive.

- `D7` : _New read data available_. Indicates if there is at least 1 byte available for reading from Port 3 (value=0). It will be reset after data are read (value=1). If the value is 1, data read from Port 3 will be invalid or no new data is available.
- `D6` : _Track 0_. Indicates if the head is positioned at track 0 (value=0).
- `D5` : _Interrupt Enabled_. Indicates if interrupts are used (value=0). The plugin does not support interrupts, therefore the value will be always 1.
- `D4 D3` : Unused bits; they are always 0.
- `D2` : _Head Status_. Indicates the correctness of the head setting. If the value is 0, reading sector number from Port 2 will be valid.
- `D1` : _Move head_. Indicates if the movement of the disk head is allowed. If the value is 1, all track number changes will be ignored.
- `D0` : _Enter new write data_. Indicates if the device is ready for writing data. If the value is 1, all written data will be ignored.

Initial values of the bits are: `11100111`.

#### Port 2 (default address: 0x09)

*WRITE*:

Control the disk head, and other settings if a disk drive is selected.

- `D7` : _Write Enable_. Initializes write sequence (enables writing to the disk; value=1). The plugin sets the sector number to 0 and also value 0 to bit `D0` of Port 1 (_Enter new write data_). According to manual the write sequence holds only for short time, maximally until the end of sector is reached. The plugin does not limit the sequence period, it is deactivated only when the end of the sector is reached. In addition each first byte and the last byte of a sector should have set its MSB (7th bit) to 1. It was called the "sync bit" for easier identification of start or end of a sector. However, the plugin does not require it. 
- `D6` : _Head Current Switch_. On real disks the bit should be set to 1 when a program is writting data to tracks from 43-76. The plugin the bit is ignored.
- `D5` : _Interrupt Disable_. Setting is ignored sicne plugin does not support interrupts.
- `D4` : _Interrupt Enable_. Setting is ignored sicne plugin does not support interrupts.
- `D3` : _Head unload_. Removes head from the disk surface. Reading sector number will now become invalid. In addition, value of bit `D7` from Port 1 (_New read data available_) become 1 (no new data).
- `D2` : _Head load_. Sets the disk head onto disk surface. Reading sector number now becomes valid. If additionally the bit `D7` from Port 1 (_New data available_) is set, it is possible to read data from the disk.
- `D1` : _Step Out_. Move the disk head back by 1 track (the track number is decremented). It is required to check bit `D1` of Port 1 (_Move head_) to have value 0.
- `D0` : _Step In_. Move the disk head ahead by 1 track (the track number is incremented). It is required to check bit `D1` of Port 1 (_Move head_) to have value 0.

*READ*:

Reads the number of the sector. The value can be read only if a disk drive is selected and the disk head is positioned at the disk surface (by setting the bit `D2`).

- `D7 D6` : Unused bits; they are always 1.
- `D5 D4 D3 D2 D1`: Number of the sector, counted from 0.
- `D0` : _Sector True_. If the value is 0, the offset in sector is 0. According to manual, the bit is set for maximum 30 microseconds. Programs could detect the bit set and quickly start writing data until the _Sector true_ came back again. It could be made in time easily, because CPU was much faster than disk itself. plugin does not limit the period. The value is 0 practically all the time, until first byte is written.

#### Port 3 (default address: 0x0A)

*WRITE*:

Write a byte to disk. In order to perform valid write, the _Write Enable_ `D7` bit of Port 2 must be set to 1. Before data are written to disk, it is required to check bit `D0` from Port 1 (_Enter new write data_).

*READ*:

Read a byte from disk. In order to perform valid read, the _Head load_ `D2` bit of Port 2 must be set to 1. Only if bit `D7` from Port 1 (_New read data available_) is set to 0, the read data are valid.

### Program example

In this section, an example is presented showing how to read/write data from/to the floppy disk. At first, it writes one byte (letter `A` with ASCII value 65) to track 1, sector 18 and offset 20. Then, it reads the byte to operating memory at address 0x200.

The program uses 3 procedures (in assembler for Intel 8080) for setting the disk position (`ltrack` for loading the track number, `lsector` for loading the sector number, and `loffset` for loading the offset within the sector) and two more for data reading (`read`) and writing (`write`).


```
disk0  equ 0    ; disk number
track  equ 1    ; track number
sector equ 18   ; sector number
offset equ 20   ; offset within the sector
data   equ 'A'  ; data for writing

dcx sp          ; set stack register to 0xFFFF

mvi a, disk0    ; select disk
out 08h

call ltrack     ; set track number

call we         ; set 'write enable' sequence
call lsector    ; set sector number
call loffset    ; set sector offset
call write      ; write data

call lsector    ; set sector number (for clearing the offset)
call loffset    ; set sector offset
call read       ; read data

lxi h, readdata ; load address for reading the data
mov m, a        ; move the data there

hlt             ; end

ltrack0:        ; the procedure will set track number to 0
in 08h          ; read disk status
ani 1000000b    ; track 0 ?
rz              ; yes, return
mvi a, 1000b    ; head unload
out 09h
call movetrk    ; wait until the disk head can be moved
mvi a, 10b      ; step out, decrement track number
out 08h
jmp ltrack0

ltrack:         ; procedure sets a track number
call ltrack0    ; at first, set track number to 0
mvi b, track+1  ; b = track + 1
stepin:         ; stepin: {
dcr b           ;   b--;
rz              ;   if (b == 0) return;
call movetrk    ;   wait until the disk head can be moved
mvi a, 1        ;   step in, increment track number
out 09h
jmp stepin      ;   goto stepin;
                ; }

movetrk:        ; procedure waits until the disk head can be moved
in 08h          ; read disk status
ani 10b         ; can the disk head be moved?
jnz movetrk     ; nope, try again...
ret             ; yes, return

lsector:        ; procedure sets a sector number
mvi a, 100b     ; head load
out 09h
waits:
in 09h          ; read sector number
ani 3Fh         ; clear unused bits
rrc
cpi sector      ; is the number what is requested?
jnz waits       ; nope, try again
ret             ; yes, return

loffset:        ; procedure sets a sector offset
mvi b, offset+1 ; b = offset + 1
stepoff:        ; stepoff: {
dcr b           ;   b--;
rz              ;   if (b == 0) return;
call read       ;   read data; the offset is incremented
jmp stepoff     ;   goto stepoff;
                ; }

read:           ; procedure reads data from the disk
in 08h          ; read disk status
ani 100b        ; check if the disk head is loaded on the disk surface
rnz             ; if not, return
waitr:
in 08h          ; read disk status
ani 10000000b   ; New read data available ?
jnz waitr       ; nope, try again...
in 0Ah          ; yes, read data
ret             ; return

we:             ; procedure enables 'write enable' sequence
mvi a, 10000000b ; write enable
out 09h
ret

write:          ; procedure writes data to the disk
in 08h          ; read disk status
ani 100b        ; check if the disk head is loaded on the disk surface
rnz             ; if not, return
waitw:
in 08h          ; read disk status
ani 1           ; enter new write data ?
jnz waitw       ; nope, try again...
mvi a, data     ; yes, write data
out 0Ah
ret

org 200h
readdata: db 0
```


[manual]: http://www.virtualaltair.com/virtualaltair.com/PDF/88dsk%20manual%20v2.pdf
[simh]: http://simh.trailing-edge.com/
