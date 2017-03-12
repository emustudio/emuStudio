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

import net.sf.emustudio.devices.mits88disk.cpmfs.CpmDirectory;
import net.sf.emustudio.devices.mits88disk.cpmfs.CpmFile;
import net.sf.emustudio.devices.mits88disk.cpmfs.RawDisc;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Main {
    private static boolean ARG_LIST = false;
    private static String IMAGE_FILE = null;
    private static boolean ARG_HELP = false;
    private static boolean ARG_INFO = false;
    private static int SECTOR_SIZE = RawDisc.SECTOR_SIZE;
    private static int SECTOR_SKEW = RawDisc.SECTOR_SKEW;
    private static int SECTORS_COUNT = RawDisc.SECTORS_PER_TRACK;
    private static int BLOCK_LENGTH = RawDisc.BLOCK_LENGTH;
    private static int DIRECTORY_TRACK = RawDisc.DIRECTORY_TRACK;
    private static boolean BLOCKS_ARE_TWO_BYTES = CpmDirectory.BLOCKS_ARE_TWO_BYTES;
    private static String CAT_FILE = null;

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
                        ARG_LIST = true;
                        break;
                    case "--IMAGE":
                        i++;
                        IMAGE_FILE = args[i];
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
                    case "--DIRECTORY":
                        i++;
                        DIRECTORY_TRACK = Integer.decode(args[i]);
                        break;
                    case "--CAT":
                        i++;
                        CAT_FILE = args[i].toUpperCase();
                        break;
                    case "--BLOCKPTRS2":
                        BLOCKS_ARE_TWO_BYTES = true;
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

    public static void main(String[] args) throws Exception {
        parseCommandLine(args);

        if (ARG_HELP) {
            printHelp();
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
            DIRECTORY_TRACK,
            StandardOpenOption.READ
        )) {
            CpmDirectory directory = new CpmDirectory(disc, BLOCKS_ARE_TWO_BYTES);

            if (ARG_INFO) {
                System.out.println("Disc label: " + directory.findDiscLabel());
                System.out.println("Number of files: " + directory.filterValidFiles().size());
            }

            if (ARG_LIST) {
                printFiles(directory.filterValidFiles());
            }

            if (CAT_FILE != null) {
                directory.catFile(CAT_FILE);
            }
        }
    }

    private static void printFiles(Collection<CpmFile> files) {
        for (CpmFile file : files) {
            System.out.println(file.toString());
        }
    }

    public static String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.devices.mits88disk.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    private static void printHelp() {
        System.out.println("MITS 88-DISK emuStudio plug-in, version " + getVersion());
        System.out.println("\n88-DISK implements the EXPERIMENTAL feature regarding working with CP/M 2.2 filesystem.");
        System.out.println("In particular, it can only list files so far.");

        System.out.println("\nThe following command line parameters are available:\n"
            + "\n--image name  : use the image file given by the file name"
            + "\n--sectors NUM : number of sectors per track (default " + RawDisc.SECTORS_PER_TRACK + ")"
            + "\n--sectorsize X: sector size in bytes (default " + RawDisc.SECTOR_SIZE + ")"
            + "\n--sectorskew X: sector skew in bytes (default " + RawDisc.SECTOR_SKEW + ")"
            + "\n--blocklen X  : block length in bytes (default " + RawDisc.BLOCK_LENGTH + ")"
            + "\n--directory X : directory track number (default " + RawDisc.DIRECTORY_TRACK + ")"
            + "\n--blockptrs2  : use 2 bytes per block pointer (default "
            + (CpmDirectory.BLOCKS_ARE_TWO_BYTES ? "2 bytes" : "1 byte") + ")"
            + "\n--list        : list all files in the image"
            + "\n--info        : return some drive information"
            + "\n--cat name    : print the file content from the image to stdout"
            + "\n--help        : output this message");
    }

}
