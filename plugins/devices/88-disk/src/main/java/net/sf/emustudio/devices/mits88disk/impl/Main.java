/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import emulib.runtime.ContextPool;
import net.sf.emustudio.devices.mits88disk.cpmfs.CpmDirectory;
import net.sf.emustudio.devices.mits88disk.cpmfs.RawDisc;

import java.io.File;
import java.nio.file.StandardOpenOption;

public class Main {

    private static boolean ARG_LIST = false;
    private static String IMAGE_FILE = null;
    private static boolean ARG_HELP = false;
    private static boolean ARG_INFO = false;
    private static boolean ARG_VERSION = false;
    private static String DIR_FILE = null;
    private static int SECTOR_SIZE = RawDisc.SECTOR_SIZE;
    private static int SECTOR_SKEW = RawDisc.SECTOR_SKEW;
    private static int SECTORS_COUNT = RawDisc.SECTORS_COUNT;
    private static int BLOCK_LENGTH = RawDisc.BLOCK_LENGTH;

    /**
     * This method parsers the command line parameters. It sets internal class
     * data members accordingly.
     *
     * @param args The command line arguments
     */
    private static void parseCommandLine(String[] args) {
        // process arguments
        int size = args.length;
        for (int i = 0; i < size; i++) {
            String arg = args[i].toUpperCase();
            try {
                switch (arg) {
                    case "--LIST":
                        // list files in the image
                        ARG_LIST = true;
                        break;
                    case "--IMAGE":
                        i++;
                        // the image file
                        if (IMAGE_FILE != null) {
                            System.out.println("Image file already defined,"
                                + " ignoring this one: " + args[i]);
                        } else {
                            IMAGE_FILE = args[i];
                            System.out.println("Image file name: " + IMAGE_FILE);
                        }
                        break;
                    case "--DIR":
                        i++;
                        // the directory bitmap file
                        if (DIR_FILE != null) {
                            System.out.println("Directory bitmap file already defined,"
                                + " ignoring this one: " + args[i]);
                        } else {
                            DIR_FILE = args[i];
                            System.out.println("Directory bitmap file name: " + DIR_FILE);
                        }
                        break;
                    case "--VERSION":
                        ARG_VERSION = true;
                        break;
                    case "--HELP":
                        ARG_HELP = true;
                        break;
                    case "--INFO":
                        ARG_INFO = true;
                        break;
                    case "--SECTORS":
                        i++;
                        SECTORS_COUNT = Integer.decode(args[i]);
                        break;
                    case "--SECTORSKEW":
                        i++;
                        SECTOR_SKEW = Integer.decode(args[i]);
                        break;
                    case "--SECTORSIZE":
                        i++;
                        SECTOR_SIZE = Integer.decode(args[i]);
                        break;
                    case "--BLOCKLEN":
                        i++;
                        BLOCK_LENGTH = Integer.decode(args[i]);
                        break;
                    default:
                        System.out.println("Error: Invalid command line argument (" + arg + ")!");
                        break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Expected argument: " + e.getMessage());
            }
        }
    }

    /**
     * The plug-in is able to transfer files from/to CP/M images by command line
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        System.out.println("MITS 88-DISK emuStudio plug-in");
        parseCommandLine(args);

        if (ARG_HELP) {
            printHelp();
            return;
        }

        if (ARG_VERSION) {
            System.out.println(new DiskImpl(0L, new ContextPool()).getVersion());
            return;
        }

        if (IMAGE_FILE == null) {
            System.out.println("Error: Image file cannot be null!");
            System.exit(0);
            return;
        }

        try (RawDisc disc = new RawDisc(
            new File(IMAGE_FILE).toPath(),
            SECTOR_SIZE,
            SECTORS_COUNT,
            SECTOR_SKEW,
            BLOCK_LENGTH,
            StandardOpenOption.READ
        )) {
            CpmDirectory directory = CpmDirectory.fromDisc(disc);

            if (ARG_INFO) {
                System.out.println("Disc label: " + directory.findDiscLabel());
                System.out.println("Number of files: " + directory.filterValidFiles().size());
            }

            if (ARG_LIST) {
                System.out.println(directory.filterValidFiles());
            }
        }
    }

    private static void printHelp() {
        System.out.println("88-DISK implements the EXPERIMENTAL feature regarding working with CP/M 2.2 filesystem.");
        System.out.println("In particular, it can only list files so far.");

        System.out.println("\nThe following command line parameters are available:\n"
            + "\n--image name  : use the image file given by the file name"
            + "\n--sectors NUM : number of sectors in track (default 32)"
            + "\n--sectorsize X: sector size in bytes (default 137)"
            + "\n--sectorskew X: sector skew in bytes (default 17)"
            + "\n--blocklen X  : block length in bytes (default 1024)"
            + "\n--list        : list all files in the image"
            + "\n--info        : return some drive information"
            + "\n--dir name    : directory bitmap file (not used yet)"
            + "\n--version     : print version"
            + "\n--help        : output this message");
    }

}
