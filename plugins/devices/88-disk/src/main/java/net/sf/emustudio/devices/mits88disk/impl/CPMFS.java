/*
 * Copyright (C) 2011-2015, Peter Jakubčo
 *
 * KISS, YAGNI, DRY
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.emustudio.devices.mits88disk.impl;

import emulib.runtime.StaticDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP/M Filesystem handler
 */

// block = 1024, 2048, 4096, 8192 and 16384
public class CPMFS {
    private static final Logger LOGGER = LoggerFactory.getLogger(CPMFS.class);

    private final static int TRACKS = 254;
    private final static int SECTOR_SIZE = 137;
    private final static int SECTORS_COUNT = 32; // 26, 32
    private final static int INTERLEAVE = 2;

    private final static int MAX_FILES = 256; //64, 256;
    private final static int DIRECTORY_TRACK = 6;

    // specific for altcpm.dsk
    private final static int[] DIRECTORY_SECTORS_BITMAP = new int[] {
            0,17,2,19,4,21,6
    };
    private final static int DIRECTORY_ENTRY_SIZE = 32;
    private final static int UNUSED_FILE = 0xE5;

    private final File imageFile;

    private int track;
    private int sector;
    private byte[] sectorData;

    public CPMFS(String imageFile) {
        this(new File(imageFile));
    }

    public CPMFS(File imageFile) {
        this.imageFile = Objects.requireNonNull(imageFile);
        sectorData = new byte[SECTOR_SIZE];

        resetPosition();
    }

    private byte[] readDirectory() throws IOException {
        byte[] directory = new byte[DIRECTORY_SECTORS_BITMAP.length * SECTOR_SIZE];
        ByteBuffer directoryBuffer = ByteBuffer.wrap(directory);

        try (RandomAccessFile randomFile = new RandomAccessFile(imageFile, "r")) {
            resetPosition(DIRECTORY_TRACK);
            for (int i = 0; i < DIRECTORY_SECTORS_BITMAP.length; i++) {
                sector = DIRECTORY_SECTORS_BITMAP[i];

                readSector(randomFile);
                directoryBuffer.put(sectorData);
            }
        }
        return directory;
    }

    private String formatFileName(int status, byte[] fileName) {
        byte[] nameBytes = new byte[11];
        String name;

        for (int i = 0; i < nameBytes.length; i++) {
            nameBytes[i] = (byte)(fileName[i] & 0x7F);
        }
        name = new String(nameBytes);

        return String.format("%02X: %s", status, name);
    }

    public List<String> getFileNames() {
        List<String> fileNames = new ArrayList<>();
        try {
            byte[] directory = readDirectory();
            for (int i = 3; i < directory.length - DIRECTORY_ENTRY_SIZE; i += DIRECTORY_ENTRY_SIZE) {
                ByteBuffer directoryEntry = ByteBuffer.wrap(directory, i, DIRECTORY_ENTRY_SIZE);

                int fileStatus = directoryEntry.get();
                if (fileStatus < 32) {
                    byte[] fileName = new byte[11];
                    directoryEntry.get(fileName);
                    int extentLower = directoryEntry.get();
                    if (extentLower == 0) {
                        fileNames.add(formatFileName(fileStatus, fileName));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unknown error during getting file names", e);
        }
        return fileNames;
    }

    public String getInfo() {
        int fileCount = 0;
        String discLabel = "";

        try {
            byte[] directory = readDirectory();

            for (int i = 3; i < directory.length - DIRECTORY_ENTRY_SIZE; i += DIRECTORY_ENTRY_SIZE) {
                ByteBuffer directoryEntry = ByteBuffer.wrap(directory, i, DIRECTORY_ENTRY_SIZE);

                int fileStatus = directoryEntry.get();

                if (fileStatus != UNUSED_FILE) {
                    fileCount++;
                }
                if (fileStatus == 32) {
                    byte[] fileName = new byte[11];
                    directoryEntry.get(fileName);
                    discLabel = formatFileName(fileStatus, fileName);
                }
            }
        } catch(FileNotFoundException e) {
            StaticDialogs.showErrorMessage("The image file was not found!");
        } catch (IOException r) {   
        }
        return "DISC: " + discLabel + "\n"
                + "Number of files: " + fileCount;
    }

    /************************************************************************/

    private void nextSector() {
        sector += INTERLEAVE;

        if (sector == (SECTORS_COUNT +1)) {
            track++;
            sector = 0;
        } else if (sector == SECTORS_COUNT) {
            sector %= SECTORS_COUNT;
            sector++;
        }

    //    System.out.println("NEXTSECTOR: T " + track + ", S " + sector);
    }

    private void resetPosition() {
        resetPosition(0, 0);
    }

    private void resetPosition(int track) {
        resetPosition(track, 0);
    }

    private void resetPosition(int track, int sector) {
        this.track = track;
        this.sector = sector;

     //   System.out.println("POSITION: T " + track + ", S " + sector);
    }

    private void readSector(RandomAccessFile f) throws IOException {
     //   System.out.println("READING: T " + track + ", S " + sector + "; pos=" +
       //         (SECTORS_COUNT * SECTOR_SIZE * track + SECTOR_SIZE * sector));

        f.seek(SECTORS_COUNT * SECTOR_SIZE * track + SECTOR_SIZE * sector);
        if (f.read(sectorData, 0, SECTOR_SIZE) < SECTOR_SIZE) {
            throw new IOException("Could not read whole sector! (T:" + track + " S:" + sector + ")");
        }
    }

}
