/*
 * Created on 6.2.2008, 8:46:46
 *
 * Copyright (C) 2008-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.devices.mits88disk.impl;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs disk operations on single drive.
 */
public class Drive {
    public final static int TRACKS_COUNT = 254;
    public final static int SECTORS_COUNT = 32;
    public final static int SECTOR_LENGTH = 137;

    private short track;
    private short sector;
    private short sectorOffset;

    private File mountedFloppy = null;
    private RandomAccessFile image;
    private boolean selected = false;

    private final List<DriveListener> listeners = new ArrayList<>();

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

    public interface DriveListener {

        public void driveSelect(Drive drive, boolean sel);

        public void driveParamsChanged(Drive drive);
    }

    public Drive() {
        track = 0;
        sector = SECTORS_COUNT;
        sectorOffset = SECTOR_LENGTH;
        flags = 0xE7; // 11100111b
    }

    public void addDriveListener(DriveListener l) {
        listeners.add(l);
    }

    public void removeDriveListener(DriveListener l) {
        listeners.remove(l);
    }

    private void notifyListeners(boolean sel, boolean par) {
        for (DriveListener listener : listeners) {
            if (sel) {
                listener.driveSelect(this, selected);
            }
            if (par) {
                listener.driveParamsChanged(this);
            }
        }
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    /**
     * select device
     */
    public void select() {
        selected = true;
        flags = 0xE5; // 11100101b
        sector = SECTORS_COUNT;
        sectorOffset = SECTOR_LENGTH;
        if (track == 0) {
            flags &= 0xBF;
        } // head is on track 0
        notifyListeners(true, true);
    }

    /**
     * disable device
     */
    public void deselect() {
        selected = false;
        flags = 0xE7;
        notifyListeners(true, false);
    }

    public boolean isSelected() {
        return selected;
    }

    public static void createNewImage(String filename) throws IOException {
        try (RandomAccessFile fout = new RandomAccessFile(filename, "w")) {
            for (int i = 0; i < TRACKS_COUNT * SECTORS_COUNT * SECTOR_LENGTH; i++) {
                fout.writeByte(0);
            }
        }
    }

    public void mount(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.isFile() == false) {
            throw new IOException("Specified file name doesn't point to a file");
        }
        umount();
        this.mountedFloppy = f;
        image = new RandomAccessFile(f, "rwd");
    }

    public void umount() {
        mountedFloppy = null;
        try {
            if (image != null) {
                image.close();
            }
        } catch (IOException e) {
        }
    }

    public File getImageFile() {
        return mountedFloppy;
    }

    public short getFlags() {
        return flags;
    }

    /**
     * Drive Control (Device 11 OUT):
     *
     * +---+---+---+---+---+---+---+---+ | W | C | D | E | U | H | O | I |
     * +---+---+---+---+---+---+---+---+
     *
     * I - When 1, steps head IN one track O - When 1, steps head OUT out track
     * H - When 1, loads head to drive surface U - When 1, unloads head E -
     * Enables interrupts (ignored this simulator) D - Disables interrupts
     * (ignored this simulator) C - When 1 lowers head current (ignored this
     * simulator) W - When 1, starts Write Enable sequence: W bit on device 10
     * (see above) will go 1 and data will be read from port 12 until 137 bytes
     * have been read by the controller from that port. The W bit will go off
     * then, and the sector data will be written to disk. Before you do this,
     * you must have stepped the track to the desired number, and waited until
     * the right sector number is presented on device 11 IN, then set this bit.
     */
    public void setFlags(short val) {
        if (mountedFloppy == null) {
            return;
        }
        if ((val & 0x01) != 0) { /* Step head in */
            track++;
            // if (track > 76) track = 76;
            sector = SECTORS_COUNT;
            sectorOffset = SECTOR_LENGTH;
        }
        if ((val & 0x02) != 0) { /* Step head out */
            track--;
            if (track < 0) {
                track = 0;
                flags &= 0xBF; // head is on track 0
            }
            sector = SECTORS_COUNT;
            sectorOffset = SECTOR_LENGTH;
        }
        if ((val & 0x04) != 0) { /* Head load */
            // 11111011
            flags &= 0xFB; /* turn on head loaded bit */
            flags &= 0x7F; /* turn on 'read data available */
        }
        if ((val & 0x08) != 0) { /* Head Unload */
            flags |= 0x04; /* turn off 'head loaded' */
            flags |= 0x80; /* turn off 'read data avail */

            sector = SECTORS_COUNT;
            sectorOffset = SECTOR_LENGTH;
        }
        /* Interrupts & head current are ignored */
        if ((val & 0x80) != 0) { /* write sequence start */
            sectorOffset = 0; // sectorLength-1;
            flags &= 0xFE; /* enter new write data on */
        }
        notifyListeners(false, true);
    }

    /**
     * @return sector position in specified format
     */
    public short getSectorPos() {
        if (((~flags) & 0x04) != 0) { /* head loaded? */
            sector++;
            if (sector > 31) {
                sector = 0;
            }
            sectorOffset = SECTOR_LENGTH;
            short stat = (short) (sector << 1);
            stat &= 0x3E;  /* 111110b, return 'sector true' bit = 0 (true) */

            stat |= 0xC0;  // set on 'unused' bits  ?? > in simh bit are gonna up
            notifyListeners(false, true);
            return stat;
        } else {
            return 1;
        }   /* head not loaded - sector true is 1 (false) */

    }

    public void writeData(int data) throws IOException {
        int i = sectorOffset;

        if (sectorOffset < SECTOR_LENGTH) {
            sectorOffset++;
        } else {
            flags |= 1; /* ENWD off */

            notifyListeners(false, true);
            return;
        }

        long pos = SECTORS_COUNT * SECTOR_LENGTH * track + SECTOR_LENGTH * sector + i;

        image.seek(pos);
        image.writeByte(data & 0xFF);

        notifyListeners(false, true);
    }

    public short readData() throws IOException {
        if (mountedFloppy == null) {
            return 0;
        }
        int i;

        if (sectorOffset >= SECTOR_LENGTH) {
            i = 0;
        } else {
            i = sectorOffset;
        }

        if (sectorOffset >= SECTOR_LENGTH) {
            sectorOffset = 1;
        } else {
            sectorOffset++;
        }

        long pos = SECTORS_COUNT * SECTOR_LENGTH * track
                + SECTOR_LENGTH * sector + i;

        image.seek(pos);
        notifyListeners(false, true);
        try {
            short r = (short) (image.readUnsignedByte() & 0xFF);
            return r;
        } catch (IOException e) {
            return 0;
        }
    }

    // for gui calls (drive info)
    public int getSector() {
        return sector;
    }

    public int getTrack() {
        return track;
    }

    public int getOffset() {
        return sectorOffset;
    }

    public boolean getHeadLoaded() {
        return ((~flags) & 0x04) != 0;
    }

    public void setTrack(short track) {
        sector = SECTORS_COUNT;
        sectorOffset = SECTOR_LENGTH;
    }
}
