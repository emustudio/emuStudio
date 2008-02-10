/*
 * DiskImpl.java
 *
 * Created on Streda, 30 january 2008
 * 
 * KEEP IT SIMPLE STUPID
 * sometimes just... YOU AREN'T GONNA NEED IT
 * 
 * MITS 88-DISK Floppy Disk controller with up to eight drives (although I
 * think that the interface can actually support up to 16 drives). The connected
 * floppy drives were Pertec FD-400 8" hard-sectored floppy drives. Each
 * single-sided diskette has 77 tracks of 32, 137-byte sectors each (an
 * unformatted capacity of 337,568 bytes). The controller supported neither
 * interrupts nor DMA, so floppy access required the sustained attention of
 * the CPU. The standard I/O addresses were 10Q-12Q.
 * 
 *    The controller is interfaced to the CPU by use of 3 I/O addreses,
    standardly, these are device numbers 10, 11, and 12 (octal).

    Address     Mode    Function
    -------             ----    --------

        10              Out             Selects and enables Controller and Drive
        10              In              Indicates status of Drive and Controller
        11              Out             Controls Disk Function
        11              In              Indicates current sector position of disk
        12              Out             Write data
        12              In              Read data

    Drive Select Out (Device 10 OUT):

    +---+---+---+---+---+---+---+---+
    | C | X | X | X |   Device      |
    +---+---+---+---+---+---+---+---+

    C = If this bit is 1, the disk controller selected by 'device' is
        cleared. If the bit is zero, 'device' is selected as the
        device being controlled by subsequent I/O operations.
    X = not used
    Device = value zero thru 15, selects drive to be controlled.

    Drive Status In (Device 10 IN):

      7   6   5   4   3   2   1   0
    +---+---+---+---+---+---+---+---+
    | R | Z | I | X | X | H | M | W |
    +---+---+---+---+---+---+---+---+

    W - When 0, write circuit ready to write another byte.
    M - When 0, head movement is allowed
    H - When 0, indicates head is loaded for read/write
    X - not used (will be 0)
    I - When 0, indicates interrupts enabled (not used this emulator)
    Z - When 0, indicates head is on track 0
    R - When 0, indicates that read circuit has new byte to read

    Drive Control (Device 11 OUT):

    +---+---+---+---+---+---+---+---+
    | W | C | D | E | U | H | O | I |
    +---+---+---+---+---+---+---+---+

    I - When 1, steps head IN one track
    O - When 1, steps head OUT out track
    H - When 1, loads head to drive surface
    U - When 1, unloads head
    E - Enables interrupts (ignored this simulator)
    D - Disables interrupts (ignored this simulator)
    C - When 1 lowers head current (ignored this simulator)
    W - When 1, starts Write Enable sequence:   W bit on device 10
        (see above) will go 1 and data will be read from port 12
        until 137 bytes have been read by the controller from
        that port.  The W bit will go off then, and the sector data
        will be written to disk. Before you do this, you must have
        stepped the track to the desired number, and waited until
        the right sector number is presented on device 11 IN, then
        set this bit.

    Sector Position (Device 11 IN):

    As the sectors pass by the read head, they are counted and the
    number of the current one is available in this register.

    +---+---+---+---+---+---+---+---+
    | X | X |  Sector Number    | T |
    +---+---+---+---+---+---+---+---+

    X = Not used
    Sector number = binary of the sector number currently under the
        head, 0-31.
    T = Sector True, is a 1 when the sector is positioned to read or
        write.

 */

package disk;

import disk.gui.DiskFrame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import plugins.memory.IMemory;

/**
 *
 * @author vbmacher
 */
public class DiskImpl implements IDevice {
    private final static int DRIVES_COUNT = 16;
    private ICPU cpu;
    private IMemory mem;
    private ArrayList drives;
    private int current_drive;
    private DiskFrame gui = null;
    
    public DiskImpl() {
        this.drives = new ArrayList();
        for (int i = 0; i < DRIVES_COUNT; i++)
            drives.add(new Drive(this));
        this.current_drive = 0xFF;
    }

    /**
     * Called from Drive.java if drive selection changes
     */
    public void selectDrive(Drive drive, boolean sel) {
        int i = drives.indexOf(drive);
        if (gui != null) gui.select(i, sel);
    }
    
    // called from Drive.java
    public void driveParamsChanged(Drive drive, boolean headL, int sec,
            int track, int off) {
        int i = drives.indexOf(drive);
        if (gui != null) gui.driveParamsChanged(i, headL, sec, track, off);        
    }
    
    public static void showErrorMessage(String message) {
        javax.swing.JOptionPane.showMessageDialog(null,
                message,"Error",javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    public void init(ICPU cpu, IMemory mem) {
        this.cpu = cpu;
        this.mem = mem;
        
        // attach device to CPU
        if (cpu.attachDevice(new IDevListener() {
            public void devOUT(EventObject evt, int data) {
                // select device
                current_drive = data & 0x0F;
                if ((data & 0x80) != 0) {
                    // disable device
                    ((Drive)drives.get(current_drive)).deselect();
                    current_drive = 0xFF;
                } else
                    ((Drive)drives.get(current_drive)).select();
            }
            public int devIN(EventObject evt) {
                return ((Drive)drives.get(current_drive)).getFlags();
            }
        }, 0x8) == false) {
            showErrorMessage("Error: this device can't be attached (maybe there is a hardware conflict)");
            return;
        }
        if (cpu.attachDevice(new IDevListener() {
            public void devOUT(EventObject evt, int data) {
                ((Drive)drives.get(current_drive)).setFlags((short)data);
            }
            public int devIN(EventObject evt) {
                return ((Drive)drives.get(current_drive)).getSectorPos();
            }
        }, 0x9) == false) {
            showErrorMessage("Error: this device can't be attached (maybe there is a hardware conflict)");
            return;
        }
        if (cpu.attachDevice(new IDevListener() {
            public void devOUT(EventObject evt, int data) {
                try {
                    ((Drive)drives.get(current_drive)).writeData(data);
                } catch(IOException e) {
                    showErrorMessage("Couldn't write to disk");
                }
            }
            public int devIN(EventObject evt) {
                int d = 0;
                try { d = ((Drive)drives.get(current_drive)).readData(); }
                catch(IOException e) {
                    showErrorMessage("Couldn't read from disk");
                }
                return d;
            }
        }, 0xA) == false) {
            showErrorMessage("Error: this device can't be attached (maybe there is a hardware conflict)");
            return;
        }

    }

    public void showGUI() {
        if (gui == null) gui = new DiskFrame(drives);
        gui.setVisible(true);
    }

    public String getDescription() {
        return "MITS 88-DISK Floppy Disk controller with up to 16 drives. "
                + "The connected floppy drives were Pertec FD-400 8\" "
                + "hard-sectored floppy drives. Each single-sided "
                + "diskette has 77 tracks of 32, 137-byte sectors each (an "
                + "unformatted capacity of 337,568 bytes). The controller "
                + "supported neither interrupts nor DMA, so floppy access "
                + "required the sustained attention of the CPU. The standard "
                + "I/O addresses were 10Q-12Q.";
    }

    public String getVersion() { return "0.1b"; }

    public String getName() { return "MITS-88 DISK (floppy drive)"; }

    public String getCopyright() {
        return "\u00A9 Copyright 2008, P. Jakubčo";
    }

    public void destroy() {
        if (gui != null) gui.dispose();
        cpu.disattachDevice(0x8);
        cpu.disattachDevice(0x9);
        cpu.disattachDevice(0xA);
        drives.clear();
    }

}
