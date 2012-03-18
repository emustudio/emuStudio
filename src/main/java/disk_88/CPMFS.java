/*
 * CPMFS.java
 *
 * KISS, YAGNI
 *
 *  Copyright (C) 2011-2012 vbmacher
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


package disk_88;

import emulib.runtime.StaticDialogs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * CP/M Filesystem handler
 *
 * For CP/M there is one and only one standard disk storage - 8" SSSD, that is
 * to say 8 inch Single-Sided Single-Density Soft sectored disk formatted as
 * 26 128 byte FM encoded sectors per track totaling 77 tracks.
 * The sectors are formatted sequentially as sector numbers 1 through 26, the
 * tracks are formatted as track numbers 0 through 76.
 * The first two tracks are reserved and normally contains the boot program.
 * The next 75 tracks contain the directory and data, the sectors are not
 * accessed sequentially but are in fact accessed in an interleaved fashion.
 * The interleave was optimized for an 8080 running at 2mhz.
 * The interleave factor is 6, this means that the sectors accessed in this
 * order:  1,7,13,19,25,5,11,17,23,3,9,15,21,2,8,14,20,26,6,12,18,24,4,10,16,22.
 *
 *
 * Directory entries - The directory is a sequence of directory entries
 * (also called extents), which contain 32 bytes of the following structure:
 *
 * St F0 F1 F2 F3 F4 F5 F6 F7 E0 E1 E2 Xl Bc Xh Rc
 * Al Al Al Al Al Al Al Al Al Al Al Al Al Al Al Al
 *
 * St is the status; possible values are:
 *    0-15: used for file, status is the user number
 *    16-31: used for file, status is the user number (P2DOS)
 *           or used for password extent (CP/M 3 or higher)
 *    32: disc label
 *    33: time stamp (P2DOS)
 *    0xE5: unused
 *
 * F0-E2 are the file name and its extension. They may consist of any printable
 * 7 bit ASCII character but: < > . , ; : = ? * [ ]. The file name must not
 * be empty, the extension may be empty. Both are padded with blanks. The
 * highest bit of each character of the file name and extension is used as
 * attribute. The attributes have the following meaning:
 *
 * F0: requires set wheel byte (Backgrounder II)
 * F1: public file (P2DOS, ZSDOS), forground-only command (Backgrounder II)
 * F2: date stamp (ZSDOS), background-only commands (Backgrounder II)
 * F7: wheel protect (ZSDOS)
 * E0: read-only
 * E1: system file
 * E2: archived
 * 
 * @author vbmacher
 */

/**
 * sectors per track: 32
sectors per block: 8
blocks per disk: 254
reserved tracks: 6
tracks per disk: 70

 */
public class CPMFS {
    private final static int SECTOR_SIZE = 137; // block = 1024, 2048, 4096, 8192 and 16384
    private final static int TRACKS = 254;
    private final static int SECTORS = 32; // 26, 32
    private final static int INTERLEAVE = 2;

    private final static int MAX_FILES = 256; //64, 256;

    private short track;
    private short sector;
    private long image_pos;

    // 88-DCDD
    private File image;

    private byte[] tempData;

    public CPMFS(String image) {
        this(new File(image));
    }

    public CPMFS(File image) {
        this.image = image;
        tempData = new byte[SECTOR_SIZE];

        resetPos();
    }

    public String getFiles() {
        int files = 0;
        String result = "";

        try {
            RandomAccessFile raf = new RandomAccessFile(image, "r");

            // search for the directory label
            setTrack((short)6);

            // 66C3
            int cnt = 0;
            do {
                int x = readSector(raf);
                nextSector();
                if (x <= 0)
                    break;

                // search for files
                int i = 3;
                do {
                    cnt++;
                    if ((tempData[i] >= 0) && (tempData[i] < 32)) {
                        files++;
                        result += "\n(" + ((int)tempData[i]) + ") " + (char)tempData[i+1] +
                                (char)tempData[i+2] + (char)tempData[i+3] +
                                (char)tempData[i+4] + (char)tempData[i+5] +
                                (char)tempData[i+6] + (char)tempData[i+7] +
                                (char)tempData[i+8] + (char)tempData[i+9] +
                                (char)tempData[i+10] + (char)tempData[i+11];
                    }
                    i += 32;
                } while ((cnt <= MAX_FILES) && (i < SECTOR_SIZE));

            } while (cnt <= MAX_FILES);

            result += "\nNumber of files: " + files;
            raf.close();
        } catch(FileNotFoundException e) {
            StaticDialogs.showErrorMessage("The image file was not found!");
        } catch (IOException r) {
        }
        return result;
    }

    public String getInfo() {
        int files = 0;
        String result = "";

        try {
            RandomAccessFile raf = new RandomAccessFile(image, "r");

            // search for the directory label
            setTrack((short)6);

            // 66C3
            int cnt = 0;
            do {
                int x = readSector(raf);
                nextSector();
                if (x <= 0)
                    break;

                // search for files
                int i = 3;
                do {
                    cnt++;
                    if ((tempData[i] >= 0) && (tempData[i] < 32)) {
                        files++;
                    }

                    if (tempData[i] == 32) {
                        result = "Disc label (t " +track+ ", s " + sector +"): " + (char)tempData[i+1] +
                                (char)tempData[i+2] + (char)tempData[i+3] +
                                (char)tempData[i+4] + (char)tempData[i+5] +
                                (char)tempData[i+6] + (char)tempData[i+7] +
                                (char)tempData[i+8] + (char)tempData[i+9] +
                                (char)tempData[i+10] + (char)tempData[i+11];
                    }

                    i += 32;
                } while ((cnt <= MAX_FILES) && (i < SECTOR_SIZE));

            } while (cnt <= MAX_FILES);

            result += "\nNumber of files: " + files;
            raf.close();
        } catch(FileNotFoundException e) {
            StaticDialogs.showErrorMessage("The image file was not found!");
        } catch (IOException r) {   
        }
        return result;
    }

    /************************************************************************/

    private void nextSector() {
        sector += INTERLEAVE;

        if (sector == (SECTORS+1)) {
            track++;
            sector = 0;
        } else if (sector == SECTORS) {
            sector %= SECTORS;
            sector++;
        }

        image_pos = SECTORS * SECTOR_SIZE * track + SECTOR_SIZE * sector;
//        System.out.println("NEWPOS: T " + track + ", S " + sector + ", pos="
  //              + image_pos);
    }

    private void resetPos() {
        sector = 0;
        track = 0;
        image_pos = 0;
    //    System.out.println("NEWPOS: T " + track + ", S " + sector + ", pos="
      //          + image_pos);
    }

    private void setTrack(short track) {
        this.track = track;
        sector = 0;

        image_pos = SECTORS * SECTOR_SIZE * track;
//        System.out.println("NEWPOS: T " + track + ", S " + sector + ", pos="
  //              + image_pos);
    }

    private int readSector(RandomAccessFile f) {
        try {
            f.seek(image_pos);
            return f.read(tempData, 0, SECTOR_SIZE);
        } catch (IOException e) {
        }
        return -1;
    }



}
