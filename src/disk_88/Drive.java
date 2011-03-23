/*
 * Drive.java
 *
 * Created on 6.2.2008, 8:46:46
 * hold to: KISS, YAGNI
 *
 * Performs disk operations on single drive
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package disk_88;

import java.io.*;
import java.util.EventListener;
import javax.swing.event.EventListenerList;

/**
 * This class provides a single drive.
 *
 * @author vbmacher
 */
public class Drive {
    public final static int tracksCount = 254;
    public final static int sectorsCount = 32;
    public final static int sectorLength = 137;
    
    private short track;
    private short sector;
    private short sectorOffset;
    
    /** mounted image */
    private File floppy = null;
    private RandomAccessFile image;
    private boolean selected = false;
    
 //   private int rotateLatency = 100;

    /** gui interaction */
    private EventListenerList listeners; // list of listeners

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
    private short flags; 

    public interface DriveListener extends EventListener {
        public void driveSelect(Drive drive, boolean sel);
        public void driveParamsChanged(Drive drive);
    }
    
    public Drive() {
        track = 0; 
        sector = sectorsCount;
        sectorOffset = sectorLength;
        flags = 0xE7; // 11100111b
        listeners = new EventListenerList();
    }
    
    public void addDriveListener(DriveListener l) {
        listeners.add(DriveListener.class, l);
    }
    
    public void removeDriveListener(DriveListener l) {
        listeners.remove(DriveListener.class, l);
    }
    
    private void fireListeners(boolean sel, boolean par) {
        Object[] lis= listeners.getListenerList();
        for (int i = 0; i < lis.length; i +=2)
            if (lis[i] == DriveListener.class) {
                if (sel) ((DriveListener)lis[i+1]).driveSelect(this, selected);
                if (par) ((DriveListener)lis[i+1]).driveParamsChanged(this);
            }
    }
    
    public void removeAllListeners() {
        Object[] lis= listeners.getListenerList();
        for (int i = 0; i < lis.length; i +=2)
            listeners.remove(DriveListener.class, (DriveListener)lis[i+1]);
    }
    
    /**
     * select device
     */
    public void select() {
        selected = true;
        flags = 0xE5; // 11100101b
        sector = sectorsCount;
        sectorOffset = sectorLength;
        if (track == 0)
            flags &= 0xBF; // head is on track 0
        fireListeners(true, true);
    }

    /**
     * disable device
     */
    public void deselect() {
        selected = false;
        flags = 0xE7;
        fireListeners(true,false);
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * Create new image
     */
    public static void createNewImage(String filename) throws IOException {
        RandomAccessFile fout = new RandomAccessFile(filename,"w");
        for (int i = 0; i < tracksCount * sectorsCount * sectorLength; i++)
            fout.writeByte(0);
        fout.close();
    }    

    /**
     * Mount an image file to drive (insert diskette)
     */
    public void mount(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.isFile() == false)
            throw new IOException("Specified file name doesn't point to a file");
//        if (f.length() < tracksCount * sectorsCount * sectorLength)
  //          throw new IOException("Image file has small size");
        umount();
        this.floppy = f;
        image = new RandomAccessFile(f, "rwd");
    }
    
    /**
     * Umount image from drive (remove diskette)
     */
    public void umount() {
        floppy = null;
        try { if (image != null) image.close(); }
        catch (IOException e) {}
    }
    
    /**
     * gets image File
     */
    public File getImageFile() { return floppy; }

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
           // if (track > 76) track = 76;
            sector = sectorsCount;
            sectorOffset = sectorLength;
            System.out.println("HEAD IN: T " + track + ", S " + sector + " SO " + sectorOffset);
        }
        if ((val & 0x02) != 0) { /* Step head out */
            track--;
            if (track < 0) {
                track = 0;
                flags &= 0xBF; // head is on track 0
            }
            sector = sectorsCount;
            sectorOffset = sectorLength;
            System.out.println("HEAD OUT: T " + track + ", S " + sector + " SO " + sectorOffset);
        }
        if ((val & 0x04) != 0) { /* Head load */
            // 11111011
            flags &= 0xFB; /* turn on head loaded bit */
            flags &= 0x7F; /* turn on 'read data available */
            System.out.println("HEAD LOAD: T " + track + ", S " + sector + " SO " + sectorOffset);
        }
        if ((val & 0x08) != 0) { /* Head Unload */
            flags |= 0x04; /* turn off 'head loaded' */
            flags |= 0x80; /* turn off 'read data avail */
            sector = sectorsCount;
            sectorOffset = sectorLength;
            System.out.println("HEAD UNLOAD: T " + track + ", S " + sector + " SO " + sectorOffset);
        }
        /* Interrupts & head current are ignored */
        if ((val & 0x80) != 0) { /* write sequence start */
            sectorOffset = 0; // sectorLength-1;
            flags &= 0xFE; /* enter new write data on */
            System.out.println("WRITE START: T " + track + ", S " + sector + " SO " + sectorOffset);
        }
        fireListeners(false,true);
    }
    
    /**
     * return sector position in specified format
     */
    public short getSectorPos() {
        if (((~flags) & 0x04) != 0) { /* head loaded? */
            sector++;
            if (sector > 31) sector = 0;
            sectorOffset = sectorLength;
            short stat = (short) (sector << 1);
            stat &= 0x3E;  /* 111110b, return 'sector true' bit = 0 (true) */
            stat |= 0xC0;  // set on 'unused' bits  ?? > in simh bit are gonna up
            fireListeners(false,true);
            return stat;
        } else return 1;   /* head not loaded - sector true is 1 (false) */
    }
    
    public void writeData(int data) throws IOException {
        int i = sectorOffset;

        if (sectorOffset < sectorLength) sectorOffset++;
        else {
            flags |= 1; /* ENWD off */
            fireListeners(false,true);
            return;
        }

        long pos = sectorsCount * sectorLength * track
                + sectorLength * sector + i;
        
        System.out.println("WRITE: T " + track + ", S " + sector + " SO " + sectorOffset + "; pos=" + pos);
        image.seek(pos);
        image.writeByte(data & 0xFF);
        
        fireListeners(false,true);
    }
    
    public short readData() throws IOException {
        if (floppy == null) return 0;
        int i=0;
                
        if (sectorOffset >= sectorLength) i = 0;
        else i = sectorOffset;
        
        if (sectorOffset >= sectorLength) sectorOffset = 1;
        else sectorOffset++;

        long pos = sectorsCount * sectorLength * track
                + sectorLength * sector + i;
        
        System.out.println("READ: T " + track + ", S " + sector + " SO " + sectorOffset + ", pos=" + pos);

        image.seek(pos);
        fireListeners(false,true);
        try {
        short r = (short) (image.readUnsignedByte() & 0xFF);
        return r;
        } catch(Exception e) { return 0; } 
    }

    // for gui calls (drive info)
    public int getSector() { return sector; }
    public int getTrack() { return track; }
    public int getOffset() { return sectorOffset; }
    public boolean getHeadLoaded() { return ((~flags) & 0x04) != 0; }

    public void setTrack(short track) {
                    sector = sectorsCount;
            sectorOffset = sectorLength;

    }
}
