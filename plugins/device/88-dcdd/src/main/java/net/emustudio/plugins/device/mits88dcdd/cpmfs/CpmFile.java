/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CP/M file entry.
 *
 * It tries to unify all CP/M versions used in all systems.
 *
 * Flags:
 *  F0: requires set wheel byte (Backgrounder II)
 *  F1: public file (P2DOS, ZSDOS), forground-only command (Backgrounder II)
 *  F2: date stamp (ZSDOS), background-only commands (Backgrounder II)
 *  F7: wheel protect (ZSDOS)
 *  E0: read-only
 *  E1: system file
 *  E2: archived
 *
 * @see <a href="https://www.seasip.info/Cpm/format22.html">CP/M directory format</a>
 */
@Immutable
public class CpmFile {
    public final static int ENTRY_SIZE = 32;
    public final static int RAW_BLOCK_POINTERS_COUNT = 16;
    private final static String INVALID_CHARS_REGEX = "[<>.,;:=?*\\[\\]]";

    // According to: http://www.sydneysmith.com/wordpress/2261/what-is-in-a-cp-m-file-control-block/
    // Extent is always 16K (well, "ex" must make it so)
    // for blockSize=2K and blockPointerIsWord=false, "ex" is either:
    //  1. sequential (00, 01, 02, ...), but contains just 8 values and rest are 0
    //  2. use lowest bit to signify another 1K, so "ex" = xxxxxxxy where xxxxxxx is sequential "ex" and y signifies
    //     1K, so in disk 1 block pointer would refer to 2K, but extent would "appear" still as 16K since the effect
    //     of this xxxxxxxy schema is skipping numbers (like making gaps).
    //
    // For blockSize=2K and blockPointerIsWord=true, it is nicer:
    //  - "ex" is sequential
    //  - 1 block pointer has 2 bytes, so there are 8 values pointing to 2K, thus 16K as expected
    private final static boolean USE_EX_LSB = false;

    public final byte status;

    public final String fileName;
    public final String fileExt;
    public final boolean readOnly;
    public final boolean invisible;
    public final boolean archived;

    public final byte ex; // 0-4 bits
    public final byte s2; // 0-5 bits
    public final int entryNumber; // ((32*S2)+EX) / (exm+1)  ; the same as (s2 << 5 | ex) / (exm+1)
    public final int extentNumber; // (32*S2)+EX)

    public final byte rc; // the number of 128 byte records of the last used logical extent.
    public final byte bc; // the number of bytes in the last used record
    public final int numberOfRecords; // number of records used in this extent (EX & exm) * 128 + RC
    public final List<Byte> al; // allocation

    /**
     * Constructs a CP/M file using raw extent number
     *
     * @param status       user number of file status
     * @param fileName     file name
     * @param readOnly     read only?
     * @param invisible    invisible?
     * @param archived     archived?
     * @param extentNumber raw extent number - i.e. raw entry index across all directory blocks
     * @param exm          extent mask
     * @param bc           bytes count in this extent
     * @param rc           records count in this extent
     * @param al           extents
     */
    CpmFile(byte status, String fileName, boolean readOnly, boolean invisible, boolean archived,
            int extentNumber, byte exm, byte bc, byte rc, List<Byte> al) {
        if (fileName.matches(INVALID_CHARS_REGEX)) {
            // https://linux.die.net/man/5/cpm
            throw new IllegalArgumentException("File name contains invalid chars!");
        }

        this.al = Collections.unmodifiableList(Objects.requireNonNull(al));
        int lastDot = fileName.lastIndexOf('.');
        this.fileName = fileName.substring(0, (lastDot == -1) ? fileName.length() : lastDot).toUpperCase(Locale.ENGLISH);
        this.fileExt = (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toUpperCase(Locale.ENGLISH);

        this.ex = (byte) (extentNumber & 0x1F);
        this.s2 = (byte) ((extentNumber >> 5) & 0x3F);
        this.extentNumber = extentNumber;

        this.status = status;
        this.readOnly = readOnly;
        this.invisible = invisible;
        this.archived = archived;
        this.entryNumber = ((32 * s2) + ex) / (exm + 1);
        this.bc = bc;
        this.rc = rc;
        this.numberOfRecords = USE_EX_LSB ? ((ex & 1) << 8 + (rc & 0xFF)) : (rc & 0xFF);
    }

    private CpmFile(byte status, String fileName, String fileExt, boolean readOnly, boolean invisible, boolean archived,
                    byte ex, byte s2, byte exm, byte bc, byte rc, List<Byte> al) {
        if (fileName.matches(INVALID_CHARS_REGEX) || fileExt.matches(INVALID_CHARS_REGEX)) {
            // https://linux.die.net/man/5/cpm
            throw new IllegalArgumentException("File name contains invalid chars!");
        }
        this.al = Collections.unmodifiableList(Objects.requireNonNull(al));

        this.status = status;
        this.fileName = Objects.requireNonNull(fileName);
        this.fileExt = Objects.requireNonNull(fileExt);
        this.readOnly = readOnly;
        this.invisible = invisible;
        this.archived = archived;

        this.ex = ex;
        this.s2 = s2;
        this.entryNumber = ((32 * s2) + ex) / (exm + 1);
        this.extentNumber = (32 * s2) + ex;

        this.bc = bc;
        this.rc = rc;
        // if ex goes: 01 03 05 06  (odd, odd.. even)
        this.numberOfRecords = USE_EX_LSB ? ((ex & 1) << 8 + (rc & 0xFF)) : (rc & 0xFF);
    }

    public String getFileName() {
        String result = fileName.trim();

        String ext = fileExt.trim();
        if (!ext.equals("")) {
            result += "." + ext;
        }
        return result;
    }

    static CpmFile fromEntry(ByteBuffer entry, byte exm) {
        byte status = (byte) (entry.get() & 0xFF);

        byte[] fileNameBytes = new byte[11];
        entry.get(fileNameBytes);
        boolean readOnly = (fileNameBytes[8] & 0x80) == 0x80;
        boolean invisible = (fileNameBytes[9] & 0x80) == 0x80;
        boolean archived = (fileNameBytes[10] & 0x80) == 0x80;

        fileNameBytes[8] = (byte) (fileNameBytes[8] & 0x7F);
        fileNameBytes[9] = (byte) (fileNameBytes[9] & 0x7F);
        fileNameBytes[10] = (byte) (fileNameBytes[10] & 0x7F);
        String fileNameExt = new String(fileNameBytes);
        String fileName = fileNameExt.substring(0, 8);
        String fileExt = fileNameExt.substring(8);

        byte ex = (byte) (entry.get() & 0x1F); // only bits 0-4 are valid
        byte bc = entry.get();
        byte s2 = (byte) (entry.get() & 0x3F); // only bits 0-5 are valid
        byte rc = entry.get();

        List<Byte> extents = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            byte bp = entry.get();
            extents.add(bp);
        }

        return new CpmFile(status, fileName, fileExt, readOnly, invisible, archived, ex, s2, exm, bc, rc, extents);
    }

    ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(status);

        byte[] fileNameBytes = new byte[11];
        int i;
        for (i = 0; i < fileName.length(); i++) {
            fileNameBytes[i] = (byte) (fileName.charAt(i) & 0x7F);
        }
        for (; i < 8; i++) {
            fileNameBytes[i] = 0x20; // space
        }
        for (; i < fileExt.length(); i++) {
            fileNameBytes[i] = (byte) (fileExt.charAt(i) & 0x7F);
        }
        for (; i < 11; i++) {
            fileNameBytes[i] = 0x20; // space
        }
        if (readOnly) {
            fileNameBytes[8] |= 0x80;
        }
        if (invisible) {
            fileNameBytes[9] |= 0x80;
        }
        if (archived) {
            fileNameBytes[10] |= 0x80;
        }
        entry.put(fileNameBytes);

        entry.put(ex);
        entry.put(bc);
        entry.put(s2);
        entry.put(rc);

        for (byte alByte : al) {
            entry.put(alByte);
        }
        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    public static String getLongHeader() {
        return "St |File name    |Flags |Ex |S2 |Rc |Bc |Al\n" +
            "-------------------------------------------";
    }

    public String toLongString() {
        return String.format("%02x |", status & 0xFF) +
            String.format("%12s |", getFileName()) +
            String.format("%3s   |", (readOnly ? "R" : " ") + (invisible ? "I" : " ") + (archived ? "A" : " ")) +
            String.format("%02x |", ex & 0xFF) +
            String.format("%02x |", s2 & 0xFF) +
            String.format("%02x |", rc & 0xFF) +
            String.format("%02x |", bc & 0xFF) +
            al.stream().map(b -> String.format("%02X", b & 0xFF)).collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return getLongHeader() + toLongString();
    }
}
