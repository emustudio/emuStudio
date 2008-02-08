/*
 * Drive.java
 *
 * Created on 6.2.2008, 8:46:46
 * hold to: KISS, YAGNI
 *
 * Performs disk operations
 */

package impl;

import java.io.*;

/**
 *
 * @author vbmacher
 */
public class Drive {
    public final static int tracksCount = 77;
    public final static int sectorsCount = 32;
    public final static int sectorLength = 137;
    
    private int track = 0xFF;
    private int sector = 0xFF;
    private File floppy = null;
    
    private int rotateLatency = 100;
    
    private char[] buffer = new char[sectorLength];
    private int currentChar = 0xFF;
    
    private RandomAccessFile image;

    /*
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
     */
    private short flags=0xE7; // 11100111b

    /**
     * select device
     */
    public void select() {
        flags = 0xE5; // 11100101b
        sector = 0xFF;
        currentChar = 0xFF;
        if (track == 0)
            flags &= 0xBF; // head is on track 0
    }
    
    /**
     * disable device
     */
    public void deselect() {
        flags = 0xE7;
    }

    /**
     * Mount an image file to drive (insert diskette)
     */
    public void mount(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.isFile() == false)
            throw new IOException("Specified file name doesn't point to a file");
        if (f.length() < tracksCount * sectorsCount * sectorLength)
            throw new IOException("Image file has small size");
        umount();
        this.floppy = f;        
        image = new RandomAccessFile(f, "rws");
    }
    
    /**
     * Umount image from drive (remove diskette)
     */
    public void umount() {
        floppy = null;
        try { if (image != null) image.close(); }
        catch (IOException e) {}
    }
    
    public short getFlags() { return flags; }
    
    /**
     * Drive Control (Device 11 OUT):
     *
     * +---+---+---+---+---+---+---+---+
     * | W | C | D | E | U | H | O | I |
     * +---+---+---+---+---+---+---+---+
     *
     * I - When 1, steps head IN one track
     * O - When 1, steps head OUT out track
     * H - When 1, loads head to drive surface
     * U - When 1, unloads head
     * E - Enables interrupts (ignored this simulator)
     * D - Disables interrupts (ignored this simulator)
     * C - When 1 lowers head current (ignored this simulator)
     * W - When 1, starts Write Enable sequence:   W bit on device 10
     *     (see above) will go 1 and data will be read from port 12
     *     until 137 bytes have been read by the controller from
     *     that port.  The W bit will go off then, and the sector data
     *     will be written to disk. Before you do this, you must have
     *     stepped the track to the desired number, and waited until
     *     the right sector number is presented on device 11 IN, then
     *     set this bit.
     */      
    public void setFlags(short val) {
        if (floppy == null) return;
        if ((val & 0x01) != 0) { /* Step head in */
            track++;
            if (track > 76) track = 76;
            sector = 0xFF;
            currentChar = 0xFF;
        }
        if ((val & 0x02) != 0) { /* Step head out */
            track--;
            if (track < 0) {
                track = 0;
                flags &= 0xBF; // head is on track 0
            }
            sector = 0xFF;
            currentChar = 0xFF;
        }
        if ((val & 0x04) != 0) { /* Head load */
            // 11111011
            flags &= 0xFB; /* turn on head loaded bit */
            flags &= 0x7F; /* turn on 'read data available */
        }
        if ((val & 0x08) != 0) { /* Head Unload */
            flags |= 0x04; /* turn off 'head loaded' */
            flags |= 0x80; /* turn off 'read data avail */
            sector = 0xFF;
            currentChar = 0xFF;
        }
        /* Interrupts & head current are ignored */
        if ((val & 0x80) != 0) { /* write sequence start */
            currentChar = 0;
            flags &= 0xFE; /* enter new write data on */
        }
    }
    
    /**
     * return sector position in specified format
     */
    public int getSectorPos() {
        if (((~flags) & 0x04) != 0) { /* head loaded? */
            sector++;
            if (sector > 31) sector = 0;
            currentChar = 0xFF;
            int stat = sector << 1;
            stat &= 0x3E;  /* 111110b, return 'sector true' bit = 0 (true) */
            stat |= 0xC0;  // set on 'unused' bits  ?? > in simh bit are gonna up
            return stat;
        } else return 1;   /* head not loaded - sector true is 1 (false) */
    }
    
    public void writeData(int data) throws IOException {
        currentChar++;
        // this is questionable... what to do if pos is on the end ?
        if (currentChar >= sectorLength) currentChar = 0;
        long pos = sectorsCount * sectorLength * track
                + sectorLength * sector
                + currentChar;
        image.seek(pos);
        image.writeByte(data & 0xFF);
        flags |= 1; /* ENWD off */
    }
    
    public int readData() throws IOException {
        currentChar++;
        // this is questionable... what to do if pos is on the end ?
        if (currentChar >= sectorLength) currentChar = 0;
        long pos = sectorsCount * sectorLength * track
                + sectorLength * sector
                + currentChar;
        image.seek(pos);
        return image.readByte();
    }

}
