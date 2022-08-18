;==============================================================
;= CDBL - Combo Disk Boot Loader ROM                          =
;= For the Altair 88-DCDD 8" disk system and                  =
;= the Altair 88-MDS Minidisk system                          =
;= By Martin Eberhard                                         =
;=                                                            =
;= CDBL loads software (e.g. Altair Disk BASIC) from an       =
;= Altair 88-DCDD 8" disk or an 88-MDS 5-1/4" minidisk,       =
;= automatically detecting which kind of drive is attached.   =
;==============================================================
;= NOTES                                                      =
;=                                                            =
;= Like DBL and MDBL, CDBL fits in one (256-byte) 1702A       =
;= EPROM and execution begins at address FF00h. However,      =
;= since version 3.00, the PROM is position independent       =
;= and can run at most any 256 byte boundary.                 =
;=                                                            =
;= Because of the slow 1702A EPROM access time, and because   =
;= some versions of MITS's 8800b Turnkey Module disable PROM  =
;= when any IN instruction is executed, CDBL copies itself    =
;= into RAM at 4C00h (RAMADR), and executes there. CDBL       =
;= therefore requres 512 bytes of 0-wait state RAM, starting  =
;= at RAMADR.                                                 =
;=                                                            =
;= Minidisks have 16 sectors/track, numbered 0 through 15.    =
;= 8" disks have 32 sectors/track, numbered 0 through 31.     =
;= CDBL figures out which kind of disk drive is attached,     =
;= based on the existance of sector number 16.                =
;=                                                            =
;= ALTAIR DISK SECTOR FORMAT (FOR BOOT SECTORS)               =
;=                                                            =
;= BYTE(s) FUNCTION BUFFER ADDRESS                            =
;= 0 Track number+80h (sync) RAMADR+7Bh                       =
;= 1 File size low byte RAMADR+7Ch                            =
;= 2 File size High Byte RAMADR+7Dh                           =
;= 3-130 Sector data RAMADR+7Eh to RAMADR+FDh                 =
;= 131 Marker Byte (0FFh) RAMADR+FEh                          =
;= 132 Checksum RAMADR+FFh                                    =
;= 133-136 Spare not read                                     =
;=                                                            =
;= Each sector header contains a 16-bit file-size value:      =
;= this many bytes (rounded up to an exact sector) are read   =
;= from the disk and written to RAM, starting at address 0.   =
;= When done (assuming no errors), CDBL then jumps to         =
;= address 0 (DMAADR) to execute the loaded code.             =
;=                                                            =
;= Sectors are interleaved 2:1. CDBL reads the even sectors   =
;= on each track first (starting with track 0, sector 0)      =
;= followed by the odd sectors (starting with sector 1),      =
;= continuing through the interleaved sectors of each track   =
;= until the specified number of bytes have been read.        =
;=                                                            =
;= CDBL first reads each sector (including the actual data    =
;= payload, as well as the 3 header and the first 2 trailer   =
;= bytes) from disk into the RAM buffer (SECBUF). Next, CDBL  =
;= checks to see if this sector would overwrite the RAM       =
;= portion of CDBL, and aborts with an 'O' error if so. It    =
;= then copies the data payload portion from the buffer to    =
;= its final RAM location, calculating the checksum along the =
;= way. During the copy, each byte is read back, to verify    =
;= correct writes. Any write-verify failure will immediately  =
;= abort the load with an 'M' error.                          =
;=                                                            =
;= Any disk read error (a checksum error or an incorrect      =
;= marker byte) will cause a retry of that sector read. After =
;= 16 retries on the same sector, CDBL will abort the load    =
;= with a 'C' error.                                          =
;=                                                            =
;= If the load aborts with any error, then CDBL will turn on  =
;= the INTE LED on the front panel (as an indicator), write   =
;= the error code ('C', 'M', or 'O') to RAM address 0, write  =
;= the offending memory address to RAM addresses 1 and 2, and =
;= then hang forever in a loop, printing the error code to    =
;= all known Altair Terminal output ports.                    =
;==============================================================
;= REVISION HISTORY                                           =
;=                                                            =
;= 1.00  05May2014  M.Eberhard                                =
;=   Combined MDBL and DBLme code, with assembly options to   =
;=   create exactly both of these boot loaders                =
;= 2.00  08May2014  M.Eberhard                                =
;=   Restructure and squeeze the code. Automatic 8"/Minidisk  =
;=   detection. Select boot disk from the sense switches.     =
;=   Improve track 0 seek by waiting for -MVHEAD before       =
;=   testing TRACK0. Detect memory overrun errors. Verify     =
;=   copy of CDBL code into RAM.                              =
;= 2.01  15May2014  M.Eberhard                                =
;=   Step in once before seeking track 0. Restart 6.4 Sec     =
;=   motor shutoff timer on retries.                          =
;= 2.02  04Jun2014  M. Eberhard                               =
;=   Eliminate booting from other than drive 0 because Basic  =
;=   and Burcon CP/M just load a 2-sector boot loader, and    =
;=   that boot loader loads the rest, always from drive 0.    =
;= 2.03  17Jan2015  M. Douglas                                =
;=   Force 43ms minimum delay when changing seek direction    =
;=   to meet/exceed 8" drive requirements.                    =
;= 2.04  12Mar2015  M. Douglas                                =
;=   Change 2SIO init constant (ACINIT) from 21h (7E2, xmit   =
;=   interrupts on) to 11h (8N2)                              =
;= 2.05  11Jan2016  M. Eberhard                               =
;=   No IN or OUT instructions until code is moved to RAM     =
;=   (for compatibility with some Turnkey Modules). This      =
;=   increased the RAM footprint from 256 to 512 bytes. Also  =
;=   fixed a bug when reporting overlay errors.               =
;= 3.00  12Jan2016  M. Douglas                                =
;=   Make the PROM position independent by making the         =
;=   routine that copies PROM to RAM position independent.    =
;==============================================================

;-----------------
; Disk Parameters
;-----------------
BPS equ 128 ;data bytes/sector
MDSPT equ 16 ;Minidisk sectors/track

;this code assumes SPT for 8"
;disks = MDSPT * 2.
HDRSIZ equ 3 ;header bytes before data
TLRSIZ equ 2 ;trailer bytes read after data

SECSIZ equ BPS+HDRSIZ+TLRSIZ ;total bytes/sector

RETRYS equ 16 ;max retries per sector

;--------------------------------------------------------------
; Memory Parameters. To keep code short, several assumptions
; about these values are embedded in the code:
; 1) RAMADR and PROM low address byte = 0
; 2) The address of the last byte of SECBUF (the SECSIZ-sized
; sector buffer) must be XXFF.
; 3) The ls bit of the high byte of RAMADR must be 0
; 4) The value of DMAADR is assumed to be 0
;--------------------------------------------------------------
RAMADR equ 4C00H ;Address for code copied to RAM
SECBUF equ RAMADR+512-SECSIZ
STACK equ SECBUF ;grows down from here
DMAADR equ 0 ;Disk load/execution address

;----------------------------------------------
; Addresses of sector components within SECBUF
;----------------------------------------------
SFSIZE equ SECBUF+1 ;address of file size
SDATA equ SECBUF+HDRSIZ ;address of sector data
SMARKR equ SDATA+BPS ;address of marker byte
SCKSUM equ SMARKR+1 ;address of checksum byte

;----------------
; 88-SIO Equates
;----------------
SIOCTL EQU 00 ;Control
SIOSTA EQU 00 ;Status
SIODAT EQU 01 ;Rx/Tx Data

;-----------------------------------------------
; 88-2SIO's port 0, Turnkey Module, and 88-UIO
; Equates (all based on the Motorola 6850 ACIA)
;-----------------------------------------------
ACCTRL equ 10h ;ACIA Control output port
ACSTAT equ 10h ;ACIA Status input port
ACDATA equ 11h ;ACIA Tx/Rx Data register

ACRST equ 03h ;Master reset
ACINIT equ 11h ;/16, 8bit, No Parity, 2Stops

;----------------
; 88-PIO Equates
;----------------
PIOCTL EQU 04 ;Control
PIOSTA EQU 04 ;Status
PIODAT EQU 05 ;Tx/Rx Data

;-----------------
; 88-4PIO equates
;-----------------
P4CA0 equ 20h ;Port 0 Section A Ctrl/Status
P4DA0 equ 21h ;Port 0 Section A Data
P4CB0 equ 22h ;Port 0 Section B Ctrl/Status
P4DB0 equ 23h ;Port 0 Section B Data

P4CINI equ 2Ch ;Control/status initialization

;----------------------------------------------------------
; Altair 8800 Disk Controller Equates (These are the same
; for the 88-DCDD controller and the 88-MDS controller.)
;----------------------------------------------------------
DENABL equ 08H ;Drive Enable output
DDISBL equ 80h ;disable disk controller
DSTAT equ 08H ;Status input (active low)
ENWDAT equ 01h ;-Enter Write Data
MVHEAD equ 02h ;-Move Head OK
HDSTAT equ 04h ;-Head Status
DRVRDY equ 08h ;-Drive Ready
INTSTA equ 20h ;-Interrupts Enabled
TRACK0 equ 40h ;-Track 0 detected
NRDA equ 80h ;-New Read Data Available

DCTRL equ 09h ;Drive Control output
STEPIN equ 01H ;Step-In
STPOUT equ 02H ;Step-Out
HDLOAD equ 04H ;8" disk: load head

;Minidisk: restart 6.4 S timer
HDUNLD equ 08h ;unload head (8" only)
IENABL equ 10h ;Enable sector interrupt
IDSABL equ 20h ;Disable interrupts
WENABL equ 80h ;Enable drive write circuits

DSECTR equ 09h ;Sector Position input
SVALID equ 01h ;Sector Valid (1st 30 uS of sector pulse)
SECMSK equ 3Eh ;Sector mask for MDSEC

DDATA equ 0Ah ;Disk Data (input/output)

;----------------------------
; Single-byte error messages
;----------------------------
CERMSG equ 'C' ;checksum/marker byte error
MERMSG equ 'M' ;memory write verify error
OERMSG equ 'O' ;Memory overlay error message

;==============================================================
ORG RAMADR ;assemble at dest RAM address
;==============================================================

di ;turn off INTE (no error yet)

;--------------------------------------------------------------
; Copy the PROM content to RAM for execution. This copy routine
; is position independent so the boot PROM can be at most any
; address. The LSB of the PROM address and RAMADR must be 0.
;--------------------------------------------------------------
lxi d,MLOOP ;DE->MLOOP in RAM

lxi sp,STACK
lxi h,0E9E1h ;H=PCHL,L=POP H
push h ;POP H, PCHL at STACK-2, STACK-1
call STACK-2 ;addr of MLOOP in HL and stack RAM

MLOOP: dcx sp ;point SP to MLOOP address
dcx sp ; in stack memory

mov a,m ;get next EPROM byte
stax d ;store it in RAM

inr e ;bump pointers
inr l
rnz ;copy to end of 256 byte page

jmp RAMIMG ;jump to code now in RAM

; e=l=0

;==============================================================
; RAM Code Image
; All of the following code gets copied into RAM at RAMADR,
; and run there.
;==============================================================
RAMIMG:

;-------------------------------------------------------------
; Wait for user to insert a diskette into the drive 0, and
; then load that drive's head. Do this first so that the disk
; has plenty of time to settle. Note that a minidisk will
; always report that it is ready. Minidisks will hang (later
; on) waiting for sector 0F, until a few seconds after the
; user inserts a disk.
;
; On Entry:
; l = 0
;-------------------------------------------------------------

WAITEN: xra a ;boot from disk 0
out DENABL ;enable disk 0
in DSTAT ;Read drive status
ani DRVRDY ;Diskette in drive?
jnz WAITEN ;no: wait for drive ready

mvi a,HDLOAD ;Load 8" disk head, or enable
out DCTRL ;..minidisk for 6.4 Sec

;-------------------------------------------------------
; Step in once, then step out until track 0 is detected
; On Exit: b=0
;-------------------------------------------------------
lxi b,20000/12 ;20 mS delay 1st time thru
mvi a,STEPIN ;step in once first

SKTRK0: out DCTRL ;issue step command

; The first time through, delay at least 20ms to force a
; minimum 43 ms step wait instead of 10ms. This meets
; the 8" spec for changing seek direction. The minidisk
; step time is always 50ms.

DELAY: dcx b ;(5)
mov a,b ;(5)
ora c ;(4)
jnz DELAY ;(10)12 uS/pass

inr c ;from now on, the above loop goes 1 time.
WSTEP: in DSTAT ;wait for step to complete
rrc ;put MVHEAD bit in carry
rrc ;is the servo stable?
jc WSTEP ;no: wait for servo to settle

ani TRACK0/4 ;Are we at track 00?
mvi a,STPOUT ;STEP-OUT command
jnz SKTRK0 ;no: step out another track

;Exit with b=0
;--------------------------------------------------------
; Determine if this is an 8" disk or a minidisk, and set
; c to the correct sectors/track for the detected disk.
; An 8" disk has 20h sectors, numbered 0-1Fh. A minidisk
; has 10h sectors, numbered 0-0Fh.
;--------------------------------------------------------

; wait for the highest minidisk sector, sector number 0Fh

CKDSK1: in DSECTR ;Read the sector position

ani SECMSK+SVALID ;Mask sector bits, and hunt
cpi (MDSPT-1)*2 ;..for minidisk last sector
jnz CKDSK1 ;..only while SVALID is 0

; wait for this sector to pass

CKDSK2: in DSECTR ;Read the sector position
rrc ;wait for invalid sector
jnc CKDSK2

; wait for and get the next sector number

CKDSK3: in DSECTR ;Read the sector position
rrc ;put SVALID in carry
jc CKDSK3 ;wait for sector to be valid

; The next sector after sector 0Fh will be 0 for a minidisk,
; and 10h for an 8" disk. Adding MDSPT (10h) to that value
; will compute c=10h (for minidisks) or c=20h (for 8" disks).

ani SECMSK/2 ;Mask sector bits
adi MDSPT ;compute SPT
mov c,a ;..and save SPT in c

;------------------------------------------------------------
; Initialize the ACIA (2SIO port 0/Turnkey/UIO). Do this
; late in the initialization, so that e.g. the 'B' character
; from UBMON won't get eaten by resetting the ACIA.
;------------------------------------------------------------
mvi a,ACRST ;reset first
out ACCTRL

mvi a,ACINIT ;then initialize
out ACCTRL

;---------------------
; Initialize the 4PIO
;---------------------
xra a
out P4CB0 ;Port 0 section B is output
cma ;All output bits high
out P4DB0
mvi a,P4CINI ;set up handshake bits
out P4CB0

;--------------------------------------------
; Set up to load
; On Entry:
; b = 0 (initial sector number)
; c = SPT (for either minidisk or 8" disk)
; l = 0 (part of DMA address)
;--------------------------------------------
mov h,l ;initial DMA address=0000

;--------------------------------------------------------
; Read current sector over and over, until either the
; checksum is right, or there have been too many retries
; b = current sector number
; c = sectors/track for this kind of disk
; hl = current DMA address
;--------------------------------------------------------
NXTSEC: mvi a,RETRYS ;(7)Initialize sector retries

;------------------------------------------------------
; Begin Sector Read
; a = Remaining retries for this sector
; b = Current sector number
; c = Sectors/track for this kind of disk
; hl = current DMA address
;------------------------------------------------------
RDSECT: lxi sp,STACK ;(10)(re)initialize the stack
push psw ;(11)Remaining retry count

;-----------------------------------------------------------
; Sector Read: Step 1. Hunt for sector specified in b. Data
; will become avaiable 250 uS after -SVALID
; goes low. -SVALID is low for 30 uS.
;-----------------------------------------------------------
FNDSEC: in DSECTR ;(10)Read the sector position

ani SECMSK+SVALID ;(7)yes: Mask sector bits
;..along with -SVALID bit
rrc ;(4)sector bits to bits <4:0>
cmp b ;(4)Found the desired sector
;..with -SVALID low?
jnz FNDSEC ;(10)no: wait for it

;------------------------------------------------------------
; Test for DMA address that would overwrite this RAM code
; or the next page (which contains the sector buffer stack)
; Do this here, while we have some time.
;------------------------------------------------------------
lxi d,SECBUF ;(10)Sector buffer address

mov a,h ;(5)high byte of DMA address
xra d ;(4)high byte of RAM code addr
ani 0FEh ;(7)ignore lsb
mvi a,OERMSG ;(7)overlay error message
jz RPTERR ;(10)report overlay error

;----------------------------------------
; Set up for the upcoming data move
; Do this here, while we have some time.
;----------------------------------------
push h ;(11)Current DMA address
push b ;(11)Current sector & SPT
lxi b,BPS ;(10)b= init checksum,
;c= byte count for MOVLUP
;-------------------------------------------------------------
; Sector Read: Step 2. Read sector data into SECBUF at de.
; SECBUF is positioned in memory such that e
; overflows at the end of the buffer. Read data
; becomes available 250 uS after -SVALID becomes
; true (0).This loop must be << 32 uS per pass.
;-------------------------------------------------------------
DATLUP: in DSTAT ;(10)Read the drive status
rlc ;(4)New Read Data Available?
jc DATLUP ;(10)no: wait for data

in DDATA ;(10)Read data byte
stax d ;(7)Store it in sector buffer
inr e ;(5)Move to next buffer address
;..and test for end
jnz DATLUP ;(10)Loop if more data

;--------------------------------------------------------
; Sector Read: Step 3. Move sector data from SECBUF into
; memory at hl. Compute checksum as we go.
;
; 8327 cycles for this section
;--------------------------------------------------------
mvi e,SDATA and 0FFh ;(7)de= address of sector data
;..within the sector buffer
MOVLUP: ldax d ;(7)Get sector buffer byte
mov m,a ;(7)Store it at the destination
cmp m ;(7)Did it store correctly?
jnz MEMERR ;(10)no: abort w/ memory error

add b ;(4)update checksum
mov b,a ;(5)Save the updated checksum

inx d ;(5)Bump sector buffer pointer
inx h ;(5)Bump DMA pointer
dcr c ;(5)More data bytes to copy?
jnz MOVLUP ;(10)yes: loop

;--------------------------------------------------------------
; Sector Read: Step 4. Check Marker byte and compare computed
; checksum against sector's checksum. Retry/abort
; if wrong Marker byte or checksum mismatch.
;
; a=computed checksum
; 98 cycles for for this section
;--------------------------------------------------------------
xchg ;(4)hl=1st trailer byte address
;de=DMA address
mov c,m ;(7)get marker, should be FFh
inr c ;(5)c should be 0 now

inx h ;(5)(hl)=checksum byte
xra m ;(7)compare to computed cksum
ora c ;(4)..and test marker=ff

pop b ;(10)Current sector & SPT
jnz BADSEC ;(10)NZ: checksum error

; Compare next DMA address to the file byte count that came
; from the sector header. Done of DMA address is greater.

lhld SFSIZE ;(16)hl gets file size
xchg ;(4)put DMA address back in hl
;..and file size into de
mov a,l ;(4)16-bit subtraction
sub e ;(4)
mov a,h ;(4)..throw away the result
sbb d ;(4)..but keep carry (borrow)

jnc LDDONE ;(10)done loading if hl >= de
;carry will be clear at LDDONE
;------------------------------------------------------------
; Next Sector: The sectors are interleaved by two. Read all
; the even sectors first, then the odd sectors.
; Note that NXTSEC will repair the stack.
;
; 44 cycles for the next even or next odd sector
;------------------------------------------------------------
lxi d,NXTSEC ;(10)for compact jumps
push d ;(10)

inr b ;(5)sector = sector + 2
inr b ;(5)

mov a,b ;(5)even or odd sectors done?
cmp c ;(4)c=SPT
rc ;(5/11)no: go read next sector
;..at NXTSEC
; Total sector-to-sector = 28+8327+98+44=8497 cycles=4248.5 uS
; one 8" sector time = 5208 uS, so with 2:1 interleave, we will
; make the next sector, no problem.

mvi b,01H ;1st odd sector number
rz ;Z: must read odd sectors now
;..at NXTSEC
;--------------------------------------------------------------
; Next Track: Step in, and read again.
; Don't wait for the head to be ready (-MVHEAD),
; since we just read the entire previous track.
; Don't need to wait for this step-in to complete
; either, because we will definitely blow a
; revolution going from the track's last sector to
; sector 0. (One revolution takes 167 mS, and one
; step takes a a maximum of 40 uS.)
; Note that NXTRAC will repair the stack.
;--------------------------------------------------------------
mov a,b ;STEPIN happens to be 01h
out DCTRL

dcr b ;start with b=0 for sector 0
ret ;go to NXTSEC

;***Error Routine**********************************************
; Checksum error: attempt retry if not too many retries
; already. Otherwise, abort, reporting the error
; On Entry:
; Top of stack = adress for first byte of the failing sector
; Next on stack = retry count
;**************************************************************
BADSEC: mvi a,HDLOAD ;Restart Minidisk 6.4 uS timer
out DCTRL

pop h ;Restore DMA address
pop psw ;Get retry count
dcr a ;Any more retries left?
jnz RDSECT ;yes: try reading it again
;------------------------------------------------------
; Irrecoverable error in one sector: too many retries.
; These errors may be either incorrect marker bytes,
; wrong checksums, or a combination of both.
; On Entry:
; hl=RAM adress for first byte of the failing sector
;------------------------------------------------------
mvi a,CERMSG ;Checksum error message
db 11H ;'lxi d' opcode to skip
;..MEMERR and go to RPTERR
;***Error Routine**********************
; Memory error: memory readback failed
; On Entry:
; hl = offending RAM address
;**************************************
MEMERR: mvi a,MERMSG ;Memory Error message

; Fall into RPTERR

;***CDBL Termination*******************************************
; Entry at RPTERR:
; Report an error: turn the disk controller off, turn the
; INTE light on, record the error in RAM at 0000h-0002h, and
; then loop forever writing the error code (in register a)
; to all known Terminal ports.
; On Entry:
; a = error code
; hl = offending RAM address
;
; Entry at LDDONE:
; Normal exit: Disable the disk controller and go execute
; the loaded code at DMAADR.
; On Entry:
; Carry bit is cleared
;**************************************************************
RPTERR: mov b,a ;error code
stc ;remember we had an error

LDDONE: mvi a,DDISBL ;Disable the disk controller
out DENABL

jnc DMAADR ;normal exit: go execute the
;..loaded program
ei ;Signal error on the INTE LED

shld 1 ;Store the bad address
mov a,b ;recover the error code
sta 0 ;Store the error code

ERHANG: out SIODAT ;SIO
out ACDATA ;2SIO port 0/Turnkey/UIO
out PIODAT ;PIO
out P4DB0 ;4PIO
jmp ERHANG ;Keep printing error code

end
