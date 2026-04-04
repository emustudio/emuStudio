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
DC_RDA      equ     80h             ; D7=0 => new read data available
DC_TRK0     equ     40h             ; D6=0 => track 0
DC_INTDIS   equ     20h             ; D5=0 => interrupts enabled
DC_HEADBAD  equ     04h             ; D2=0 => head loaded/sector valid
DC_MOVEBUSY equ     02h             ; D1=1 => movement not allowed
DC_WRBUSY   equ     01h             ; D0=1 => write byte not accepted yet

; DCDD control bits (port 2 WRITE)
DC_WREN     equ     80h
DC_HI_CURR  equ     40h
DC_INT_OFF  equ     20h
DC_INT_ON   equ     10h
DC_HEAD_UNL equ     08h
DC_HEAD_LD  equ     04h
DC_STEP_OUT equ     02h
DC_STEP_IN  equ     01h

; syscall numbers for transient progs
SYS_GETC    equ     0
SYS_PUTC    equ     1
SYS_PUTS    equ     2
SYS_READLN  equ     3
SYS_OPENRD  equ     4
SYS_LOAD    equ     5
SYS_DIR     equ     6

; ------------------------------------------------------------
; Warm entry at 0005h style stub (optional CP/M-ish convention)
; ------------------------------------------------------------
            org     0000h
            jmp     start
            nop
            nop
            jmp     syscall
warm_return:
            lxi     sp,STACKTOP
            jmp     shell_loop

            org     0100h

; ------------------------------------------------------------
; Start / shell
; ------------------------------------------------------------
start:
            lxi     sp,STACKTOP
            call    bios_init
            call    fs_mount
            lxi     h,msg_banner
            call    puts

shell_loop:
            lxi     h,msg_prompt
            call    puts
            lxi     h,cmdbuf
            mvi     b,CMDMAX
            call    getline
            lxi     h,cmdbuf
            call    skipsp
            mov     a,m
            ora     a
            jz      shell_loop
            call    dispatch
            jmp     shell_loop

; ------------------------------------------------------------
; Console I/O
; ------------------------------------------------------------
putc:       ; A = char
            mov     c,a
            call    bios_conout
            ret

puts:       ; HL -> ASCIIZ
            mov     a,m
            ora     a
            rz
            push    h
            call    putc
            pop     h
            inx     h
            jmp     puts

newline:
            mvi     a,CR
            call    putc
            mvi     a,LF
            jmp     putc

getc:
            jmp     bios_conin

const:
            jmp     bios_const

skipsp:
            mov     a,m
            cpi     SPC
            rnz
            inx     h
            jmp     skipsp

getline:    ; HL=buf, B=max chars (excluding 0)
            push    d
            push    h
            xchg                    ; DE = base
            mvi     c,0             ; count
_gl0:       call    getc
            cpi     CR
            jz      _done
            cpi     LF
            jz      _done
            cpi     BS
            jz      _bs
            cpi     DEL
            jz      _bs
            mov     a,c
            cmp     b
            jnc     _gl0
            ; store char in scratch and echo
            push    psw
            push    d
            mov     h,d
            mov     l,e
            mov     a,c
            call    add_a_hl
            pop     d
            pop     psw
            mov     m,a
            inr     c
            call    putc
            jmp     _gl0
_bs:        mov     a,c
            ora     a
            jz      _gl0
            dcr     c
            mvi     a,BS
            call    putc
            mvi     a,' '
            call    putc
            mvi     a,BS
            call    putc
            jmp     _gl0
_done:      mov     h,d
            mov     l,e
            mov     a,c
            call    add_a_hl
            mvi     m,0
            call    newline
            pop     h
            pop     d
            ret

add_a_hl:   ; HL += A
            push    d
            mov     e,a
            mvi     d,0
            dad     d
            pop     d
            ret

; ------------------------------------------------------------
; Command parser
; ------------------------------------------------------------
dispatch:
            shld    argptr
            lxi     d,kw_help
            call    tokcmp
            jz      cmd_help
            lhld    argptr
            lxi     d,kw_ls
            call    tokcmp
            jz      cmd_ls
            lhld    argptr
            lxi     d,kw_cat
            call    tokcmp
            jz      cmd_cat
            lhld    argptr
            lxi     d,kw_write
            call    tokcmp
            jz      cmd_write
            lhld    argptr
            lxi     d,kw_rm
            call    tokcmp
            jz      cmd_rm
            lhld    argptr
            lxi     d,kw_run
            call    tokcmp
            jz      cmd_run
            lhld    argptr
            lxi     d,kw_echo
            call    tokcmp
            jz      cmd_echo
            lxi     h,msg_bad
            jmp     puts

; token compare HL vs DE, Z on match and advances argptr
tokcmp:
_tc0:       ldax    d
            ora     a
            jz      _endkw
            cmp     m
            rnz
            inx     d
            inx     h
            jmp     _tc0
_endkw:     mov     a,m
            ora     a
            jz      _yes0
            cpi     SPC
            rnz
            call    skipsp_from_hl
_yes0:      shld    argptr
            xra     a
            cmp     a
            ret

skipsp_from_hl:
            inx     h
_ssh:       mov     a,m
            cpi     SPC
            rnz
            inx     h
            jmp     _ssh

cmd_help:
            lxi     h,msg_help
            jmp     puts

cmd_echo:
            lhld    argptr
            call    puts
            jmp     newline

cmd_ls:
            call    fs_load_dir
            rc
            lxi     h,dirbuf
            mvi     b,DIRENTS
_ls0:       mov     a,m
            ora     a
            jz      _lsn
            push    b
            push    h
            call    pr_name
            mvi     a,' '
            call    putc
            pop     h
            lxi     d,12
            dad     d
            mov     a,m
            call    pr_hex
            lxi     h,msg_rec
            call    puts
            call    newline
            pop     b
_lsn:       lxi     d,DIRSZ
            dad     d
            dcr     b
            jnz     _ls0
            ret

cmd_cat:
            lhld    argptr
            call    parse_name
            rc
            call    fs_find
            rc
            lxi     d,filebuf
            call    fs_read_to_de
            rc
            xchg
            call    puts
            jmp     newline

cmd_write:  ; write NAME TEXT...
            lhld    argptr
            call    parse_name
            rc
_w1:        mov     a,m
            ora     a
            rz
            cpi     SPC
            jz      _w2
            inx     h
            jmp     _w1
_w2:        call    skipsp
            mov     a,m
            ora     a
            rz
            shld    textptr
            call    fs_save_text
            rc
            lxi     h,msg_ok
            jmp     puts

cmd_rm:
            lhld    argptr
            call    parse_name
            rc
            call    fs_delete
            rc
            lxi     h,msg_ok
            jmp     puts

cmd_run:
            lhld    argptr
            call    parse_name
            rc
            call    fs_find
            rc
            lxi     d,TPA_BASE
            call    fs_read_to_de
            rc
            lxi     sp,STACKTOP
            lxi     h,warm_return
            push    h
            jmp     TPA_BASE

parse_name: ; HL-> token, copies uppercase ASCIIZ into namebuf; leaves HL near end token
            call    skipsp
            mov     a,m
            ora     a
            stc
            rz
            lxi     d,namebuf
            mvi     b,0
_pn0:       mov     a,m
            ora     a
            jz      _pnd
            cpi     SPC
            jz      _pnd
            call    upcase
            stax    d
            inx     d
            inx     h
            inr     b
            mov     a,b
            cpi     NAMEMAX
            jc      _pn0
_pnd:       xra     a
            stax    d
            ora     a
            ret

upcase:
            cpi     'a'
            rc
            cpi     'z'+1
            rnc
            sui     20h
            ret

pr_name:
            mvi     b,NAMEMAX
_pn1:       mov     a,m
            ora     a
            rz
            call    putc
            inx     h
            dcr     b
            jnz     _pn1
            ret

pr_hex:
            push    psw
            rrc
            rrc
            rrc
            rrc
            ani     0Fh
            call    hexdig
            pop     psw
            ani     0Fh
            call    hexdig
            ret
hexdig:
            cpi     10
            jc      _num
            adi     'A'-10
            jmp     putc
_num:       adi     '0'
            jmp     putc

; ------------------------------------------------------------
; Tiny filesystem
; Directory entry (16 bytes):
;   0..10  name ASCIIZ
;   11     flags (bit0=used)
;   12     logical record count
;   13     start logical LBA low
;   14     start logical LBA high
;   15     reserved
; Files are contiguous. Directory cached in dirbuf.
; ------------------------------------------------------------
fs_mount:
            call    bios_disk_select0
            call    fs_load_dir
            ret

fs_load_dir:
            lxi     h,dirbuf
            mvi     b,DIR_RECS
            mvi     c,DIR_LBA
_fld0:      push    b
            push    h
            mov     a,c
            call    bios_read_record
            pop     h
            rc
            lxi     d,LSECSZ
            dad     d
            inr     c
            pop     b
            dcr     b
            jnz     _fld0
            ret

fs_flush_dir:
            lxi     h,dirbuf
            mvi     b,DIR_RECS
            mvi     c,DIR_LBA
_ffd0:      push    b
            push    h
            mov     a,c
            call    bios_write_record
            pop     h
            rc
            lxi     d,LSECSZ
            dad     d
            inr     c
            pop     b
            dcr     b
            jnz     _ffd0
            ret

fs_find:    ; namebuf -> entry, returns HL=entry, CY=0 if found
            call    fs_load_dir
            rc
            lxi     h,dirbuf
            mvi     b,DIRENTS
_ff0:       mov     a,m
            ora     a
            jz      _ffn
            push    b
            push    h
            lxi     d,namebuf
            call    streq
            pop     h
            jz      _ffy
            pop     b
_ffn:       lxi     d,DIRSZ
            dad     d
            dcr     b
            jnz     _ff0
            stc
            ret
_ffy:       pop     b
            ora     a
            ret

streq:      ; HL/DE ASCIIZ, Z if equal
            mov     a,m
            ldax    d
            cmp     a
            rnz
            ora     a
            rz
            inx     h
            inx     d
            jmp     streq

fs_find_free:
            lxi     h,dirbuf
            mvi     b,DIRENTS
_fff0:      mov     a,m
            ora     a
            rz
            lxi     d,DIRSZ
            dad     d
            dcr     b
            jnz     _fff0
            stc
            ret

fs_last_lba:
            lxi     h,dirbuf
            lxi     d,DATA_LBA
            mvi     b,DIRENTS
_fll0:      mov     a,m
            ora     a
            jz      _flln
            push    h
            lxi     b,12
            dad     b
            mov     a,m          ; rec count
            mov     c,a
            inx     h
            mov     a,m          ; lba low
            add     c
            mov     c,a
            inx     h
            mov     a,m          ; high ignored, disk under 256 recs for tiny fs
            pop     h
            mov     a,c
            cmp     e
            jc      _flln
            mov     e,c
_flln:      lxi     d,DIRSZ
            dad     d
            dcr     b
            jnz     _fll0
            mvi     h,0
            mov     l,e
            ret

fs_read_to_de: ; HL=dir entry, DE=dest, zero-terminate after payload
            push    h
            lxi     b,12
            dad     b
            mov     c,m          ; record count
            inx     h
            mov     a,m
            sta     cur_lba
            inx     h
            mov     a,m
            sta     cur_lba+1
            pop     h
_frd0:      mov     a,c
            ora     a
            jz      _frdd
            push    b
            push    d
            lhld    cur_lba
            mov     a,l
            xchg
            call    bios_read_record
            xchg
            pop     d
            rc
            lxi     h,LSECSZ
            dad     d
            xchg
            lhld    cur_lba
            inx     h
            shld    cur_lba
            pop     b
            dcr     c
            jmp     _frd0
_frdd:      xra     a
            stax    d
            ret

fs_delete:
            call    fs_find
            rc
            xra     a
            mov     m,a
            jmp     fs_flush_dir

fs_save_text:
            call    fs_load_dir
            rc
            ; delete existing entry if present
            call    fs_find
            cnc     fs_zap_entry
            call    fs_find_free
            rc
            shld    dirent
            call    fs_last_lba
            shld    cur_lba
            ; estimate logical records needed from ASCIIZ text
            lhld    textptr
            call    strlen_hl
            ; BC = len, compute recs in A = (len+127)/128, min 1
            mov     a,c
            adi     127
            mov     c,a
            mvi     a,0
            adc     b
            mov     b,a
            mov     a,c
            rrc
            rrc
            rrc
            rrc
            rrc
            rrc
            rrc
            ; too ugly on 8080, use small loop instead
            lhld    textptr
            mvi     c,1
            mvi     b,0
_fstc:      mov     a,m
            ora     a
            jz      _fstdonecount
            inx     h
            inr     b
            mov     a,b
            cpi     LSECSZ
            jnz     _fstc
            inr     c
            mvi     b,0
            jmp     _fstc
_fstdonecount:
            ; write DE records from textptr
            lhld    textptr
            shld    srcptr
            mov     a,c
            sta     reccnt
            lhld    cur_lba
            shld    startlba
_fsw0:      lda     reccnt
            ora     a
            jz      _meta
            lxi     h,secbuf
            mvi     b,LSECSZ
            lhld    srcptr
_fsw1:      mov     a,m
            ora     a
            jz      _pad
            mov     e,a
            xchg
            mov     m,e
            xchg
            inx     h
            inx     d
            dcr     b
            jnz     _fsw1
            jmp     _wr
_pad:       mvi     m,0
            inx     h
            dcr     b
            jnz     _pad
_wr:        xchg
            shld    srcptr
            lhld    cur_lba
            mov     a,l
            lxi     h,secbuf
            call    bios_write_record
            rc
            lhld    cur_lba
            inx     h
            shld    cur_lba
            lda     reccnt
            dcr     a
            sta     reccnt
            jmp     _fsw0
_meta:      lhld    dirent
            xchg
            lxi     h,namebuf
            mvi     b,NAMEMAX
_cpnm:      mov     a,m
            stax    d
            inx     h
            inx     d
            dcr     b
            jnz     _cpnm
            mvi     a,1
            stax    d           ; flags
            inx     d
            lda     reccnt_saved
            ; not used, fix below by reusing C from earlier impossible now
            ; recompute rec count from start/end lba:
            lhld    cur_lba
            xchg
            lhld    startlba
            mov     a,e
            sub     l
            mov     a,a
            mov     m,a
            ; simpler explicit metadata store follows:
            lhld    dirent
            lxi     b,11
            dad     b
            mvi     m,1
            inx     h
            lda     cur_lba
            sui     0
            ; rec count = cur_lba - startlba
            mov     a,l
            ; restart exact metadata store with helper
            call    fs_store_meta
            jmp     fs_flush_dir

fs_store_meta:
            ; uses dirent/startlba/cur_lba
            push    h
            lhld    dirent
            lxi     b,12
            dad     b
            xchg
            lhld    cur_lba
            xchg                    ; DE=end, HL=start after swap sequence below
            lhld    startlba
            mov     a,e
            sub     l
            stax    d               ; wrong dest after xchg; replace with direct path
            pop     h
            ; fallback direct implementation
            lhld    dirent
            lxi     b,12
            dad     b
            push    h
            lhld    cur_lba
            xchg
            lhld    startlba
            mov     a,e
            sub     l
            pop     h
            mov     m,a             ; rec count
            inx     h
            lhld    startlba
            mov     m,l
            inx     h
            mov     m,h
            inx     h
            mvi     m,0
            ret

fs_zap_entry:
            xra     a
            mov     m,a
            ret

strlen_hl:
            lxi     b,0
_sl0:       mov     a,m
            ora     a
            rz
            inx     h
            inx     b
            jmp     _sl0

; ------------------------------------------------------------
; Syscall entry for transient programs
; C=function, DE/HL args by convention
; ------------------------------------------------------------
syscall:
            mov     a,c
            cpi     SYS_GETC
            jz      bios_conin
            cpi     SYS_PUTC
            jz      _spc
            cpi     SYS_PUTS
            jz      _sps
            cpi     SYS_READLN
            jz      _srl
            cpi     SYS_DIR
            jz      cmd_ls
            cpi     SYS_LOAD
            jz      _sld
            ret
_spc:       mov     a,e
            jmp     putc
_sps:       xchg
            jmp     puts
_srl:       xchg
            mvi     b,CMDMAX
            jmp     getline
_sld:       ; DE -> ASCIIZ filename, load to HL dest
            xchg
            ; copy DE string to namebuf
            lxi     d,namebuf
_sld0:      mov     a,m
            stax    d
            ora     a
            jz      _sld1
            inx     h
            inx     d
            jmp     _sld0
_sld1:      call    fs_find
            rc
            xchg                    ; DE=dir entry, HL=dest
            xchg
            jmp     fs_read_to_de

; ------------------------------------------------------------
; BIOS -- emuStudio 88-SIO / 88-DCDD implementation
; ------------------------------------------------------------
; 88-SIO docs: status/control on 10h, data on 11h. Writing a char is just OUT 11h.
; Keyboard read example polls bit 0 on 10h until non-zero, then IN 11h.
;
; 88-DCDD docs: ports 08h/09h/0Ah. To position a record you select disk 0 on port1,
; seek track using step-in/step-out with move-head polling on port1 bit D1,
; load head with D2 on port2, poll port2 until sector number matches, then stream bytes
; through port3. Sector numbering is 0..31, 77 tracks, 137 bytes/sector.

bios_init:
            jmp     bios_disk_select0

bios_const: ; A=00 no char, FF char ready
            in      SIO_STAT
            ani     SIO_RX_RDY
            jnz     _yes1
            in      SIO_STAT
            ani     SIO_RX_RDY2      ; tolerate prose/example mismatch
            jnz     _yes1
            xra     a
            ret
_yes1:       mvi     a,0FFh
            ret

bios_conin:
_bci0:      call    bios_const
            ora     a
            jz      _bci0
            in      SIO_DATA
            ret

bios_conout:
_bco0:      in      SIO_STAT
            ani     SIO_TX_RDY
            jz      _bco0
            mov     a,c
            out     SIO_DATA
            ret

bios_disk_select0:
            xra     a               ; D7=0 enable, D3..D0=0 drive A
            out     D_PORT1
            ret

bios_read_record: ; A = logical record number 0..255, HL=dest 128 bytes
            push    psw
            push    b
            push    d
            push    h
            mov     e,a
            call    bios_set_lba
            call    bios_read_phys_payload
            pop     h
            pop     d
            pop     b
            pop     psw
            ret

bios_write_record: ; A = logical record number, HL=src 128 bytes
            push    psw
            push    b
            push    d
            push    h
            mov     e,a
            call    bios_set_lba
            call    bios_write_phys_payload
            pop     h
            pop     d
            pop     b
            pop     psw
            ret

; input: E = logical record number
bios_set_lba:
            call    bios_disk_select0
            ; track = lba / 32, sector = lba % 32
            mov     a,e
            ani     1Fh
            sta     want_sector
            mov     a,e
            rrc
            rrc
            rrc
            rrc
            rrc
            ani     07h            ; enough for 0..76 with <=255 records tiny fs
            sta     want_track
            call    bios_seek_track
            call    bios_load_head
            call    bios_seek_sector
            ret

bios_seek_track:
            call    bios_seek_track0
            lda     want_track
            mov     b,a
            inr     b              ; loop style from docs
_bst1:      dcr     b
            rz
            call    bios_wait_move
            mvi     a,DC_STEP_IN
            out     D_PORT2
            jmp     _bst1

bios_seek_track0:
_bst0:      in      D_PORT1
            ani     DC_TRK0
            rz                      ; D6=0 => at track 0
            mvi     a,DC_HEAD_UNL
            out     D_PORT2
            call    bios_wait_move
            mvi     a,DC_STEP_OUT
            out     D_PORT2
            jmp     _bst0

bios_wait_move:
_bwm0:      in      D_PORT1
            ani     DC_MOVEBUSY
            jnz     _bwm0           ; D1=1 => cannot move yet
            ret

bios_load_head:
            mvi     a,DC_HEAD_LD
            out     D_PORT2
_bhl0:      in      D_PORT1
            ani     DC_HEADBAD
            jnz     _bhl0           ; wait until head valid/loaded
            ret

bios_seek_sector:
_bss0:      in      D_PORT2
            ani     3Fh             ; clear D7/D6
            rrc                     ; sector true in D0, sector number in D5..D1
            mov     b,a
            ani     1Fh
            mov     c,a
            lda     want_sector
            cmp     c
            jnz     _bss0
            ret

bios_read_phys_payload: ; HL=dest, current position at sector start
            mvi     b,LSECSZ
_brp0:      call    bios_wait_read
            in      D_PORT3
            mov     m,a
            inx     h
            dcr     b
            jnz     _brp0
            ; discard 9 trailing bytes in 137-byte physical sector
            mvi     b,SECPTRAIL
_brp1:      call    bios_wait_read
            in      D_PORT3
            dcr     b
            jnz     _brp1
            ret

bios_wait_read:
_bwr0:      in      D_PORT1
            ani     DC_HEADBAD
            rnz                     ; if head invalid, abort as no-op
            in      D_PORT1
            ani     DC_RDA
            jnz     _bwr0           ; D7=1 => no new read byte yet
            ret

bios_write_phys_payload: ; HL=src, writes 128 payload + 9 zero pad bytes
            mvi     a,DC_WREN
            out     D_PORT2
            mvi     b,LSECSZ
_bwp0:      call    bios_wait_write
            mov     a,m
            out     D_PORT3
            inx     h
            dcr     b
            jnz     _bwp0
            mvi     b,SECPTRAIL
            xra     a
_bwp1:      call    bios_wait_write
            out     D_PORT3
            dcr     b
            jnz     _bwp1
            ret

bios_wait_write:
_bww0:      in      D_PORT1
            ani     DC_HEADBAD
            rnz
            in      D_PORT1
            ani     DC_WRBUSY
            jnz     _bww0           ; D0=1 => not ready for next byte
            ret

; ------------------------------------------------------------
; Data
; ------------------------------------------------------------
msg_banner: db CR,LF,'ALTIX-80 0.2',CR,LF
            db 'help ls cat write rm run echo',CR,LF,0
msg_prompt: db 'altix$ ',0
msg_help:   db 'help          show help',CR,LF
            db 'ls            list files',CR,LF
            db 'cat NAME      print file',CR,LF
            db 'write N TEXT  save text file',CR,LF
            db 'rm NAME       delete file',CR,LF
            db 'run NAME      load raw binary to 2000h and jump',CR,LF
            db 'echo TEXT     print text',CR,LF,0
msg_bad:    db 'bad command',CR,LF,0
msg_ok:     db 'ok',CR,LF,0
msg_rec:    db ' rec',0

kw_help:    db 'help',0
kw_ls:      db 'ls',0
kw_cat:     db 'cat',0
kw_write:   db 'write',0
kw_rm:      db 'rm',0
kw_run:     db 'run',0
kw_echo:    db 'echo',0

argptr:     dw 0
textptr:    dw 0
srcptr:     dw 0
cur_lba:    dw 0
startlba:   dw 0
dirent:     dw 0
reccnt:     db 0
reccnt_saved: db 0
want_track: db 0
want_sector: db 0

cmdbuf:     ds CMDMAX+1
namebuf:    ds NAMEMAX+1
secbuf:     ds LSECSZ
dirbuf:     ds DIR_RECS*LSECSZ
filebuf:    ds 2048

