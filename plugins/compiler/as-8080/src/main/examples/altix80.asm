; ALTIX-80 -- compact Unix-like hobby OS for Altair 8800 / emuStudio
; Intel 8080, 64 KiB RAM, 88-SIO, ADM-3A, 88-DCDD
;
; What changed in this revision:
; - BIOS now follows the documented emuStudio 88-SIO / 88-DCDD port model.
; - Kernel is smaller and cleaner than the first draft.
; - Added transient program loading: `run NAME`.
; - File system simplified into fixed directory + contiguous extents.
; - Disk I/O uses 128-byte logical records stored in 137-byte physical sectors
;   (first 128 bytes carry payload, trailing 9 bytes are ignored/padded).
;
; IMPORTANT
; - This is written to the emuStudio documentation model, not to unknown real-world
;   Altair clone quirks. Port assumptions: SIO status/data = 10h/11h, DCDD = 08h/09h/0Ah.
; - 88-SIO documentation has one inconsistency: the bit description says received data
;   availability is on D1, but the example code polls D0. The emulator examples for
;   reading keyboard input poll D0, so bios_const uses D0 for compatibility.
; - The program is intentionally tiny and CP/M-like: single task, single address space,
;   flat namespace, no protection, no subdirectories, no true exec format.
;
; ------------------------------------------------------------
; Memory map
; ------------------------------------------------------------
; 0000h-00FFh  vectors / scratch / warm entry / transient return stub
; 0100h-17FFh  kernel + shell + BIOS + tiny FS
; 1800h-1FFFh  directory cache + sector buffer
; 2000h-DFFFh  transient program area (TPA)
; E000h-EFFFh  file buffer / work area
; F000h-FFFFh  stack
;
; ------------------------------------------------------------
; Build assumptions
; ------------------------------------------------------------
; - ORG 0100h so it can be loaded similarly to CP/M transient code.
; - Cold boot / monitor is expected to jump to START.
; - Transient program contract for RUN:
;     * image is loaded at TPA_BASE (2000h)
;     * image must be raw 8080 binary beginning at its own entry point
;     * program returns to shell with RET (stack preloaded with warm_return)
;
            org     0100h

; ------------------------------------------------------------
; Constants
; ------------------------------------------------------------
CR          equ     0Dh
LF          equ     0Ah
BS          equ     08h
DEL         equ     7Fh
SPC         equ     20h

CMDMAX      equ     63
NAMEMAX     equ     11              ; 8.3-ish but stored as plain ASCIIZ
DIRSZ       equ     16
DIRENTS     equ     32
LSECSZ      equ     128             ; logical record size used by OS
PSECSZ      equ     137             ; physical disk sector size on Altair disk
SECPTRAIL   equ     9               ; unused/padded bytes per physical sector

DIR_LBA     equ     1               ; logical records 1..4 hold directory
DIR_RECS    equ     4
DATA_LBA    equ     5
TPA_BASE    equ     2000h
STACKTOP    equ     0FFF0h

; DCDD ports per emuStudio docs
D_PORT1     equ     08h             ; status / select drive
D_PORT2     equ     09h             ; sector / disk controls
D_PORT3     equ     0Ah             ; read/write data

; SIO ports per emuStudio docs
SIO_STAT    equ     10h
SIO_DATA    equ     11h

; 88-SIO status masks
SIO_RX_RDY  equ     01h             ; doc example polls bit 0 for getchar
SIO_TX_RDY  equ     01h             ; doc says D0 always ready for CPU write
SIO_RX_RDY2 equ     02h             ; doc prose says D1 = data available

; DCDD status masks (port 1 READ)
            end start
